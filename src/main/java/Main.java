import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import nv.core.graphic.NvGraphic;
import sound.MovingCamera;
import sound.WavePlayer;

import static nv.core.errors.NvLogger.logErr;
import static sound.loading.DecodeManager.getExecutableDirectory;

void main() {
    var context = new ContextBuilder("Sound wave")
            .setVsync(true)
            .build();

    var page = context.addAndSetPage("Main", NvCont.newPage());
    page.setBackground(0.005f,0.005f,0.005f);

    int margin = 150;

    Path folder = Paths.get("audio_files").toAbsolutePath();

    try {
        Files.createDirectories(folder);
    } catch (IOException e) {
        logErr("Impossibile creare la cartella: " + folder);
        return;
    }
    List<String> paths = new ArrayList<>(10);
    try (Stream<Path> stream = Files.walk(folder)) {
        paths = stream
                .filter(Files::isRegularFile)
                .map(p -> p.toAbsolutePath().toString())
                .toList();

    } catch (IOException e) {
        logErr("Error while reading the directory: " + folder.toAbsolutePath());
    }

    var w = (int) context.getRenderWidth() - margin*2;
    var h = (int) context.getRenderHeight();

    MovingCamera camera = new MovingCamera(w/2,h/2);
    NvGraphic.setCurrentCamera(camera.getCamera());
    context.setCurrentCameraUpdateCycle(camera);
    context.setKeyboardFocus(camera);
    camera.setNeedCamera(true);

    WavePlayer display = new WavePlayer(
            margin, 0,
            w, h,
            paths.toArray(new String[0])
    );

    page.addChild(display);
    page.addChild(camera);

    context.run();
}