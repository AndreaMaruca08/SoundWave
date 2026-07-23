package sound.decoding;


import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Mp3Decoder implements AudioDecoder {
    @Override
    public short[] decode(String fileName) {
        if (!fileName.toLowerCase().endsWith(".mp3")) {
            throw new IllegalArgumentException("File is not a mp3 file");
        }

        try {
            Bitstream bitstream = new Bitstream(new FileInputStream(fileName));
            Decoder decoder = new Decoder();

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            Header header;

            while ((header = bitstream.readFrame()) != null) {

                SampleBuffer buffer =
                        (SampleBuffer) decoder.decodeFrame(header, bitstream);

                short[] samples = buffer.getBuffer();
                int length = buffer.getBufferLength();

                ByteBuffer bb = ByteBuffer.allocate(length * 2)
                        .order(ByteOrder.LITTLE_ENDIAN);

                for (int i = 0; i < length; i++) {
                    bb.putShort(samples[i]);
                }

                output.write(bb.array());

                bitstream.closeFrame();
            }

            bitstream.close();

            byte[] bytes = output.toByteArray();

            short[] samples = new short[bytes.length / 2];

            ByteBuffer.wrap(bytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(samples);

            return samples;
        }catch (Exception e){
            return new short[0];
        }
    }
}
