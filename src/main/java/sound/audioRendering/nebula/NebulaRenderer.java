package sound.audioRendering.nebula;

import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;
import sound.audioRendering.WavePlayer;

import java.util.Random;

/**
 * Nebulosa audio-reattiva: particelle che orbitano attorno a diversi "nuclei" energetici,
 * cambiano colore lungo tutto lo spettro in base all'intensità del suono, e accelerano
 * violentemente sui picchi (beat), con esplosioni cromatiche radiali ad ogni impulso forte.
 */
public class NebulaRenderer extends AudioRenderer {

    private static final int PARTICLE_COUNT = 1000;
    private static final int NUCLEI_COUNT = 8;

    private final Random random = new Random();

    private Particle[] particles;
    private Nucleus[] nuclei;

    private int lastPeakIndex = -1;

    private float hueScroll = 0f;
    private float globalPulse = 0f;

    private static class Particle {
        float x, y;
        float vx, vy;
        int nucleusIndex;
        float orbitRadius;
        float orbitAngle;
        float orbitSpeed;
        float hueOffset;
        float size;
        float energy;
    }

    private static class Nucleus {
        float x, y;
        float vx, vy;
        float hue;
    }

    public NebulaRenderer(WavePlayer player) {
        createNuclei(player.getW(), player.getH() * 0.8f);
        createParticles(player.getW(), player.getH() * 0.8f);
        reload(player.waveBuffer);
    }

    private void createNuclei(float w, float h) {
        nuclei = new Nucleus[NUCLEI_COUNT];
        for (int i = 0; i < NUCLEI_COUNT; i++) {
            Nucleus n = new Nucleus();
            n.x = random.nextFloat() * w;
            n.y = random.nextFloat() * h;
            n.vx = (random.nextFloat() - 0.5f) * 0.4f;
            n.vy = (random.nextFloat() - 0.5f) * 0.4f;
            n.hue = random.nextFloat();
            nuclei[i] = n;
        }
    }

    private void createParticles(float w, float h) {
        particles = new Particle[PARTICLE_COUNT];
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.nucleusIndex = random.nextInt(NUCLEI_COUNT);

            p.orbitRadius = 15f + random.nextFloat() * 140f;
            p.orbitAngle = random.nextFloat() * (float) (Math.PI * 2);
            p.orbitSpeed = (0.3f + random.nextFloat() * 1.2f) * (random.nextBoolean() ? 1 : -1);

            Nucleus n = nuclei[p.nucleusIndex];
            p.x = n.x + (float) Math.cos(p.orbitAngle) * p.orbitRadius;
            p.y = n.y + (float) Math.sin(p.orbitAngle) * p.orbitRadius;

            p.hueOffset = random.nextFloat();
            p.size = 1f + random.nextFloat() * 2.5f;
            p.energy = random.nextFloat() * 0.15f;

            particles[i] = p;
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
                globalPulse += impulse * 1.5f;
            }
        }

        updateNuclei(width, height, audioPower);
        updateParticles(width, height, audioPower, impulse);

        drawNuclei(g);
        drawParticles(g);

        globalPulse *= 0.90f;
        hueScroll += 0.0006f + audioPower * 0.004f;
        if (hueScroll > 1f) hueScroll -= 1f;
    }

    private void updateNuclei(float width, float height, float audioPower) {
        for (Nucleus n : nuclei) {
            float speedFactor = 0.2f + audioPower * 3f + globalPulse * 2f;

            n.x += n.vx * speedFactor;
            n.y += n.vy * speedFactor;

            if (n.x < 0 || n.x > width) n.vx *= -1;
            if (n.y < 0 || n.y > height) n.vy *= -1;

            n.hue += 0.0003f + audioPower * 0.002f;
            if (n.hue > 1f) n.hue -= 1f;
        }
    }

    private void updateParticles(float width, float height, float audioPower, float impulse) {
        for (Particle p : particles) {
            p.energy *= 0.92f;
            p.energy += impulse * 0.8f;

            Nucleus n = nuclei[p.nucleusIndex];

            float speedFactor = 1f + audioPower * 8f + p.energy * 10f;
            p.orbitAngle += p.orbitSpeed * 0.03f * speedFactor;

            float radiusBoost = 1f + p.energy * 2.2f;
            float radius = p.orbitRadius * radiusBoost;

            float targetX = n.x + (float) Math.cos(p.orbitAngle) * radius;
            float targetY = n.y + (float) Math.sin(p.orbitAngle) * radius;

            p.x += (targetX - p.x) * 0.35f;
            p.y += (targetY - p.y) * 0.35f;

            if (p.x < -20 || p.x > width + 20 || p.y < -20 || p.y > height + 20) {
                p.orbitAngle += (float) Math.PI;
            }
        }
    }

    private void drawNuclei(NvGraphic g) {
        g.beginBatch();
        for (Nucleus n : nuclei) {
            float[] rgb = hsvToRgb(n.hue, 0.6f, 0.9f);
            float glowSize = 12f + globalPulse * 20f;
            g.batchDrawOval(n.x, n.y, glowSize, 20, rgb[0] * 0.4f, rgb[1] * 0.4f, rgb[2] * 0.4f);
        }
        g.endBatch();

        g.beginBatch();
        for (Nucleus n : nuclei) {
            float[] rgb = hsvToRgb(n.hue, 0.4f, 1f);
            float coreSize = 5f + globalPulse * 8f;
            g.batchDrawOval(n.x, n.y, coreSize, 16, rgb[0], rgb[1], rgb[2]);
        }
        g.endBatch();
    }

    private void drawParticles(NvGraphic g) {
        g.beginBatch();
        for (Particle p : particles) {
            Nucleus n = nuclei[p.nucleusIndex];
            float hue = frac(n.hue + hueScroll + p.hueOffset * 0.3f);

            float brightness = Math.min(1f, 0.45f + p.energy * 1.6f + globalPulse * 0.3f);
            float saturation = 0.75f + p.energy * 0.25f;

            float[] rgb = hsvToRgb(hue, Math.min(saturation, 1f), brightness);

            float size = p.size + p.energy * 10f;

            g.batchDrawOval(p.x, p.y, size, 8, rgb[0], rgb[1], rgb[2]);
        }
        g.endBatch();
    }

    private float[] hsvToRgb(float h, float s, float v) {
        float r, gg, b;

        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);

        switch (i % 6) {
            case 0 -> { r = v; gg = t; b = p; }
            case 1 -> { r = q; gg = v; b = p; }
            case 2 -> { r = p; gg = v; b = t; }
            case 3 -> { r = p; gg = q; b = v; }
            case 4 -> { r = t; gg = p; b = v; }
            default -> { r = v; gg = p; b = q; }
        }

        return new float[]{r, gg, b};
    }

    private float frac(float x) {
        return x - (float) Math.floor(x);
    }
}