import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import nv.core.io.AppPathUtils;
import nv.utils.camera.NvControlledCamera;
import sound.audioRendering.AudioCategory;
import sound.audioRendering.WavePlayer;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static nv.core.errors.NvLogger.logErr;

void main() {
    var context = new ContextBuilder("Sound wave")
            .setVsync(true)
            .setIdleWhenUnfocused(true)
            .build();

    var page = context.addAndSetPage("Main", NvCont.newPage());
    page.setBackgroundColor(0,0,0);
    context.changeFont(new Font("monospaced", Font.PLAIN, (int) (context.getRenderWidth()*0.01f)));

    int margin = 150;

    var w = (int) context.getRenderWidth();
    var h = (int) context.getRenderHeight();

    var camera = new NvControlledCamera((int) (w/2.15f), (int) (h/2.30f), 1000);
    camera.minZoom = 1f;
    WavePlayer display = new WavePlayer(
            margin, 0,
            w - margin*3, h,
            getAudioCategories(),
            camera
    );
    page.addChild(display);


    context.run();
}

private List<AudioCategory> getAudioCategories() {
    Path baseFolder = AppPathUtils.resolvePath("audio_files");

    try {
        Files.createDirectories(baseFolder);
    } catch (IOException e) {
        logErr("Impossibile creare la cartella: " + baseFolder);
        return List.of();
    }

    Map<String, List<String>> folderMap = new TreeMap<>((a, b) -> {
        if (a.equals(b)) return 0;
        if (a.equals("audio_files")) return -1;
        if (b.equals("audio_files")) return 1;
        return a.compareToIgnoreCase(b);
    });

    try (Stream<Path> stream = Files.walk(baseFolder)) {
        stream.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav");
                })
                .forEach(p -> {
                    Path parent = p.getParent();
                    String catName;
                    if (parent == null || parent.equals(baseFolder)) {
                        catName = "audio_files";
                    } else {
                        catName = baseFolder.relativize(parent).toString().replace('\\', '/');
                    }
                    folderMap.computeIfAbsent(catName, k -> new ArrayList<>())
                            .add(p.toAbsolutePath().toString());
                });
    } catch (IOException e) {
        logErr("Error while reading directory tree: " + baseFolder.toAbsolutePath());
    }

    List<AudioCategory> categories = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : folderMap.entrySet()) {
        entry.getValue().sort(Comparator.comparing(Path::of));
        categories.add(new AudioCategory(entry.getKey(), entry.getValue()));
    }
    return categories;
}