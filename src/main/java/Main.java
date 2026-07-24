import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import nv.core.graphic.NvGraphic;
import sound.MovingCamera;
import sound.audioRendering.WavePlayer;

import java.awt.*;
import java.util.List;

import static nv.core.errors.NvLogger.logErr;

void main() {
    var context = new ContextBuilder("Sound wave")
            .setVsync(true)
            .build();

    var page = context.addAndSetPage("Main", NvCont.newPage());
    page.setBackground(0,0,0);
    context.changeFont(new Font("monospaced", Font.PLAIN, 30));

    int margin = 150;

    var w = (int) context.getRenderWidth();
    var h = (int) context.getRenderHeight();

    MovingCamera camera = new MovingCamera((int) (w/2.15f), (int) (h/2.30f));
    camera.setNeedCamera(true);
    context.setCurrentCameraUpdateCycle(camera);

    NvGraphic.setCurrentCamera(camera.getCamera());
    context.setKeyboardFocus(camera);

    WavePlayer display = new WavePlayer(
            margin, 0,
            w - margin*3, h,
            getAudioPaths().toArray(new String[0])
    );

    page.addChild(display);
    page.addChild(camera);

    context.run();
}

private List<String> getAudioPaths() {
    Path folder = Paths.get("audio_files").toAbsolutePath();

    try {
        Files.createDirectories(folder);
    } catch (IOException e) {
        logErr("Impossibile creare la cartella: " + folder);
        return List.of();
    }
    List<String> paths = new ArrayList<>(10);
    try (Stream<Path> stream = Files.walk(folder)) {
        paths = stream
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav");
                })
                .map(p -> p.toAbsolutePath().toString())
                .toList();

    } catch (IOException e) {
        logErr("Error while reading the directory: " + folder.toAbsolutePath());
    }
    return paths;
}