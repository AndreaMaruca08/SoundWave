package sound.audioRendering;

import nv.core.graphic.NvGraphic;

public abstract class AudioRenderer {
    protected static int WINDOW = 1024;
    protected static int MINIMUM = 5000;

    protected float[] peaks;
    protected float peakDuration;

    protected double average = 0.3f;

    protected boolean going = false;

    public void reload(short[] samples){
        peaks = new float[(samples.length + WINDOW - 1) / WINDOW];

        float sum = 0;
        long count = 0;

        peakDuration = WINDOW / 44100f;

        int index = 0;

        for(int i = 0; i < samples.length; i += WINDOW) {
            int max = 0;

            for(int j = i; j < i + WINDOW && j < samples.length; j++) {
                int value = Math.abs(samples[j]);

                if(value > MINIMUM) {
                    sum += value;
                    count++;
                }

                if(value > max)
                    max = value;
            }
            peaks[index++] = max / 32767f;
        }

        if(count > 0) average = (sum / count) / 32767f;
        else average = 0;
    }

    public void render(NvGraphic g, short[] samples, float width, float height, float currentTime){
        if(!going)
            return;
        renderInternal(g, samples, width, height, currentTime);
    }

    abstract protected void renderInternal(NvGraphic g, short[] samples, float width, float height, float currentTime);

    public void start(){
        going=true;
    }
    public void stop(){
        going = false;
    }
}
