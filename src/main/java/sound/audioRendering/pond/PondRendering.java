package sound.audioRendering.pond;

import nv.core.graphic.NvGraphic;
import sound.audioRendering.WavePlayer;
import sound.audioRendering.AudioRenderer;

public class PondRendering extends AudioRenderer {
    private final WavePlayer player;
    private int lastPeakIndex = -1;

    public PondRendering(WavePlayer player){
        this.player = player;
        reload(player.waveBuffer);
    }
    @Override
    public void renderInternal(NvGraphic g, short[] samples, float width, float height, float currentTime) {
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

    private void startCircle(int x, int y, float amplitude){
        player.addChild(new Circle(x,y, amplitude, average));
    }
}
