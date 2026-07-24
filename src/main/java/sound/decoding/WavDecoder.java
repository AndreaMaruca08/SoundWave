package sound.decoding;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class WavDecoder implements AudioDecoder {

    private static final String EXTENSION = ".wav";

    @Override
    public String getExtension() {
        return EXTENSION;
    }

    @Override
    public short[] decode(String fileName) {

        if (!fileName.toLowerCase().endsWith(EXTENSION)) {
            throw new IllegalArgumentException("File is not a wav file");
        }

        try {

            AudioInputStream originalStream =
                    AudioSystem.getAudioInputStream(
                            new File(fileName)
                    );


            AudioFormat originalFormat = originalStream.getFormat();

            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100,
                    16,
                    1,
                    2,
                    44100,
                    false
            );

            AudioInputStream pcmStream =
                    AudioSystem.getAudioInputStream(
                            targetFormat,
                            originalStream
                    );


            byte[] bytes = pcmStream.readAllBytes();


            short[] samples = new short[bytes.length / 2];


            for (int i = 0; i < samples.length; i++) {

                int low = bytes[i * 2] & 0xFF;
                int high = bytes[i * 2 + 1];

                samples[i] = (short) ((high << 8) | low);
            }


            pcmStream.close();
            originalStream.close();


            return samples;


        } catch (UnsupportedAudioFileException |
                 IOException |
                 IllegalArgumentException e) {

            throw new RuntimeException(
                    "Cannot decode wav file: " + fileName,
                    e
            );
        }
    }
}