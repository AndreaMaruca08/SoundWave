package sound.loading;

import java.util.Map;

public class DecodeManager {
    private static final Map<String, AudioDecoder> DECODERS = Map.of(
            ".wav", new WavDecoder()
    );

    public static AudioDecoder get(String filePath){
        return DECODERS.get(filePath.substring(filePath.lastIndexOf(".")));
    }
    public static short[] decode(String filePath){
        return get(filePath).decode(filePath);
    }
}
