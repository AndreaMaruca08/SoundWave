package sound.decoding;

import java.util.HashMap;
import java.util.Map;

public class DecodeManager {
    private static final WavDecoder wav = new WavDecoder();
    private static final Mp3Decoder mp3 = new Mp3Decoder();

    private static final Map<String, AudioDecoder> DECODERS = Map.of(
            wav.getExtension(), wav,
            mp3.getExtension(), mp3
    );

    private static final Map<String, short[]> decodedCache = new HashMap<>(10);

    public static AudioDecoder get(String filePath){
        return DECODERS.get(filePath.substring(filePath.lastIndexOf(".")));
    }
    public static short[] decode(String filePath){
        if(decodedCache.containsKey(filePath))
            return decodedCache.get(filePath);

        var decoded = get(filePath).decode(filePath);
        decodedCache.put(filePath, decoded);

        return decoded;
    }
}
