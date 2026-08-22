package sound.audioRendering.reactor;

import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;
import sound.audioRendering.WavePlayer;

import java.util.Random;

public class ReactorRendering extends AudioRenderer {

    private static final int ION_COUNT = 900;
    private static final int CONTAINMENT_SIDES = 6;
    private static final int MAX_ARCS = 10;

    private final Random random = new Random();

    private Ion[] ions;
    private PlasmaArc[] arcs;

    private float centerX, centerY;
    private float containmentRadius;

    private int lastPeakIndex = -1;
    private float corePulse = 0f;
    private float coreRotation = 0f;
    private float meltdownFactor = 0f;

    private static class Ion {
        float orbitRadius;
        float orbitAngle;
        float orbitSpeed;
        float verticalWobble;
        float wobblePhase;
        float charge;
        float energy;
        float size;
        float x, y;
    }

    private static class PlasmaArc {
        float angle;
        float length;
        float life;
        float jitterSeed;
        boolean active;
    }

    public ReactorRendering(WavePlayer player) {
        centerX = player.getW() / 2f;
        centerY = (player.getH() * 0.8f) / 2f;
        containmentRadius = Math.min(player.getW(), player.getH() * 0.8f) * 0.38f;

        createIons();
        createArcs();
        reload(player.waveBuffer);
    }

    private void createIons() {
        ions = new Ion[ION_COUNT];
        for (int i = 0; i < ION_COUNT; i++) {
            Ion ion = new Ion();
            ion.orbitRadius = 20f + random.nextFloat() * (containmentRadius - 30f);
            ion.orbitAngle = random.nextFloat() * (float) (Math.PI * 2);
            ion.orbitSpeed = (0.1f + random.nextFloat() * 1.6f) * (random.nextBoolean() ? 1 : -1);
            ion.verticalWobble = 4f + random.nextFloat() * 10f;
            ion.wobblePhase = random.nextFloat() * (float) (Math.PI * 2);
            ion.charge = random.nextFloat();
            ion.energy = random.nextFloat() * 0.2f;
            ion.size = 1f + random.nextFloat() * 1.8f;
            ions[i] = ion;
        }
    }

    private void createArcs() {
        arcs = new PlasmaArc[MAX_ARCS];
        for (int i = 0; i < MAX_ARCS; i++) {
            PlasmaArc a = new PlasmaArc();
            a.active = false;
            arcs[i] = a;
        }
    }

    @Override
    public void renderInternal(
            NvGraphic g,
            short[] samples,
            float width,
            float height,
            float currentTime
    ) {
        int index = (int) ((currentTime / 1000f) / peakDuration);

        float audioPower = 0;
        float impulse = 0;

        if (index >= 0 && index < peaks.length) {
            audioPower = peaks[index];

            if (index != lastPeakIndex) {
                lastPeakIndex = index;
                impulse = (float) Math.max(0, audioPower - average);
                corePulse += impulse * 2f;

                if (impulse > 0.08f) {
                    triggerArcs(impulse);
                }
            }
        }

        meltdownFactor += (audioPower - meltdownFactor) * 0.02f;

        updateIons(audioPower);
        updateArcs();

        drawContainmentField(g, audioPower);
        drawArcs(g);
        drawIons(g);
        drawCore(g);

        corePulse *= 0.90f;
        coreRotation += 0.004f + audioPower * 0.03f;
    }

    private void triggerArcs(float impulse) {
        int toFire = 2 + (int) (impulse * 12f);
        for (int i = 0; i < arcs.length && toFire > 0; i++) {
            if (!arcs[i].active) {
                arcs[i].active = true;
                arcs[i].angle = random.nextFloat() * (float) (Math.PI * 2);
                arcs[i].length = containmentRadius * (0.5f + random.nextFloat() * 0.5f);
                arcs[i].life = 1f;
                arcs[i].jitterSeed = random.nextFloat() * 1000f;
                toFire--;
            }
        }
    }

    private void updateIons(float audioPower) {
        for (Ion ion : ions) {
            ion.energy *= 0.93f;

            float speedFactor = 0.2f + audioPower * 5f + ion.energy * 3f;
            ion.orbitAngle += ion.orbitSpeed * 0.05f * speedFactor;
            ion.wobblePhase += 0.15f * speedFactor;

            float radiusJitter = 1f + ion.energy * 0.6f;
            float r = ion.orbitRadius * radiusJitter;

            float wobble = (float) Math.sin(ion.wobblePhase) * ion.verticalWobble;

            ion.x = centerX + (float) Math.cos(ion.orbitAngle) * r;
            ion.y = centerY + (float) Math.sin(ion.orbitAngle) * r * 0.6f + wobble;
        }
    }

    private void updateArcs() {
        for (PlasmaArc a : arcs) {
            if (!a.active) continue;
            a.life -= 0.09f;
            if (a.life <= 0f) a.active = false;
        }
    }

    private void drawCore(NvGraphic g) {
        float pulseSize = 22f + corePulse * 30f + (float) Math.sin(coreRotation * 3f) * 3f;

        float coreR = lerp(0.15f, 1f, meltdownFactor * 1.3f);
        float coreG = lerp(0.9f, 0.5f, meltdownFactor);
        float coreB = lerp(0.4f, 0.3f, meltdownFactor);

        g.beginBatch();
        g.batchDrawOval(centerX, centerY, pulseSize * 2.6f, 32, coreR * 0.2f, coreG * 0.35f, coreB * 0.2f);
        g.batchDrawOval(centerX, centerY, pulseSize * 1.6f, 32, coreR * 0.4f, coreG * 0.7f, coreB * 0.4f);
        g.batchDrawOval(centerX, centerY, pulseSize * 0.7f, 24,
                Math.min(1f, coreR + 0.3f), Math.min(1f, coreG + 0.2f), Math.min(1f, coreB + 0.3f));
        g.endBatch();
    }

    private void drawContainmentField(NvGraphic g, float audioPower) {
        float vibration = audioPower * 8f + corePulse * 5f;
        float radius = containmentRadius + (float) Math.sin(coreRotation * 6f) * vibration;

        for (int i = 0; i < CONTAINMENT_SIDES; i++) {
            float a1 = (float) (i * 2 * Math.PI / CONTAINMENT_SIDES) + coreRotation * 0.5f;
            float a2 = (float) ((i + 1) * 2 * Math.PI / CONTAINMENT_SIDES) + coreRotation * 0.5f;

            float jitterA = (random.nextFloat() - 0.5f) * vibration * 0.3f;
            float jitterB = (random.nextFloat() - 0.5f) * vibration * 0.3f;

            float x1 = centerX + (float) Math.cos(a1) * (radius + jitterA);
            float y1 = centerY + (float) Math.sin(a1) * (radius + jitterA) * 0.6f;
            float x2 = centerX + (float) Math.cos(a2) * (radius + jitterB);
            float y2 = centerY + (float) Math.sin(a2) * (radius + jitterB) * 0.6f;

            float intensity = Math.min(1f, 0.3f + audioPower * 0.7f + corePulse * 0.3f);

            g.drawLine(x1, y1, x2, y2, 1.5f, intensity * 0.3f, intensity, intensity * 0.5f);
        }
    }

    private void drawArcs(NvGraphic g) {
        for (PlasmaArc a : arcs) {
            if (!a.active) continue;

            int segments = 6;
            float prevX = centerX;
            float prevY = centerY;

            for (int s = 1; s <= segments; s++) {
                float t = (float) s / segments;
                float dist = a.length * t;

                float jitter = (float) Math.sin(a.jitterSeed + s * 12.9f) * (a.length * 0.08f) * (1f - t);

                float perpX = -(float) Math.sin(a.angle);
                float perpY = (float) Math.cos(a.angle);

                float px = centerX + (float) Math.cos(a.angle) * dist + perpX * jitter;
                float py = centerY + (float) Math.sin(a.angle) * dist * 0.6f + perpY * jitter;

                float alpha = a.life;

                g.drawLine(prevX, prevY, px, py, 1.2f * a.life, 0.4f * alpha, 1f * alpha, 0.6f * alpha);

                prevX = px;
                prevY = py;
            }
        }
    }

    private void drawIons(NvGraphic g) {
        g.beginBatch();
        for (Ion ion : ions) {
            float brightness = Math.min(1f, 0.4f + ion.energy * 1.8f + corePulse * 0.2f);
            float size = ion.size + ion.energy * 6f;

            float rr = lerp(0.15f, 0.9f, ion.charge * 0.3f + meltdownFactor * 0.5f) * brightness;
            float gg = brightness;
            float bb = lerp(0.5f, 0.8f, ion.charge) * brightness;

            g.batchDrawOval(ion.x, ion.y, size, 8, rr, gg, bb);
        }
        g.endBatch();
    }

    private float lerp(float a, float b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return a + (b - a) * t;
    }
}