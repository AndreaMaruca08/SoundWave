package sound.audioRendering;

import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.Clickable;
import nv.core.io.Hoverable;

public class TitleButton extends NvComp implements Clickable, Hoverable {
    private String title;
    private final String fullPath;
    private final int index;
    private final WavePlayer player;
    private boolean visible = true;

    public TitleButton(int x, int y, int w, int h, String fullPath, int index, WavePlayer player) {
        super(x, y, w, h);
        this.fullPath = fullPath;
        this.title = " "+ fullPath;
        this.index = index;
        this.player = player;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            markDirty();
        }
    }

    @Override
    public boolean isInside(int x, int y) {
        if (!visible) {
            return false;
        }
        return super.isInside(x, y);
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setTitle(String title) {
        this.title = " "+title;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        if (!visible) {
            return;
        }
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
