package sound.audioRendering;

import nv.core.graphic.NvGraphic;

public abstract class AudioRenderer {
    protected int WINDOW = 1024;
    protected int MINIMUM = 5000;

    protected float[] peaks;
    protected float peakDuration;

    protected double average = 0.3f;

    protected boolean going = false;

    public void reload(short[] samples){
        int n = samples.length;
        int peakCount = (n + WINDOW - 1) / WINDOW;
        peaks = new float[peakCount];
        peakDuration = WINDOW / 44100f;

        float sum = 0;
        long count = 0;
        int peakIdx = 0;

        for (int i = 0; i < n; i += WINDOW) {
            int end = Math.min(i + WINDOW, n);
            int max = 0;
            for (int j = i; j < end; j++) {
                int val = samples[j];
                if (val < 0) val = -val;
                if (val > MINIMUM) {
                    sum += val;
                    count++;
                }
                if (val > max) {
                    max = val;
                }
            }
            peaks[peakIdx++] = max * (1.0f / 32767f);
        }

        if (count > 0) {
            average = (sum / (double) count) * (1.0 / 32767.0);
        } else {
            average = 0;
        }
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
