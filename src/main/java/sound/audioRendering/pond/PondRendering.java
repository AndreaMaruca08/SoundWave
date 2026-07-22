package sound.audioRendering.pond;

import nv.core.graphic.NvGraphic;
import sound.WavePlayer;
import sound.audioRendering.AudioRenderer;

public class PondRendering implements AudioRenderer {
    private static final int WINDOW = 8192;
    private static final int MINIMUM = 4000;

    private float[] peaks;
    private double average = 0.3f;

    private float peakDuration;
    private final WavePlayer player;
    private int lastPeakIndex = -1;

    private boolean going = false;

    public PondRendering(WavePlayer player){
        this.player = player;
        reload(player.waveBuffer);
    }
    @Override
    public void render(NvGraphic g, short[] samples, float width, float height, float currentTime) {
        if(!going)
            return;
        int index = (int)((currentTime / 1000f) / peakDuration);
        if (index >= 0 && index < peaks.length && index != lastPeakIndex) {

            lastPeakIndex = index;

            float amplitude = peaks[index];

            if (amplitude > average) {
                startCircle(getRandomInt(width), getRandomInt(height), amplitude);
            }
        }
    }

    public int getRandomInt(float i){
        return (int) (i * 0.2 +  (Math.random() * i*0.8f));
    }

    @Override
    public void start() {
        going = true;
    }

    @Override
    public void stop() {
        going = false;
    }

    private void startCircle(int x, int y, float amplitude){
        player.addChild(new Circle(x,y, amplitude, average));
    }

    public void reload(short[] samples) {
        peaks = new float[(samples.length + WINDOW - 1) / WINDOW];

        float sum = 0f;
        peakDuration = WINDOW / 44100f;

        int p = 0;
        long num = 0;

        for (int i = 0; i < samples.length; i += WINDOW) {
            int max = 0;
            for (int j = i; j < i + WINDOW && j < samples.length; j++) {
                int value = Math.abs(samples[j]);

                if(value > MINIMUM){
                    sum += value;
                    num++;
                }

                if (value > max)
                    max = value;
            }
            peaks[p++] = max / 32767.0f;
        }
        this.average = (sum / (float) num) / 32767.0f;
    }
}
