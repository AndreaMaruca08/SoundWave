package sound.audioRendering;

import nv.core.graphic.NvGraphic;

public interface AudioRenderer {
    void reload(short[] samples);
    void render(NvGraphic g, short[] samples, float width, float height, float currentTime);
    void start();
    void stop();
}
