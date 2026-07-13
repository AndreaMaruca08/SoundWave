package sound;

import nv.core.errors.NvLogger;
import nv.utils.shapes.dynamic.NvLabel;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Microphone extends Thread {

    private volatile boolean running = true;

    private final short[] waveBuffer = new short[1024];

    public Microphone() {}

    public void stopListening() {
        running = false;

    }

    public short[] getWaveBuffer() {

        return waveBuffer;

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

        TargetDataLine mic = null;

        try {

            mic = AudioSystem.getTargetDataLine(format);

            mic.open(format);

            mic.start();

        } catch (LineUnavailableException e) {

            NvLogger.logErr("Error opening mic: " + e);

            return;

        }

        byte[] buffer = new byte[4096];

        while (running) {

            int bytesRead = mic.read(buffer, 0, buffer.length);

            int samples = bytesRead / 2;

            synchronized (waveBuffer) {

                for (int i = 0; i < waveBuffer.length; i++) {

                    if (i < samples) {

                        int low = buffer[i * 2] & 0xFF;

                        int high = buffer[i * 2 + 1];

                        waveBuffer[i] =

                                (short)((high << 8) | low);

                    } else {

                        waveBuffer[i] = 0;

                    }

                }

            }

        }

        mic.close();

    }

}
