package sound.audioRendering.pond;

import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;

public class Circle extends NvRgbComp {

    private final float normalizedPeak;

    private float wave = 10f;
    private int radius = 0;

    public Circle(int x, int y, float peak, double average) {
        super(x, y, 500, 500);

        double reference = Math.max(average*0.8f, 0.0001);

        this.normalizedPeak = (float) (peak / reference);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        int x = radius / 2 - radius;
        int y = radius / 2 - radius;

        int accuracy = radius > 200 ? 32 : 16;

        g.drawOval(x, y, radius, accuracy, 1, 0, 0);
        g.drawOval(x, y, radius - 1, accuracy, 0, 0, 0);
    }

    @Override
    public void update(float dt) {
        wave += 500 * dt;
        radius = (int) Math.min(500, wave);

        double life = 50 + Math.pow(Math.pow(normalizedPeak, 2) * 20, 0.8f);

        if (wave > life || wave >= 500) {
            destroy();
        }
    }
}