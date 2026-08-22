package sound.audioRendering.galaxy;

import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;
import sound.audioRendering.WavePlayer;

import java.util.Random;

public class GalaxyRendering extends AudioRenderer {

    private static final int STAR_COUNT = 2500;

    private final Random random = new Random();

    private Star[] stars;

    private float centerX, centerY;
    private float galaxyRadius;

    private int lastPeakIndex = -1;
    private float rotationSpeed = 0.15f;
    private float globalRotation = 0f;
    private float pulse = 0f;

    private static class Star {
        float orbitRadius;
        float orbitAngle;
        float orbitSpeedFactor;
        float x, y;
        float baseR, baseG, baseB;
        float whiteness;
        float whitenessTarget;
        float twinkleTimer;
        float size;
    }

    public GalaxyRendering(WavePlayer player) {
        centerX = player.getW() / 2f;
        centerY = (player.getH() * 0.8f) / 2f;
        galaxyRadius = Math.min(player.getW(), player.getH() * 0.8f) * 0.45f;

        createStars();
        reload(player.waveBuffer);
    }

    private void createStars() {
        stars = new Star[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            Star s = new Star();

            float t = random.nextFloat();
            s.orbitRadius = (float) Math.sqrt(t) * galaxyRadius;
            s.orbitAngle = random.nextFloat() * (float) (Math.PI * 2);

            s.orbitSpeedFactor = (1.2f - Math.min(s.orbitRadius / galaxyRadius, 1f) * 0.9f)
                    * (random.nextBoolean() ? 1f : 1f);

            assignZoneColor(s);

            s.whiteness = random.nextFloat() * 0.3f;
            s.whitenessTarget = s.whiteness;
            s.twinkleTimer = random.nextFloat() * 2f;

            s.size = 0.8f + random.nextFloat() * 1.6f;

            stars[i] = s;
        }
    }

    private void assignZoneColor(Star s) {
        float distRatio = s.orbitRadius / galaxyRadius;

        if (distRatio < 0.35f) {
            s.baseR = 1f;
            s.baseG = 0.75f + random.nextFloat() * 0.15f;
            s.baseB = 0.35f + random.nextFloat() * 0.2f;
        } else if (distRatio < 0.7f) {
            s.baseR = 0.55f + random.nextFloat() * 0.2f;
            s.baseG = 0.35f + random.nextFloat() * 0.2f;
            s.baseB = 0.85f + random.nextFloat() * 0.15f;
        } else {
            s.baseR = 0.3f + random.nextFloat() * 0.15f;
            s.baseG = 0.55f + random.nextFloat() * 0.2f;
            s.baseB = 0.95f + random.nextFloat() * 0.05f;
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
                pulse += impulse * 2f;
            }
        }

        rotationSpeed = 0.15f + audioPower * 3.5f + pulse * 2f;
        globalRotation += rotationSpeed * 0.016f;

        updateStars(audioPower, impulse);

        drawCore(g, audioPower);
        drawStars(g);

        pulse *= 0.9f;
    }

    private void updateStars(float audioPower, float impulse) {
        for (Star s : stars) {
            s.orbitAngle += rotationSpeed * 0.02f * s.orbitSpeedFactor;

            s.x = centerX + (float) Math.cos(s.orbitAngle + globalRotation * 0.0f) * s.orbitRadius;
            s.y = centerY + (float) Math.sin(s.orbitAngle) * s.orbitRadius * 0.55f;

            s.twinkleTimer -= 0.016f;
            if (s.twinkleTimer <= 0f) {
                s.whitenessTarget = random.nextFloat();
                s.twinkleTimer = 0.3f + random.nextFloat() * 1.5f;
            }

            float boosted = Math.min(1f, s.whitenessTarget + audioPower * 0.6f + impulse * 1.5f);
            s.whiteness += (boosted - s.whiteness) * 0.08f;
        }
    }

    private void drawStars(NvGraphic g) {
        g.beginBatch();
        for (Star s : stars) {
            float w = s.whiteness;

            float dr = s.baseR + (1f - s.baseR) * w;
            float dg = s.baseG + (1f - s.baseG) * w;
            float db = s.baseB + (1f - s.baseB) * w;

            float darken = 0.3f + w * 0.7f;

            float size = s.size * (0.7f + w * 0.8f);

            g.batchDrawOval(s.x, s.y, size, 6, dr * darken, dg * darken, db * darken);
        }
        g.endBatch();
    }

    private void drawCore(NvGraphic g, float audioPower) {
        float coreSize = 18f + pulse * 12f;

        g.beginBatch();
        g.batchDrawOval(centerX, centerY, coreSize * 3f, 32, 0.4f, 0.3f, 0.5f);
        g.batchDrawOval(centerX, centerY, coreSize * 1.6f, 28, 0.8f, 0.65f, 0.55f);
        g.batchDrawOval(centerX, centerY, coreSize * 0.7f, 20, 1f, 0.95f, 0.85f);
        g.endBatch();
    }
}