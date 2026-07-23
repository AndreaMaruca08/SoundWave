package sound.decoding;

import java.util.Map;

public class DecodeManager {
    private static final Map<String, AudioDecoder> DECODERS = Map.of(
            ".wav", new WavDecoder(),
            ".mp3", new Mp3Decoder()
    );

    public static AudioDecoder get(String filePath){
        return DECODERS.get(filePath.substring(filePath.lastIndexOf(".")));
    }
    public static short[] decode(String filePath){
        return get(filePath).decode(filePath);
    }
}
