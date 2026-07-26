package sound.audioRendering.game;

import nv.core.graphic.NvGraphic;
import nv.core.io.KeyboardListener;
import org.lwjgl.glfw.GLFW;
import sound.audioRendering.AudioRenderer;

import java.util.ArrayList;
import java.util.List;

public class GameView extends AudioRenderer implements KeyboardListener {

    private boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST];

    private int points = 0;
    private int combo = 0;

    private List<Float> actualPeaks;

    private int firstPeak = 0;
    private boolean error = false;

    private boolean pressed = false;

    private static final float TRAVEL_TIME = 2000f;

    private static final int POINT_PER_PEAK = 1000;

    private static final int PROMINENCE_WINDOW = 5;
    private static final float MIN_PROMINENCE = 0.08f;
    private static final int MIN_PEAK_DISTANCE_MS = 100;

    private boolean[] validPeak;

    public GameView() {
        WINDOW = 8196;
    }

    @Override
    protected void renderInternal(NvGraphic g,
                                  short[] samples,
                                  float width,
                                  float height,
                                  float currentTime) {
        if(keys[GLFW.GLFW_KEY_R]){
            points = 0;
            combo = 0;
            reset();
        }else if(keys[GLFW.GLFW_KEY_SPACE] ||
                keys[GLFW.GLFW_KEY_B] ||
                keys[GLFW.GLFW_KEY_V] ||
                keys[GLFW.GLFW_KEY_H] ||
                keys[GLFW.GLFW_KEY_G]){
            pressed = true;
        }

        float centerX = width / 2f;
        float centerY = height / 1.3f;

        float mainRadius = width * 0.05f;

        drawIncoming(g, currentTime, centerX, centerY, mainRadius);

        drawSemiOval(
                g,
                centerX,
                centerY,
                mainRadius,
                1,
                0,
                0
        );

        g.drawText("PT: " + points + " | Combo: " + combo,centerX*0.9f, centerY*0.9f);
    }
    private void reset(){
        actualPeaks = new ArrayList<>(peaks.length);

        for (float peak : peaks) {
            actualPeaks.add(peak);
        }

        firstPeak = 0;
        computeValidPeaks();
    }

    private void drawIncoming(NvGraphic g,
                              float currentTime,
                              float centerX,
                              float centerY,
                              float mainRadius) {


        while (firstPeak < actualPeaks.size()) {

            float peakTimeMs = firstPeak * peakDuration * 1000f;

            if (peakTimeMs < currentTime) {
                if (validPeak[firstPeak]) {
                    if (pressed) {
                        combo++;
                        points += POINT_PER_PEAK + combo;
                        pressed = false;

                    } else {
                        combo = 0;
                        points -= (int) (POINT_PER_PEAK * 0.2f);
                    }
                    error = false;
                }
                firstPeak++;
            } else {
                break;
            }
        }

        if (!error && pressed) {
            points -= (int) (POINT_PER_PEAK * 0.3f);
            pressed = false;
            error = true;
        }


        int lastPeak = firstPeak;

        while (lastPeak < actualPeaks.size()) {

            float peakTimeMs = lastPeak * peakDuration * 1000f;

            if (peakTimeMs - currentTime > TRAVEL_TIME)
                break;

            lastPeak++;
        }

        for (int i = lastPeak - 1; i >= firstPeak; i--) {

            if (!validPeak[i])
                continue;

            float peak = actualPeaks.get(i);

            float peakTimeMs = i * peakDuration * 1000f;
            float remaining = peakTimeMs - currentTime;

            float ratio = remaining / TRAVEL_TIME;

            float radius = mainRadius + mainRadius * ratio;

            drawSemiOval(
                    g,
                    centerX,
                    centerY,
                    radius,
                    0.6f,
                    0.2f,
                    0.2f
            );
        }
    }
    private void computeValidPeaks() {
        int n = actualPeaks.size();
        validPeak = new boolean[n];

        int minDistanceSamples = Math.max(1, (int) (MIN_PEAK_DISTANCE_MS / (peakDuration * 1000f)));

        int lastValidIndex = -minDistanceSamples;
        int count = 0;

        for (int i = 1; i < n - 1; i++) {

            float v = actualPeaks.get(i);

            if (v < actualPeaks.get(i - 1) || v < actualPeaks.get(i + 1))
                continue;

            float minLeft = Float.MAX_VALUE;
            for (int j = Math.max(0, i - PROMINENCE_WINDOW); j < i; j++)
                minLeft = Math.min(minLeft, actualPeaks.get(j));

            float minRight = Float.MAX_VALUE;
            for (int j = i + 1; j < Math.min(n, i + PROMINENCE_WINDOW); j++)
                minRight = Math.min(minRight, actualPeaks.get(j));

            float prominence = v - Math.max(minLeft, minRight);

            if (prominence < MIN_PROMINENCE)
                continue;

            if (i - lastValidIndex < minDistanceSamples)
                continue;

            validPeak[i] = true;
            lastValidIndex = i;
            count++;
        }

    }

    private void drawSemiOval(NvGraphic g,
                              float x,
                              float y,
                              float radius,
                              float r,
                              float green,
                              float b) {

        float left = x - radius;
        float top = y - radius;

        g.drawOval(left, top, radius * 2, 64, r, green, b);
        g.drawOval(left + 1, top + 13, (radius - 1) * 2, 64, 0, 0, 0);
    }

    @Override
    public void reload(short[] samples) {

        super.reload(samples);

        actualPeaks = new ArrayList<>(peaks.length);

        for (float peak : peaks) {
            actualPeaks.add(peak);
        }

        firstPeak = 0;
        combo = 0;
        points = 0;
        computeValidPeaks();
    }

    @Override
    public void onKeyPressed(boolean[] keys, int mods) {
        this.keys = keys;
    }

    @Override
    public void onKeyReleased(boolean[] keys, int mods) {
        this.keys = keys;
    }
}
