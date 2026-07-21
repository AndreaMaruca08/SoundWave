package sound.loading;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static nv.core.errors.NvLogger.logErr;

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

    public static Path getExecutableDirectory() {
        try {
            File jarFile = new File(
                    DecodeManager.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            return jarFile.isFile() ? jarFile.getParentFile().toPath() : jarFile.toPath();

        } catch (URISyntaxException e) {
            logErr("Impossibile determinare la cartella dell'eseguibile");
            return Paths.get(".");
        }
    }
}
