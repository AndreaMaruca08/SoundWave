import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import nv.core.graphic.NvGraphic;
import sound.MovingCamera;
import sound.WaveDisplay;

void main() {
    var context = new ContextBuilder("Sound wave")
            .setVsync(true)
            .build();

    var page = context.addAndSetPage("Main", NvCont.newPage());
    page.setBackground(0.005f,0.005f,0.005f);

    int margin = 150;

    String music = "4rdsanctuary.wav";

    var w = (int) context.getRenderWidth() - margin*2;
    var h = (int) context.getRenderHeight();

    MovingCamera camera = new MovingCamera(w/2,h/2);
    NvGraphic.setCurrentCamera(camera.getCamera());
    context.setCurrentCameraUpdateCycle(camera);
    context.setKeyboardFocus(camera);
    camera.setNeedCamera(true);

    WaveDisplay display = new WaveDisplay(
            margin, 0,
            w, h,
            music
    );

    page.addChild(display);
    page.addChild(camera);

    context.run();
}