package sound.decoding;

import java.util.HashMap;
import java.util.Map;

public class DecodeManager {
    private static final WavDecoder wav = new WavDecoder();
    private static final Mp3Decoder mp3 = new Mp3Decoder();
    private static final OggVorbisDecoder ogg = new OggVorbisDecoder();

    private static final Map<String, AudioDecoder> DECODERS = Map.of(
            wav.getExtension(), wav,
            mp3.getExtension(), mp3,
            ogg.getExtension(), ogg
    );

    private static final Map<String, short[]> decodedCache = new HashMap<>(10);

    public static AudioDecoder getDecoder(String filePath){
        return DECODERS.get(filePath.substring(filePath.lastIndexOf(".")));
    }
    public static short[] decode(String filePath){
        if(decodedCache.containsKey(filePath))
            return decodedCache.get(filePath);

        var decoder = getDecoder(filePath);
        if(decoder == null){
            return new short[0];
        }

        var decoded = decoder.decode(filePath);
        decodedCache.put(filePath, decoded);

        return decoded;
    }
}
