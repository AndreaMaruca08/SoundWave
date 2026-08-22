package sound.audioRendering;

import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.Clickable;
import nv.core.io.Hoverable;

public class TitleButton extends NvComp implements Clickable, Hoverable {
    private String title;
    private final int index;
    private final WavePlayer player;

    public TitleButton(int x, int y, int w, int h, String title, int index, WavePlayer player) {
        super(x, y, w, h);
        this.title = " "+ title;
        this.index = index;
        this.player = player;
    }

    public void setTitle(String title) {
        this.title = " "+title;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        if(isHovered){
            g.setRGB(0.05f,0.05f,0.05f);
            g.drawRoundRect(0,0, getW(), getH(), 20);
        }
        g.setRGB(1,1,1);
        g.drawText(title, 0,0, 1.03f);
    }

    @Override
    public void update(float dt) {}

    @Override
    public void onClick(int x, int y) {
        player.setTo(index);
    }

    @Override
    public void onClickRelease(int x, int y) {

    }
}
