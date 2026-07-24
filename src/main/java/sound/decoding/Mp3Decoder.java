package sound.decoding;

import javazoom.jl.decoder.*;

import java.io.FileInputStream;
import java.util.ArrayList;

import static nv.core.errors.NvLogger.logErr;

public class Mp3Decoder implements AudioDecoder {

    private static final int TARGET_RATE = 44100;
    private static final String EXTENSION = ".mp3";

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
            ArrayList<Short> rawSamples = new ArrayList<>();
            Header header;
            int sampleRate = TARGET_RATE;

            while ((header = bitstream.readFrame()) != null) {

                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                sampleRate = output.getSampleFrequency();

                short[] buffer = output.getBuffer();

                if (output.getChannelCount() == 2) {

                    for (int i = 0; i < output.getBufferLength(); i += 2) {
                        rawSamples.add((short)((buffer[i] + buffer[i + 1]) / 2)
                        );
                    }

                } else {
                    for (int i = 0; i < output.getBufferLength(); i++) {
                        rawSamples.add(buffer[i]);
                    }
                }

                bitstream.closeFrame();
            }

            bitstream.close();

            short[] samples = new short[rawSamples.size()];

            for (int i = 0; i < samples.length; i++) {
                samples[i] = rawSamples.get(i);
            }

            if (sampleRate != TARGET_RATE) {
                samples = resample(samples, sampleRate);
            }

            return samples;


        } catch (Exception e) {
            logErr("Error while decoding mp3: " + e.getMessage());
            return new short[0];
        }
    }

    private short[] resample(short[] input, int fromRate) {

        int outputLength = (int)((long) input.length * Mp3Decoder.TARGET_RATE / fromRate);

        short[] output = new short[outputLength];


        for (int i = 0; i < outputLength; i++) {
            int src = (int)((long)i * fromRate / Mp3Decoder.TARGET_RATE);
            output[i] = input[src];
        }

        return output;
    }
}