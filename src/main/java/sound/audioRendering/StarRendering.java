package sound.audioRendering;

import nv.core.graphic.NvGraphic;
import sound.WavePlayer;

import java.util.Arrays;
import java.util.Random;

public class StarRendering implements AudioRenderer {

    private static final int WINDOW = 1024;
    private static final int MINIMUM = 3000;

    private static final int STAR_COUNT = 1000;
    private static final int CONNECTIONS = 4;
    private static final float CONNECTION_DISTANCE = 100;

    private float[] peaks;
    private double average = 0.3f;
    private float peakDuration;

    private boolean going = false;

    private final Random random = new Random();

    private Star[] stars;

    private int lastPeakIndex = -1;


    public StarRendering(WavePlayer player) {
        createStars(player.getW(), player.getH()*0.8f);
        reload(player.waveBuffer);
    }


    private void createStars(float w, float h) {

        stars = new Star[STAR_COUNT];

        for (int i = 0; i < STAR_COUNT; i++) {

            Star star = new Star();

            star.x = random.nextFloat() * w;
            star.y = random.nextFloat() * h;

            star.vx = (random.nextFloat() - 0.5f) * 0.15f;
            star.vy = (random.nextFloat() - 0.5f) * 0.15f;

            star.radius = 1 + random.nextFloat() * 2;

            star.energy = random.nextFloat() * 0.2f;

            stars[i] = star;
        }
    }


    @Override
    public void render(
            NvGraphic g,
            short[] samples,
            float width,
            float height,
            float currentTime
    ) {

        if (!going)
            return;


        updateStars(width, height);


        int index = (int)((currentTime / 1000f) / peakDuration);

        float audioPower = 0;

        if(index >= 0 && index < peaks.length) {

            audioPower = peaks[index];

            if(index != lastPeakIndex) {

                lastPeakIndex = index;

                float impulse = (float) Math.max(
                        0,
                        audioPower - average
                );

                for(Star s : stars) {
                    s.energy += impulse * 0.5f;
                }
            }
        }


        drawConnections(g);

        drawStars(g);
    }


    private void updateStars(float width, float height) {

        for(Star s : stars) {

            s.energy *= 0.94f;

            s.x += s.vx;
            s.y += s.vy;

            if(s.x < 0 || s.x > width)
                s.vx *= -1;


            if(s.y < 0 || s.y > height)
                s.vy *= -1;
        }
    }


    private void drawStars(NvGraphic g) {

        for(Star s : stars) {
            float size = s.radius + s.energy * 8;
            float brightness = Math.min(1, 0.5f + s.energy);

            g.drawOval(
                    s.x,
                    s.y,
                    size,
                    16,
                    brightness,
                    brightness/1.3f,
                    brightness
            );
        }
    }
    private void drawConnections(NvGraphic g) {

        for (Star star : stars) {

            Star[] nearest = new Star[CONNECTIONS];
            float[] distances = new float[CONNECTIONS];

            Arrays.fill(distances, Float.MAX_VALUE);


            for (Star other : stars) {

                if (star == other)
                    continue;


                float dx = star.x - other.x;
                float dy = star.y - other.y;

                float distance = dx * dx + dy * dy;


                for (int i = 0; i < CONNECTIONS; i++) {

                    if (distance < distances[i]) {

                        for (int j = CONNECTIONS - 1; j > i; j--) {

                            distances[j] = distances[j - 1];
                            nearest[j] = nearest[j - 1];
                        }

                        distances[i] = distance;
                        nearest[i] = other;

                        break;
                    }
                }
            }


            for (int i = 0; i < CONNECTIONS; i++) {

                Star other = nearest[i];

                if(other == null)
                    continue;


                float realDistance = (float)Math.sqrt(distances[i]);


                if(realDistance > CONNECTION_DISTANCE)
                    continue;


                float intensity = 1f - (realDistance / CONNECTION_DISTANCE);


                intensity *= Math.min(1, star.energy + other.energy + 0.2f);

                g.drawLine(
                        star.x,
                        star.y,
                        other.x,
                        other.y,
                        1,
                        intensity,
                        intensity,
                        Math.min(1, intensity * 3)
                );
            }
        }
    }


    @Override
    public void start() {
        going = true;
    }


    @Override
    public void stop() {
        going = false;
    }


    @Override
    public void reload(short[] samples) {

        peaks = new float[
                (samples.length + WINDOW - 1) / WINDOW
                ];


        float sum = 0;
        long count = 0;


        peakDuration = WINDOW / 44100f;


        int index = 0;


        for(int i = 0; i < samples.length; i += WINDOW) {

            int max = 0;


            for(int j = i; j < i + WINDOW && j < samples.length; j++) {
                int value = Math.abs(samples[j]);


                if(value > MINIMUM) {
                    sum += value;
                    count++;
                }


                if(value > max)
                    max = value;
            }


            peaks[index++] =
                    max / 32767f;
        }


        if(count > 0)
            average =
                    (sum / count) / 32767f;
        else
            average = 0;
    }
}