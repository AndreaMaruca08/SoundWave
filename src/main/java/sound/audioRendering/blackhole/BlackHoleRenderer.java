package sound.audioRendering.blackhole;

import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;
import sound.audioRendering.WavePlayer;

public class BlackHoleRenderer extends AudioRenderer {

    private static final int STAR_COUNT = 2200;
    private static final int DISK_PARTICLES = 10000;
    private static final int INFALL_PARTICLES = 1000;
    private static final int ERGO_FILAMENTS = 1000;
    private static final int MAX_SHOCKWAVES = 2;
    private static final int SAMPLE_RATE = 44100;
    private static final int ANALYSIS_WINDOW = 512;

    // Stelle di fondo
    private final float[] starX = new float[STAR_COUNT];
    private final float[] starY = new float[STAR_COUNT];
    private final float[] starBaseSize = new float[STAR_COUNT];
    private final float[] starBaseBright = new float[STAR_COUNT];
    private final float[] starTintR = new float[STAR_COUNT];
    private final float[] starTintG = new float[STAR_COUNT];
    private final float[] starTintB = new float[STAR_COUNT];
    private final float[] starPhase = new float[STAR_COUNT];
    private final float[] starDepth = new float[STAR_COUNT]; // 0 = sfondo lontano (lensing forte), 1 = primo piano (poca repulsione)

    // Particelle del disco di accrescimento (nucleo denso + alone esteso nello spazio)
    private final float[] pNormRadius = new float[DISK_PARTICLES];
    private final float[] pTheta = new float[DISK_PARTICLES];
    private final float[] pPrevTheta = new float[DISK_PARTICLES];
    private final float[] pThickness = new float[DISK_PARTICLES];
    private final float[] pHeatBias = new float[DISK_PARTICLES];

    // Flussi mareali in caduta oltre l'ISCO
    private final float[] infRadius = new float[INFALL_PARTICLES];
    private final float[] infTheta = new float[INFALL_PARTICLES];
    private final float[] infPrevRadius = new float[INFALL_PARTICLES];
    private final float[] infPrevTheta = new float[INFALL_PARTICLES];

    // Filamenti dell'ergosfera
    private final float[] ergoTheta = new float[ERGO_FILAMENTS];
    private final float[] ergoRadius = new float[ERGO_FILAMENTS];

    // Onde d'urto gravitazionali nello spazio
    private final float[] waveRadius = new float[MAX_SHOCKWAVES];
    private final float[] waveAlpha = new float[MAX_SHOCKWAVES];
    private final boolean[] waveActive = new boolean[MAX_SHOCKWAVES];
    private int nextWaveSlot = 0;
    private float waveCooldown = 0f;

    // Stato dinamico guidato dall'audio (di base quasi fermo)
    private int lastPeakIndex = -1;
    private float smoothedPower = 0f;
    private float smoothedLow = 0f;
    private float smoothedHigh = 0f;
    private float peakPulse = 0f;
    private float flareIntensity = 0f;
    private float gravityStrength = 0.05f;
    private float spaceTwist = 0f;
    private float timeAccum = 0f;

    public BlackHoleRenderer(WavePlayer player) {
        float w = player.getW();
        float h = player.getH() * 0.8f;
        initDeterministicStars(w, h);
        initAccretionDisk();
        initInfallAndErgosphere();
        reload(player.waveBuffer);
    }

    private static float halton(int index, int base) {
        float result = 0f;
        float f = 1f / base;
        int i = index;
        while (i > 0) {
            result += f * (i % base);
            i /= base;
            f /= base;
        }
        return result;
    }

    private void initDeterministicStars(float w, float h) {
        float safeW = Math.max(1f, w);
        float safeH = Math.max(1f, h);

        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = halton(i + 1, 2) * safeW;
            starY[i] = halton(i + 1, 3) * safeH;

            float lum = halton(i + 1, 5);
            starBaseSize[i] = 0.55f + lum * 1.45f;
            starBaseBright[i] = 0.22f + lum * 0.70f;
            starPhase[i] = halton(i + 1, 11) * (float) (Math.PI * 2.0);
            starDepth[i] = halton(i + 1, 13);

            float temp = halton(i + 1, 7);
            if (temp < 0.30f) {
                starTintR[i] = 0.70f;
                starTintG[i] = 0.85f;
                starTintB[i] = 1.0f;
            } else if (temp < 0.70f) {
                starTintR[i] = 0.96f;
                starTintG[i] = 0.96f;
                starTintB[i] = 0.94f;
            } else if (temp < 0.90f) {
                starTintR[i] = 1.0f;
                starTintG[i] = 0.83f;
                starTintB[i] = 0.62f;
            } else {
                // Giganti rosse lontane
                starTintR[i] = 1.0f;
                starTintG[i] = 0.58f;
                starTintB[i] = 0.42f;
            }
        }
    }

    private void initAccretionDisk() {
        for (int i = 0; i < DISK_PARTICLES; i++) {
            float u = halton(i + 17, 2);
            // 85% nel disco principale (1.12..4.4), 15% come polvere/materia esterna estesa fino a 6.8 Rs
            float rNorm = (i % 7 == 0)
                    ? (3.6f + u * 3.2f)
                    : (1.12f + (u * u) * 3.35f);
            float theta = halton(i + 17, 3) * (float) (Math.PI * 2.0);

            pNormRadius[i] = rNorm;
            pTheta[i] = theta;
            pPrevTheta[i] = theta;
            pThickness[i] = 0.85f + halton(i + 17, 5) * 1.85f;
            pHeatBias[i] = (halton(i + 17, 7) - 0.5f) * 0.22f;
        }
    }

    private void initInfallAndErgosphere() {
        for (int i = 0; i < INFALL_PARTICLES; i++) {
            float r = 1.02f + halton(i + 31, 2) * 0.52f;
            float t = halton(i + 31, 3) * (float) (Math.PI * 2.0);
            infRadius[i] = r;
            infPrevRadius[i] = r;
            infTheta[i] = t;
            infPrevTheta[i] = t;
        }
        for (int i = 0; i < ERGO_FILAMENTS; i++) {
            ergoTheta[i] = (float) (i * (Math.PI * 2.0) / ERGO_FILAMENTS);
            ergoRadius[i] = 1.025f + halton(i + 7, 3) * 0.16f;
        }
    }

    @Override
    protected void renderInternal(NvGraphic g, short[] samples, float width, float height, float currentTime) {
        if (width <= 0 || height <= 0) {
            return;
        }

        // 1. Analisi audio deterministica
        float audioPower = 0f;
        float impulse = 0f;
        int index = (int) ((currentTime / 1000f) / peakDuration);

        if (peaks != null && index >= 0 && index < peaks.length) {
            audioPower = peaks[index];
            if (index != lastPeakIndex) {
                lastPeakIndex = index;
                impulse = (float) Math.max(0.0, audioPower - average);
                peakPulse = Math.min(2.6f, peakPulse + impulse * 2.8f);
                if (impulse > 0.08f) {
                    flareIntensity = Math.min(1.6f, flareIntensity + impulse * 2.0f);
                }
            }
        }

        float lowEnergy = 0f;
        float highEnergy = 0f;
        if (samples != null && samples.length > 4) {
            int centerSample = (int) ((currentTime / 1000f) * SAMPLE_RATE);
            int start = Math.max(0, Math.min(samples.length - ANALYSIS_WINDOW, centerSample - ANALYSIS_WINDOW / 2));
            int end = Math.min(samples.length, start + ANALYSIS_WINDOW);
            int count = end - start;
            if (count > 4) {
                double lowSumSq = 0.0;
                double highSumSq = 0.0;
                float prevLow = samples[start] / 32768f;
                for (int i = start + 1; i < end; i++) {
                    float s = samples[i] / 32768f;
                    float low = prevLow + 0.14f * (s - prevLow);
                    float high = s - low;
                    lowSumSq += low * low;
                    highSumSq += high * high;
                    prevLow = low;
                }
                lowEnergy = Math.min(1f, (float) Math.sqrt(lowSumSq / count) * 2.4f);
                highEnergy = Math.min(1f, (float) Math.sqrt(highSumSq / count) * 3.0f);
            }
        }

        smoothedPower += (audioPower - smoothedPower) * 0.18f;
        smoothedLow += (lowEnergy - smoothedLow) * 0.20f;
        smoothedHigh += (highEnergy - smoothedHigh) * 0.25f;
        peakPulse *= 0.91f;
        flareIntensity *= 0.89f;

        // Di base quasi fermo (~0.05), accelera fortemente sui picchi audio
        float targetGravity = 0.05f + smoothedPower * 2.4f + smoothedLow * 1.8f + peakPulse * 2.2f;
        gravityStrength += (targetGravity - gravityStrength) * 0.22f;

        float dt = 0.016f;
        timeAccum += dt * (0.08f + smoothedPower * 2.6f + peakPulse * 1.5f);
        spaceTwist += dt * (0.01f + gravityStrength * 0.16f);
        waveCooldown = Math.max(0f, waveCooldown - dt);

        if (impulse > 0.09f && waveCooldown <= 0f) {
            spawnShockwave();
            waveCooldown = 0.15f;
        }

        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        float minDim = Math.min(width, height);
        float diag = (float) Math.hypot(width, height);

        float rs = minDim * (0.138f + smoothedLow * 0.018f + peakPulse * 0.014f);
        float tilt = 0.205f + (float) Math.sin(timeAccum * 0.55f) * 0.018f + smoothedPower * 0.03f;

        updateDiskPhysics(dt, impulse);
        updateInfallAndErgosphere(dt, impulse);
        updateShockwaves(dt, rs, diag);

        // 2. Campo stellare su tutto lo spazio con lensing gravitazionale, distorsione ad arco e onde d'urto
        drawLensedStars(g, centerX, centerY, width, height, rs);

        // 3. Onde d'urto gravitazionali che attraversano tutto lo spazio
        drawShockwaves(g, centerX, centerY, rs);

        // 4. Getti polari relativistici Blandford-Znajek
        drawPolarJets(g, centerX, centerY, rs);

        // 5. Lensing gravitazionale del lato posteriore (arco superiore + arco inferiore, continuo sulla linea centrale)
        drawLensedFarDisk(g, centerX, centerY, rs, tilt);

        // 6. Ergosfera + Flussi mareali + Orizzonte degli eventi + Sfera fotonica
        drawErgosphereAndInfall(g, centerX, centerY, rs, tilt);
        drawEventHorizonAndPhotonSphere(g, centerX, centerY, rs);

        // 7. Flare di riconnessione magnetica (sincrotrone ciano-viola sui picchi)
        drawSynchrotronFlares(g, centerX, centerY, rs);

        // 8. Disco di accrescimento primario (lato anteriore)
        drawNearAccretionDisk(g, centerX, centerY, rs, tilt);

        g.setTransparency(1f);
    }

    private void spawnShockwave() {
        waveActive[nextWaveSlot] = true;
        waveRadius[nextWaveSlot] = 1.04f;
        waveAlpha[nextWaveSlot] = Math.min(0.72f, 0.25f + peakPulse * 0.26f);
        nextWaveSlot = (nextWaveSlot + 1) % MAX_SHOCKWAVES;
    }

    private void updateShockwaves(float dt, float rs, float diag) {
        float speed = 0.6f + smoothedPower * 3.4f + peakPulse * 2.2f;
        float maxMult = (diag * 0.65f) / Math.max(1f, rs);
        for (int i = 0; i < MAX_SHOCKWAVES; i++) {
            if (!waveActive[i]) continue;
            waveRadius[i] += speed * dt;
            waveAlpha[i] *= 0.962f;
            if (waveRadius[i] > maxMult || waveAlpha[i] < 0.012f) {
                waveActive[i] = false;
            }
        }
    }

    private void updateDiskPhysics(float dt, float impulse) {
        // Di base lentissimo (0.045), cresce proporzionalmente ai picchi audio
        float omegaScale = (0.045f + gravityStrength * 1.15f + smoothedHigh * 1.1f) * dt;
        float infallRate = (0.001f + impulse * 0.16f + smoothedLow * 0.035f) * dt;

        for (int i = 0; i < DISK_PARTICLES; i++) {
            float r = pNormRadius[i];
            pPrevTheta[i] = pTheta[i];

            float invSqrtR = 1.0f / (float) (r * Math.sqrt(r));
            float dTheta = omegaScale * invSqrtR * 2.6f;
            if (dTheta > 0.42f) dTheta = 0.42f;

            float newTheta = pTheta[i] + dTheta;
            if (newTheta >= (float) (Math.PI * 2.0)) {
                newTheta -= (float) (Math.PI * 2.0);
                pPrevTheta[i] -= (float) (Math.PI * 2.0);
            }
            pTheta[i] = newTheta;

            r -= infallRate / (r * r);
            if (r < 1.04f) {
                r = (i % 7 == 0) ? (5.2f + (i % 17) * 0.08f) : (4.05f + (i % 19) * 0.022f);
                pPrevTheta[i] = pTheta[i];
            }
            pNormRadius[i] = r;
        }
    }

    private void updateInfallAndErgosphere(float dt, float impulse) {
        float spin = (0.08f + gravityStrength * 1.85f) * dt;
        float pull = (0.01f + smoothedLow * 0.35f + impulse * 0.60f) * dt;

        for (int i = 0; i < INFALL_PARTICLES; i++) {
            infPrevRadius[i] = infRadius[i];
            infPrevTheta[i] = infTheta[i];

            float r = infRadius[i];
            float omega = spin / (r * r);
            if (omega > 0.45f) omega = 0.45f;

            infTheta[i] += omega;
            if (infTheta[i] >= (float) (Math.PI * 2.0)) {
                infTheta[i] -= (float) (Math.PI * 2.0);
                infPrevTheta[i] -= (float) (Math.PI * 2.0);
            }

            r -= pull / (r * r);
            if (r <= 0.99f) {
                r = 1.38f + (i % 13) * 0.02f;
                infPrevRadius[i] = r;
                infPrevTheta[i] = infTheta[i];
            }
            infRadius[i] = r;
        }

        for (int i = 0; i < ERGO_FILAMENTS; i++) {
            float r = ergoRadius[i];
            ergoTheta[i] += (spin * 1.25f) / (r * r);
            if (ergoTheta[i] >= (float) (Math.PI * 2.0)) {
                ergoTheta[i] -= (float) (Math.PI * 2.0);
            }
        }
    }

    private void drawLensedStars(NvGraphic g, float cx, float cy, float w, float h, float rs) {
        float einsteinR = rs * (1.12f + Math.min(0.42f, gravityStrength * 0.06f));
        float einsteinSq = einsteinR * einsteinR;
        float occultR = rs * 1.01f;
        float dragStrength = (0.02f + Math.min(0.8f, gravityStrength * 0.09f)) * rs;

        g.beginBatch();
        for (int i = 0; i < STAR_COUNT; i++) {
            float dx = starX[i] - cx;
            float dy = starY[i] - cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            float depth = starDepth[i];
            // Solo le stelle dietro al buco nero vengono occultate dall'orizzonte degli eventi
            if (dist < occultR && depth < 0.78f) {
                continue;
            }

            // Deflessione selettiva: stelle lontane (depth vicino a 0) vengono respinte/piegate molto,
            // stelle più vicine o con alto depth sentono poca repulsione così non si crea un buco al centro
            float lensSensitivity = (1.0f - depth * 0.88f);
            lensSensitivity *= lensSensitivity;
            float deflection = (einsteinSq / (dist + 10f)) * lensSensitivity;
            float apparentDist = dist + deflection * (0.28f + Math.min(0.32f, peakPulse * 0.10f));
            float scale = dist > 0.001f ? (apparentDist / dist) : 1.0f;

            // Rotazione spaziale decrescente con la distanza (1/r)
            float twistAngle = (dragStrength / (dist + rs)) * lensSensitivity * (0.35f * (float) Math.sin(spaceTwist * 0.6f + starPhase[i] * 0.1f));
            float rx = dx * scale;
            float ry = dy * scale;
            float sx = cx + rx - ry * twistAngle;
            float sy = cy + ry + rx * twistAngle;

            if (sx < -15 || sx > w + 15 || sy < -15 || sy > h + 15) {
                continue;
            }

            // Interazione con eventuali onde d'urto gravitazionali in transito nello spazio
            float waveBoost = 0f;
            float distInRs = dist / Math.max(1f, rs);
            for (int k = 0; k < MAX_SHOCKWAVES; k++) {
                if (!waveActive[k]) continue;
                float diff = Math.abs(distInRs - waveRadius[k]);
                if (diff < 0.45f) {
                    waveBoost += (1f - diff / 0.45f) * waveAlpha[k] * 1.4f;
                }
            }

            // Magnificazione gravitazionale vicino all'anello di Einstein
            float ringDist = Math.abs(dist - einsteinR);
            float mag = 1.0f + (1.4f * lensSensitivity) / (1.0f + ringDist * 0.045f);
            float shimmer = 0.85f + 0.15f * (float) Math.sin(timeAccum * 2.2f + starPhase[i]);
            float bright = Math.min(1f, starBaseBright[i] * mag * shimmer * (0.70f + smoothedPower * 0.45f) + waveBoost);
            float size = starBaseSize[i] * (0.80f + 0.35f * Math.min(2f, mag) + waveBoost * 0.6f);

            g.setTransparency(bright);
            float arcFactor = (einsteinSq / (dist * dist + einsteinSq)) * lensSensitivity * (0.5f + gravityStrength * 0.4f) + waveBoost * 0.7f;
            if (arcFactor > 0.38f && dist > 1f) {
                float tx = -dy / dist;
                float ty = dx / dist;
                float halfArc = Math.min(9f, arcFactor * 4.5f);
                g.batchDrawLine(sx - tx * halfArc, sy - ty * halfArc,
                                sx + tx * halfArc, sy + ty * halfArc,
                                size * 0.85f, starTintR[i], starTintG[i], starTintB[i]);
            } else {
                g.batchDrawOval(sx, sy, size, 6, starTintR[i], starTintG[i], starTintB[i]);
            }
        }
        g.endBatch();
    }

    private void drawShockwaves(NvGraphic g, float cx, float cy, float rs) {
        int segments = 64;
        float step = (float) (Math.PI * 2.0 / segments);

        g.beginBatch();
        for (int w = 0; w < MAX_SHOCKWAVES; w++) {
            if (!waveActive[w]) continue;
            float r = waveRadius[w] * rs;
            float alpha = waveAlpha[w];
            float prevX = cx + r;
            float prevY = cy;
            for (int s = 1; s <= segments; s++) {
                float a = s * step;
                float nx = cx + (float) Math.cos(a) * r;
                float ny = cy + (float) Math.sin(a) * r;
                g.setTransparency(alpha * 0.45f);
                g.batchDrawLine(prevX, prevY, nx, ny, 4.0f + alpha * 3.0f, 0.45f, 0.72f, 1.0f);
                g.setTransparency(alpha * 0.85f);
                g.batchDrawLine(prevX, prevY, nx, ny, 1.6f + alpha * 1.4f, 1.0f, 0.85f, 0.52f);
                prevX = nx;
                prevY = ny;
            }
        }
        g.endBatch();
    }

    private void drawPolarJets(NvGraphic g, float cx, float cy, float rs) {
        float jetEnergy = Math.min(1f, peakPulse * 0.55f + smoothedHigh * 0.45f);
        if (jetEnergy < 0.07f) {
            return;
        }

        float jetLength = rs * (1.6f + jetEnergy * 2.6f);
        g.beginBatch();
        for (int layer = 0; layer < 4; layer++) {
            float t = layer / 4f;
            float w = (12f - layer * 2.5f) * (0.7f + jetEnergy * 0.6f);
            float alpha = jetEnergy * (0.16f - t * 0.03f);
            float len = jetLength * (0.55f + t * 0.45f);

            float jr = 0.50f + t * 0.30f;
            float jg = 0.68f + t * 0.25f;
            float jb = 1.0f;
            g.setTransparency(alpha);
            g.batchDrawLine(cx, cy - rs * 0.9f, cx, cy - rs * 0.9f - len, w, jr, jg, jb);
            g.batchDrawLine(cx, cy + rs * 0.9f, cx, cy + rs * 0.9f + len, w, jr, jg, jb);
        }
        g.endBatch();
    }

    private void computeDiskColor(float temp, float cosT, float[] outRgb) {
        float cr, cg, cb;
        if (temp > 0.82f) {
            float k = (temp - 0.82f) / 0.18f;
            cr = 0.82f + k * 0.18f;
            cg = 0.92f + k * 0.08f;
            cb = 1.0f;
        } else if (temp > 0.52f) {
            float k = (temp - 0.52f) / 0.30f;
            cr = 1.0f;
            cg = 0.70f + k * 0.22f;
            cb = 0.28f + k * 0.62f;
        } else if (temp > 0.22f) {
            float k = (temp - 0.22f) / 0.30f;
            cr = 0.88f + k * 0.12f;
            cg = 0.26f + k * 0.44f;
            cb = 0.06f + k * 0.22f;
        } else {
            float k = Math.max(0f, temp) / 0.22f;
            cr = 0.45f + k * 0.43f;
            cg = 0.07f + k * 0.19f;
            cb = 0.14f - k * 0.08f;
        }

        if (cosT < 0f) {
            float boost = -cosT;
            cb = Math.min(1f, cb + boost * 0.26f);
            cg = Math.min(1f, cg + boost * 0.12f);
        } else {
            cg *= (1.0f - cosT * 0.28f);
            cb *= (1.0f - cosT * 0.48f);
        }

        outRgb[0] = cr;
        outRgb[1] = cg;
        outRgb[2] = cb;
    }

    private static float archCurve(float s) {
        return s * (1.28f - 0.28f * s);
    }

    private void drawLensedFarDisk(NvGraphic g, float cx, float cy, float rs, float tilt) {
        float beamBoost = 1.0f + smoothedPower * 0.42f + peakPulse * 0.38f;
        float[] rgb = new float[3];

        g.beginBatch();
        for (int i = 0; i < DISK_PARTICLES; i++) {
            float theta = pTheta[i];
            float sinT = (float) Math.sin(theta);
            if (sinT >= 0f) {
                continue;
            }

            float rNorm = pNormRadius[i];
            float r = rNorm * rs;
            float cosT = (float) Math.cos(theta);
            float s = -sinT;

            float prevTheta = pPrevTheta[i];
            float prevSinT = (float) Math.sin(prevTheta);
            float prevCosT = (float) Math.cos(prevTheta);

            float temp = Math.max(0f, Math.min(1f, 1.0f - (rNorm - 1.12f) / 3.8f + pHeatBias[i] + smoothedHigh * 0.16f));
            float doppler = 1.0f - 0.52f * cosT;
            computeDiskColor(temp, cosT, rgb);

            float upperH = rs * 1.035f + (r - rs) * 0.36f;
            float lowerH = rs * 1.015f + (r - rs) * 0.20f;

            float cS = archCurve(s);
            float pinchU = 1.0f - 0.08f * s * s;
            float pinchL = 1.0f - 0.12f * s * s;

            float ux = cx + r * cosT * pinchU;
            float uy = cy - upperH * cS;

            float lx = cx + r * cosT * pinchL;
            float ly = cy + lowerH * cS;

            float upx, upy, lpx, lpy;
            if (prevSinT < 0f) {
                float pS = -prevSinT;
                float cpS = archCurve(pS);
                upx = cx + r * prevCosT * (1.0f - 0.08f * pS * pS);
                upy = cy - upperH * cpS;
                lpx = cx + r * prevCosT * (1.0f - 0.12f * pS * pS);
                lpy = cy + lowerH * cpS;
            } else {
                upx = cx + r * prevCosT;
                upy = cy + r * prevSinT * tilt;
                lpx = upx;
                lpy = upy;
            }

            float distFade = rNorm > 4.4f ? Math.max(0.15f, 1.0f - (rNorm - 4.4f) * 0.3f) : 1.0f;
            float alphaUpper = Math.min(0.95f, (0.22f + 0.58f * temp) * doppler * beamBoost * 0.72f * distFade);
            g.setTransparency(alphaUpper);
            g.batchDrawLine(upx, upy, ux, uy, pThickness[i] * (0.75f + 0.38f * temp), rgb[0], rgb[1], rgb[2]);

            float alphaLower = alphaUpper * 0.46f;
            g.setTransparency(alphaLower);
            g.batchDrawLine(lpx, lpy, lx, ly, pThickness[i] * 0.65f, rgb[0], rgb[1] * 0.85f, rgb[2] * 0.78f);
        }
        g.endBatch();
    }

    private void drawErgosphereAndInfall(NvGraphic g, float cx, float cy, float rs, float tilt) {
        float ergoEnergy = Math.min(1f, 0.35f + smoothedLow * 0.45f + peakPulse * 0.40f);
        g.beginBatch();

        for (int i = 0; i < ERGO_FILAMENTS; i++) {
            float a = ergoTheta[i];
            float r1 = rs * ergoRadius[i];
            float r2 = r1 * 1.07f;
            float a2 = a + 0.22f;

            float x1 = cx + (float) Math.cos(a) * r1;
            float y1 = cy + (float) Math.sin(a) * r1;
            float x2 = cx + (float) Math.cos(a2) * r2;
            float y2 = cy + (float) Math.sin(a2) * r2;

            g.setTransparency(0.22f * ergoEnergy);
            g.batchDrawLine(x1, y1, x2, y2, 1.5f, 0.68f, 0.86f, 1.0f);
        }

        for (int i = 0; i < INFALL_PARTICLES; i++) {
            float r = infRadius[i] * rs;
            float pr = infPrevRadius[i] * rs;
            float a = infTheta[i];
            float pa = infPrevTheta[i];

            float sinA = (float) Math.sin(a);
            float pSinA = (float) Math.sin(pa);

            float x1 = cx + (float) Math.cos(pa) * pr;
            float y1 = cy + pSinA * pr * (pSinA >= 0f ? tilt : 0.45f);
            float x2 = cx + (float) Math.cos(a) * r;
            float y2 = cy + sinA * r * (sinA >= 0f ? tilt : 0.45f);

            float prox = Math.max(0f, Math.min(1f, (infRadius[i] - 1.0f) / 0.48f));
            float cr = 0.85f + (1f - prox) * 0.15f;
            float cg = 0.45f + prox * 0.45f;
            float cb = 0.18f + prox * 0.72f;

            g.setTransparency(Math.min(0.85f, (0.28f + smoothedPower * 0.5f) * prox));
            g.batchDrawLine(x1, y1, x2, y2, 1.3f + (1f - prox) * 0.8f, cr, cg, cb);
        }
        g.endBatch();
    }

    private void drawEventHorizonAndPhotonSphere(NvGraphic g, float cx, float cy, float rs) {
        float pulseGlow = Math.min(1f, 0.48f + smoothedPower * 0.45f + peakPulse * 0.45f);

        g.beginBatch();
        g.setTransparency(0.14f * pulseGlow);
        g.batchDrawOval(cx, cy, rs * 1.26f, 48, 1.0f, 0.52f, 0.15f);

        g.setTransparency(0.26f * pulseGlow);
        g.batchDrawOval(cx, cy, rs * 1.13f, 48, 1.0f, 0.76f, 0.36f);

        g.setTransparency(0.82f * pulseGlow);
        g.batchDrawOval(cx, cy, rs * 1.045f, 56, 1.0f, 0.94f, 0.78f);

        g.setTransparency(0.65f * Math.min(1f, 0.4f + smoothedHigh * 0.6f + peakPulse * 0.4f));
        g.batchDrawOval(cx, cy, rs * 1.018f, 56, 0.70f, 0.88f, 1.0f);

        g.setTransparency(1.0f);
        g.batchDrawOval(cx, cy, rs, 56, 0f, 0f, 0f);
        g.endBatch();
    }

    private void drawSynchrotronFlares(NvGraphic g, float cx, float cy, float rs) {
        if (flareIntensity < 0.08f) {
            return;
        }

        float alpha = Math.min(0.65f, flareIntensity * 0.42f);
        int arcs = 6;
        g.beginBatch();
        for (int a = 0; a < arcs; a++) {
            float baseAngle = timeAccum * 1.8f + a * (float) (Math.PI * 2.0 / arcs);
            float rInner = rs * 1.03f;
            float rOuter = rs * (1.28f + flareIntensity * 0.25f);

            float x1 = cx + (float) Math.cos(baseAngle) * rInner;
            float y1 = cy + (float) Math.sin(baseAngle) * rInner;
            float xMid = cx + (float) Math.cos(baseAngle + 0.28f) * ((rInner + rOuter) * 0.5f);
            float yMid = cy + (float) Math.sin(baseAngle + 0.28f) * ((rInner + rOuter) * 0.5f);
            float x2 = cx + (float) Math.cos(baseAngle + 0.62f) * rOuter;
            float y2 = cy + (float) Math.sin(baseAngle + 0.62f) * rOuter;

            g.setTransparency(alpha);
            g.batchDrawLine(x1, y1, xMid, yMid, 2.2f, 0.58f, 0.84f, 1.0f);
            g.setTransparency(alpha * 0.6f);
            g.batchDrawLine(xMid, yMid, x2, y2, 1.4f, 0.75f, 0.62f, 1.0f);
        }
        g.endBatch();
    }

    private void drawNearAccretionDisk(NvGraphic g, float cx, float cy, float rs, float tilt) {
        float beamBoost = 1.0f + smoothedPower * 0.45f + peakPulse * 0.42f;
        float[] rgb = new float[3];

        g.beginBatch();
        for (int i = 0; i < DISK_PARTICLES; i++) {
            float theta = pTheta[i];
            float sinT = (float) Math.sin(theta);
            if (sinT < 0f) {
                continue;
            }

            float rNorm = pNormRadius[i];
            float r = rNorm * rs;
            float cosT = (float) Math.cos(theta);

            float prevTheta = pPrevTheta[i];
            float prevSinT = (float) Math.sin(prevTheta);
            float prevCosT = (float) Math.cos(prevTheta);

            float x = cx + r * cosT;
            float y = cy + r * sinT * tilt;

            float px, py;
            if (prevSinT >= 0f) {
                px = cx + r * prevCosT;
                py = cy + r * prevSinT * tilt;
            } else {
                float pS = -prevSinT;
                float cpS = archCurve(pS);
                float upperH = rs * 1.035f + (r - rs) * 0.36f;
                px = cx + r * prevCosT * (1.0f - 0.08f * pS * pS);
                py = cy - upperH * cpS;
            }

            float temp = Math.max(0f, Math.min(1f, 1.0f - (rNorm - 1.12f) / 3.8f + pHeatBias[i] + smoothedHigh * 0.18f));
            float doppler = 1.0f - 0.52f * cosT;
            computeDiskColor(temp, cosT, rgb);

            float distFade = rNorm > 4.4f ? Math.max(0.15f, 1.0f - (rNorm - 4.4f) * 0.3f) : 1.0f;
            float alpha = Math.min(0.98f, (0.28f + 0.62f * temp) * doppler * beamBoost * 0.86f * distFade);
            g.setTransparency(alpha);
            g.batchDrawLine(px, py, x, y, pThickness[i] * (0.9f + 0.45f * temp), rgb[0], rgb[1], rgb[2]);
        }
        g.endBatch();
    }
}
