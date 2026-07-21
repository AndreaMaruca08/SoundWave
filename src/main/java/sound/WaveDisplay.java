package sound;

import nv.core.NvContext;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.AudioManager;
import nv.utils.shapes.dynamic.NvLabel;
import sound.loading.DecodeManager;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WaveDisplay extends NvComp {

    private int DISPLAY_SAMPLES = 2048;
    private final Microphone mic;
    private final HudLabel title;
    private final String filePath;
    private final int maxDurationMs;
    private int currentVolume = 100;

    private float lineX1;
    private final float lineY2 = getH()*0.8f;

    private short[] waveBuffer = new short[DISPLAY_SAMPLES];

    public WaveDisplay(
            int x,
            int y,
            int w,
            int h,
            Microphone microphone
    ) {
        super(x, y, w, h);
        this.mic = microphone;
        this.filePath = "Microphone";
        this.maxDurationMs = 0;
        this.title = new HudLabel(30, 30);
        title.setHUD(true);
        title.setRgb(1,1,1);
        title.changeText("Microphone");
        addChild(title);
    }
    public WaveDisplay(
            int x,
            int y,
            int w,
            int h,
            String filePath
    ) {
        super(x, y, w, h);
        var samples = DecodeManager.decode(filePath);

        AudioManager.loadExternal(filePath);
        AudioManager.setVolumeExternal(filePath, currentVolume);

        this.waveBuffer = samples;
        this.filePath = filePath;
        this.maxDurationMs = AudioManager.getDurationExternal(filePath);
        this.DISPLAY_SAMPLES = samples.length;
        this.title = new HudLabel(30, 30);
        title.setRgb(1,1,1);
        title.changeText(filePath.substring(0, filePath.lastIndexOf(".")) + " | samples: " + DISPLAY_SAMPLES);
        addChild(title);
        this.mic = null;
        initBtn();
    }

    private int cX = 0;

    private int nextPosition(){
        return cX += 150;
    }

    private float currentTime = 0;

    private void initBtn(){
        AtomicBoolean paused = new AtomicBoolean(true);
        var pause = new PlayButton(nextPosition(), (int) (getH()*0.9f),100,100, Color.DARK_GRAY," ||");
        pause.setAction(() -> {
            paused.set(!paused.get());

            if(paused.get()){
                AudioManager.stopExternal(filePath);
                pause.setDisplay("||");
                pause.markDirty();
            }else{
                AudioManager.playExternal(filePath);
                pause.setDisplay("GO");
                pause.markDirty();
            }
        });
        var y = (int) (getH()*0.9f);
        var skipBack = new PlayButton(0, y,100,100, Color.DARK_GRAY,"<<5");
        skipBack.setAction(() -> {
            currentTime -= 5000;
            AudioManager.skipExternal(filePath, (int) currentTime);
        });
        var skipForward = new PlayButton(nextPosition(), y,100,100, Color.DARK_GRAY,"5>>");
        skipForward.setAction(() -> {
            currentTime += 5000;
            AudioManager.skipExternal(filePath, (int) currentTime);
        });
        nextPosition();

        NvLabel volume = new NvLabel(nextPosition(), y);
        volume.changeText("Vol: " + currentVolume);
        volume.setRgb(1,1,1);

        nextPosition();

        var minusVolume = new PlayButton(nextPosition(), y,100,100, Color.DARK_GRAY," -");
        minusVolume.setAction(() -> {
            currentVolume -= 5;
            if(currentVolume < 0)
                currentVolume = 0;
            AudioManager.setVolumeExternal(filePath, currentVolume);
            volume.changeText("Volume: " + currentVolume);
            markDirty();
        });
        var plusVolume = new PlayButton(nextPosition(), y,100,100, Color.DARK_GRAY," +");
        plusVolume.setAction(() -> {
            currentVolume += 5;
            if(currentVolume > 100)
                currentVolume = 100;
            AudioManager.setVolumeExternal(filePath, currentVolume);
            volume.changeText("Volume: " + currentVolume);
            markDirty();
        });

        addChild(pause);
        addChild(skipBack);
        addChild(skipForward);
        addChild(plusVolume);
        addChild(minusVolume);
        addChild(volume);
    }

    private float limiter = 0;

    @Override
    public void update(float dt) {

        if(mic != null){
            mic.copyLatest(waveBuffer);
            markDirty();
        }

        if(limiter > 10){
            limiter = 0;
            updateLine();
        }else{
            limiter += dt*1000;
        }

        currentTime += dt;
    }

    private void updateLine(){
        var percentage = AudioManager.getCurrentPercentageExternal(filePath);
        lineX1 = getW() * (percentage/100);
        currentTime = percentage * maxDurationMs / 100;
        NvContext.markSceneDirty();
    }

    @Override
    public void drawIntern(NvGraphic g) {
        WaveRenderer.drawWaveform(g, waveBuffer, getW(), getH()*0.8f);
        g.drawLine(lineX1, 0f, lineX1, lineY2, 3);
    }
}