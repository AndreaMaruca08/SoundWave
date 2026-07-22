package sound.audioRendering;

import nv.core.UpdateCycle;
import nv.core.graphic.NvGraphic;

public interface AudioRenderer {
    void render(NvGraphic g, short[] samples, float width, float height);
}
