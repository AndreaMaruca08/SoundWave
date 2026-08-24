package sound.decoding;

import javazoom.jl.decoder.*;

import java.io.FileInputStream;
import java.util.Arrays;

import static nv.core.errors.NvLogger.logErr;

public class Mp3Decoder implements AudioDecoder {

    private static final int TARGET_RATE = 44100;
    private static final String EXTENSION = ".mp3";
    private static final int INITIAL_CAPACITY = 1 << 20;

    @Override
    public String getExtension() {
        return EXTENSION;
    }

    @Override
    public short[] decode(String fileName) {

        if (!fileName.toLowerCase().endsWith(EXTENSION)) {
            throw new IllegalArgumentException("File is not a mp3 file");
        }

        try {
            Bitstream bitstream = new Bitstream(new FileInputStream(fileName));
            Decoder decoder = new Decoder();
            Header header;
            int sampleRate = TARGET_RATE;

            short[] rawSamples = new short[INITIAL_CAPACITY];
            int count = 0;

            while ((header = bitstream.readFrame()) != null) {

                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                sampleRate = output.getSampleFrequency();

                short[] buffer = output.getBuffer();
                int bufferLength = output.getBufferLength();

                if (output.getChannelCount() == 2) {

                    int needed = count + (bufferLength / 2);
                    if (needed > rawSamples.length) {
                        rawSamples = growTo(rawSamples, needed);
                    }

                    for (int i = 0; i < bufferLength; i += 2) {
                        rawSamples[count++] = (short) ((buffer[i] + buffer[i + 1]) / 2);
                    }

                } else {

                    int needed = count + bufferLength;
                    if (needed > rawSamples.length) {
                        rawSamples = growTo(rawSamples, needed);
                    }

                    System.arraycopy(buffer, 0, rawSamples, count, bufferLength);
                    count += bufferLength;
                }

                bitstream.closeFrame();
            }

            bitstream.close();

            short[] samples = Arrays.copyOf(rawSamples, count);

            if (sampleRate != TARGET_RATE) {
                samples = resample(samples, sampleRate);
            }

            return samples;

        } catch (Exception e) {
            logErr("Error while decoding mp3: " + e.getMessage());
            return new short[0];
        }
    }

    private short[] growTo(short[] array, int minCapacity) {
        int newCapacity = array.length * 2;
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        return Arrays.copyOf(array, newCapacity);
    }

    private short[] resample(short[] input, int fromRate) {

        int outputLength = (int) ((long) input.length * Mp3Decoder.TARGET_RATE / fromRate);

        short[] output = new short[outputLength];

        for (int i = 0; i < outputLength; i++) {
            int src = (int) ((long) i * fromRate / Mp3Decoder.TARGET_RATE);
            output[i] = input[src];
        }

        return output;
    }
}