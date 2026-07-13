import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import sound.Microphone;
import sound.MicrophoneDisplay;

void main() {
    var context = new ContextBuilder("Sound wave")
            .setVsync(true)
            .build();

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setBackground(0.007f,0.007f,0.007f);

    Microphone mic = new Microphone();
    mic.start();

    MicrophoneDisplay display = new MicrophoneDisplay(
            200, (int) (context.getRenderHeight()/2),
            (int) context.getRenderWidth() - 400, 1000,
            mic
    );
    page.addChild(display);

    context.run();
    mic.stopListening();
}