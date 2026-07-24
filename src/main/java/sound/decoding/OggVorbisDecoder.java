package sound.decoding;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class OggVorbisDecoder implements AudioDecoder {

    private static final String EXTENSION = ".ogg";

    @Override
    public String getExtension() {
        return EXTENSION;
    }

    @Override
    public short[] decode(String fileName) {

        if (!fileName.toLowerCase().endsWith(EXTENSION)) {
            throw new IllegalArgumentException("File is not an ogg file");
        }

        try {
            AudioInputStream originalStream =
                    AudioSystem.getAudioInputStream(new File(fileName));

            AudioFormat vorbisFormat = originalStream.getFormat();

            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    vorbisFormat.getSampleRate(),
                    16,
                    vorbisFormat.getChannels(),
                    vorbisFormat.getChannels() * 2,
                    vorbisFormat.getSampleRate(),
                    false
            );

            AudioInputStream pcmStream =
                    AudioSystem.getAudioInputStream(
                            pcmFormat,
                            originalStream
                    );


            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100,
                    16,
                    1,
                    2,
                    44100,
                    false
            );


            AudioInputStream targetStream =
                    AudioSystem.getAudioInputStream(
                            targetFormat,
                            pcmStream
                    );


            byte[] bytes = targetStream.readAllBytes();

            short[] samples = new short[bytes.length / 2];

            for (int i = 0; i < samples.length; i++) {
                int low = bytes[i * 2] & 0xFF;
                int high = bytes[i * 2 + 1];

                samples[i] = (short) ((high << 8) | low);
            }


            targetStream.close();
            pcmStream.close();
            originalStream.close();

            return samples;


        } catch (UnsupportedAudioFileException |
                 IOException |
                 IllegalArgumentException e) {

            throw new RuntimeException(
                    "Cannot decode ogg file: " + fileName,
                    e
            );
        }
    }
}