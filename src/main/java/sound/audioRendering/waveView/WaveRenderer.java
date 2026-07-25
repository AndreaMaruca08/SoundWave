package sound.audioRendering.waveView;

import nv.core.NvContext;
import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;
import sound.audioRendering.LineUser;

import java.awt.*;

public class WaveRenderer extends AudioRenderer implements LineUser {
    private final float red, green, blue;

    private final float w;
    private float lineX1;
    private final float lineY2;

    public WaveRenderer(Color color, float h, float w){
        this.red = color.getRed() / 255f;
        this.green = color.getGreen() / 255f;
        this.blue = color.getBlue() / 255f;
        lineY2 = h*0.8f;
        this.w = w;
    }

    @Override
    public void reload(short[] samples) {}

    @Override
    public void render(NvGraphic g,
                       short[] samples,
                       float width,
                       float height,
                       float currentTime
    ) {
        int pixels = (int) width;
        int samplesPerPixel = samples.length / pixels;
        float center = height / 2f;

        for(int x = 0; x < pixels; x++) {

            int start = x * samplesPerPixel;
            int end = Math.min(
                    start + samplesPerPixel,
                    samples.length
            );

            short min = Short.MAX_VALUE;
            short max = Short.MIN_VALUE;

            for(int i = start; i < end; i++) {
                short s = samples[i];
                if(s < min)
                    min = s;
                if(s > max)
                    max = s;
            }

            float y1 = center + (min / 32768f) * height / 2;
            float y2 = center + (max / 32768f) * height / 2;

            g.drawLine(
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
        g.drawLine(lineX1, 0f, lineX1, lineY2, 3);
    }

    @Override
    public void renderInternal(
            NvGraphic g,
            short[] samples,
            float width,
            float height,
            float currentTime
    ) {}

    public void updateLine(float percentage){
        if(!going)
            return;
        lineX1 = w * (percentage/100);
        NvContext.markSceneDirty();

    }
}
