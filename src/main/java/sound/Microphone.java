package sound;

import nv.core.errors.NvLogger;

import javax.sound.sampled.*;

public class Microphone extends Thread {

    public static final int BUFFER_SIZE = 16384;

    private final short[] ringBuffer = new short[BUFFER_SIZE];

    private int writePos = 0;

    private volatile boolean running = true;

    private TargetDataLine mic;

    public boolean isRunning() {
        return running;
    }

    public void stopListening() {

        running = false;

        if (mic != null) {
            mic.stop();
            mic.close();
        }
    }

    @Override
    public void run() {

        AudioFormat format = new AudioFormat(
                44100,
                16,
                1,
                true,
                false
        );

        try {

            mic = AudioSystem.getTargetDataLine(format);
            mic.open(format);
            mic.start();

        } catch (LineUnavailableException e) {

            NvLogger.logErr(e.toString());
            return;
        }

        byte[] buffer = new byte[4096];

        while (running) {

            int bytesRead = mic.read(buffer, 0, buffer.length);

            int samples = bytesRead / 2;

            synchronized (ringBuffer) {

                for (int i = 0; i < samples; i++) {

                    int low = buffer[i * 2] & 0xFF;
                    int high = buffer[i * 2 + 1];

                    ringBuffer[writePos] = (short) ((high << 8) | low);

                    writePos++;

                    if (writePos >= BUFFER_SIZE)
                        writePos = 0;
                }
            }
        }
    }

    /**
     * Copia gli ultimi destination.length campioni.
     */
    public void copyLatest(short[] destination) {

        synchronized (ringBuffer) {

            int count = destination.length;

            if (count > BUFFER_SIZE)
                throw new IllegalArgumentException("Destination too large");

            int start = writePos - count;

            if (start < 0)
                start += BUFFER_SIZE;

            int firstPart = Math.min(count, BUFFER_SIZE - start);

            System.arraycopy(
                    ringBuffer,
                    start,
                    destination,
                    0,
                    firstPart
            );

            if (firstPart < count) {

                System.arraycopy(
                        ringBuffer,
                        0,
                        destination,
                        firstPart,
                        count - firstPart
                );
            }
        }
    }
}