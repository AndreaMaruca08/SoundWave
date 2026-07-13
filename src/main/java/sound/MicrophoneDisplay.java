package sound;

import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

public class MicrophoneDisplay extends NvComp {
    private final Microphone mic;
    private short[] waveBuffer;

    public MicrophoneDisplay(int x, int y, int w, int h, Microphone microphone) {
        super(x, y, w, h);
        if(!microphone.isAlive() || microphone.isInterrupted()){
            throw new RuntimeException("Microphone is not running");
        }
        this.mic = microphone;
    }

    @Override
    public void drawIntern(NvGraphic g) {

        float amplitude = getH();
        float centerY = getH() / 2f;

        float width = getW();


        for (int i = 0; i < waveBuffer.length - 1; i++) {

            float x1 = (i / (float)(waveBuffer.length - 1)) * width;

            float x2 = ((i + 1) / (float)(waveBuffer.length - 1)) * width;


            float y1 = centerY +
                    (waveBuffer[i] / 32768f) * amplitude;

            float y2 = centerY +
                    (waveBuffer[i + 1] / 32768f) * amplitude;


            g.drawLine(
                    x1, y1,
                    x2, y2,
                    4f,
                    0f,
                    1f,
                    0f
            );
        }
    }

    @Override
    public void update(float dt) {
        waveBuffer = mic.getWaveBuffer();
        markDirty();
    }
}
