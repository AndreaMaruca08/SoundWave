package sound.audioRendering;

import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.Clickable;

import java.awt.*;

public class PlayButton extends NvRgbComp implements Clickable {
    private String display;
    private Runnable action = () -> {};

    public PlayButton(int x, int y, int w, int h, Color color, String display) {
        super(x, y, w, h);
        this.display = display;
        setRgb(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
    }

    public void setAction(Runnable action) {
        this.action = action;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.setRGB(r,this.g, b);
        g.drawRoundRect(0,0,getW(), getH(), 10);
        g.setRGB(1,1,1);
        g.drawText(display, 0,0);
    }

    @Override
    public void onClick(int x, int y) {}

    @Override
    public void onClickRelease(int x, int y) {
        action.run();
    }

    @Override
    public void update(float dt) {

    }
}
