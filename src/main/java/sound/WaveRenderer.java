package sound;

import nv.core.graphic.NvGraphic;

public class WaveRenderer {
    public static void drawWaveform(
            NvGraphic g,
            short[] samples,
            float width,
            float height
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
                    0,
                    1,
                    0
            );
        }
    }
}
