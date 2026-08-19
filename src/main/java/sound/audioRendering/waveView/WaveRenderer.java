package sound.audioRendering.waveView;

import nv.core.NvContext;
import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;
import sound.audioRendering.LineUser;

import java.awt.Color;

public class WaveRenderer extends AudioRenderer implements LineUser {

    private final float red;
    private final float green;
    private final float blue;

    private final float w;
    private final float lineY2;

    private float lineX1;

    private short[] minPeaks;
    private short[] maxPeaks;

    public WaveRenderer(Color color, float h, float w) {
        this.red = color.getRed() / 255f;
        this.green = color.getGreen() / 255f;
        this.blue = color.getBlue() / 255f;

        this.lineY2 = h * 0.8f;
        this.w = w;
    }

    @Override
    public void reload(short[] samples) {

        int pixels = Math.max(1, (int) w);

        minPeaks = new short[pixels];
        maxPeaks = new short[pixels];

        int samplesPerPixel = Math.max(
                1,
                samples.length / pixels
        );

        for (int x = 0; x < pixels; x++) {

            int start = x * samplesPerPixel;

            if (start >= samples.length) {
                minPeaks[x] = 0;
                maxPeaks[x] = 0;
                continue;
            }

            int end = Math.min(
                    start + samplesPerPixel,
                    samples.length
            );

            short min = Short.MAX_VALUE;
            short max = Short.MIN_VALUE;

            for (int i = start; i < end; i++) {
                short sample = samples[i];

                if (sample < min)
                    min = sample;

                if (sample > max)
                    max = sample;
            }

            minPeaks[x] = min;
            maxPeaks[x] = max;
        }
    }

    @Override
    public void render(
            NvGraphic g,
            short[] samples,
            float width,
            float height,
            float currentTime
    ) {

        if (minPeaks == null || maxPeaks == null)
            return;

        float center = height / 2f;
        float halfHeight = height / 2f;

        g.beginBatch();
        for (int x = 0; x < minPeaks.length; x++) {

            float y1 = center + (minPeaks[x] / 32768f) * halfHeight;

            float y2 = center + (maxPeaks[x] / 32768f) * halfHeight;

            g.batchDrawLine(
                    x,
                    y1,
                    x,
                    y2,
                    1,
                    red,
                    green,
                    blue
            );
        }
        g.endBatch();

        g.drawLine(
                lineX1,
                0f,
                lineX1,
                lineY2,
                3
        );
    }

    @Override
    public void renderInternal(
            NvGraphic g,
            short[] samples,
            float width,
            float height,
            float currentTime
    ) {
    }

    @Override
    public void updateLine(float percentage) {

        if (!going)
            return;

        lineX1 = w * (percentage / 100f);

        NvContext.markSceneDirty();
    }
}