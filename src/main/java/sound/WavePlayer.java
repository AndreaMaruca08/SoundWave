package sound;

import nv.core.NvContext;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.AudioManager;
import nv.utils.shapes.dynamic.NvLabel;
import sound.loading.DecodeManager;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WavePlayer extends NvComp {

    private int DISPLAY_SAMPLES = 2048;
    private final Microphone mic;

    private final HudLabel title;
    private final HudLabel durationLabel;

    private final String[] filePaths;
    private int current = 0;

    private boolean error = false;

    private int maxDurationMs;
    private int currentVolume = 100;

    private float lineX1;
    private final float lineY2 = getH()*0.8f;

    private short[] waveBuffer = new short[DISPLAY_SAMPLES];

    public WavePlayer(
            int x,
            int y,
            int w,
            int h,
            Microphone microphone
    ) {
        super(x, y, w, h);
        this.mic = microphone;
        this.filePaths = new String[]{};
        this.maxDurationMs = 0;
        this.title = new HudLabel(30, 30);
        this.durationLabel = new HudLabel(100,0);
        durationLabel.changeText("");
        title.setHUD(true);
        title.setRgb(1,1,1);
        title.changeText("Microphone");
        addChild(title);
    }
    public WavePlayer(
            int x,
            int y,
            int w,
            int h,
            String[] filePaths
    ) {
        super(x, y, w, h);
        if(filePaths.length == 0){
            error = true;
            this.filePaths = new String[]{};
            this.maxDurationMs = 0;
            this.title = new HudLabel(30, 30);
            this.durationLabel = new HudLabel(100,0);
            durationLabel.changeText("");
            mic = null;
            return;
        }


        var samples = DecodeManager.decode(filePaths[0]);

        AudioManager.loadExternal(filePaths[0]);
        AudioManager.setVolumeExternal(filePaths[0], currentVolume);

        this.waveBuffer = samples;
        this.filePaths = filePaths;
        this.maxDurationMs = AudioManager.getDurationExternal(filePaths[0]);
        this.DISPLAY_SAMPLES = samples.length;
        this.title = new HudLabel(30, 30);
        this.durationLabel = new HudLabel((int) (w/1.95f), 30);
        durationLabel.changeText(getFormattedDuration());
        durationLabel.setRgb(1,1,1);
        title.setRgb(1,1,1);
        var p = filePaths[0];
        title.changeText(p.substring(p.lastIndexOf("/")+1, p.lastIndexOf(".")) + " | samples: " + DISPLAY_SAMPLES);
        addChild(title);
        addChild(durationLabel);
        this.mic = null;
        initBtn();
    }

    private String getFormattedDuration(){
        int secs = maxDurationMs / 1000;
        int currSecs = (int) (currentTime / 1000);

        int currMin = currSecs / 60;
        int min = secs / 60;

        return currMin + ":" + String.format("%02d", currSecs % 60) + "/"
                + min + ":" + String.format("%02d", secs % 60);
    }

    public void next(){
        AudioManager.stopExternal(filePaths[current]);
        current++;
        if(current > filePaths.length - 1){
            current = 0;
        }
        reset();
    }

    public void previous(){
        AudioManager.stopExternal(filePaths[current]);
        current--;
        if(current < 0){
            current = filePaths.length - 1;
        }
        reset();
    }

    private void reset(){
        var p = filePaths[current];
        var newSamples = DecodeManager.decode(p);
        DISPLAY_SAMPLES = newSamples.length;
        title.changeText(p.substring(p.lastIndexOf("/")+1, p.lastIndexOf(".")) + " | samples: " + DISPLAY_SAMPLES);
        AudioManager.loadExternal(filePaths[current]);
        AudioManager.setVolumeExternal(filePaths[current], currentVolume);
        this.maxDurationMs = AudioManager.getDurationExternal(filePaths[current]);
        waveBuffer = newSamples;
    }

    private int cX = 0;

    private int nextPosition(){
        return cX += 100;
    }

    private float currentTime = 0;

    private void initBtn(){
        var size = 80;

        AtomicBoolean paused = new AtomicBoolean(true);
        var pause = new PlayButton(nextPosition(), (int) (getH()*0.9f),size,size, Color.DARK_GRAY," ||");
        pause.setAction(() -> {
            paused.set(!paused.get());

            if(paused.get()){
                AudioManager.stopExternal(filePaths[current]);
                pause.setDisplay("||");
                pause.markDirty();
            }else{
                AudioManager.playExternal(filePaths[current]);
                pause.setDisplay("GO");
                pause.markDirty();
            }
        });

        var y = (int) (getH()*0.9f);
        var skipBack = new PlayButton(0, y,size,size, Color.DARK_GRAY,"<<5");
        skipBack.setAction(() -> {
            currentTime -= 5000;
            AudioManager.skipExternal(filePaths[current], (int) currentTime);
        });
        var skipForward = new PlayButton(nextPosition(), y,size,size, Color.DARK_GRAY,"5>>");
        skipForward.setAction(() -> {
            currentTime += 5000;
            AudioManager.skipExternal(filePaths[current], (int) currentTime);
        });
        nextPosition();

        NvLabel volume = new NvLabel(nextPosition(), y);
        volume.changeText("Vol: " + currentVolume);
        volume.setRgb(1,1,1);

        nextPosition();

        var minusVolume = new PlayButton(nextPosition(), y,size,size, Color.DARK_GRAY," -");
        minusVolume.setAction(() -> {
            currentVolume -= 5;
            if(currentVolume < 0)
                currentVolume = 0;
            AudioManager.setVolumeExternal(filePaths[current], currentVolume);
            volume.changeText("Volume: " + currentVolume);
            markDirty();
        });
        var plusVolume = new PlayButton(nextPosition(), y,size,size, Color.DARK_GRAY," +");
        plusVolume.setAction(() -> {
            currentVolume += 5;
            if(currentVolume > 100)
                currentVolume = 100;
            AudioManager.setVolumeExternal(filePaths[current], currentVolume);
            volume.changeText("Volume: " + currentVolume);
            markDirty();
        });
        nextPosition();
        var nextSound = new PlayButton(nextPosition(), y,size*2,size, Color.CYAN.darker().darker(), " Next");
        nextSound.setAction(this::next);
        nextPosition();
        var previousSound = new PlayButton(nextPosition(), y,size*2,size, Color.CYAN.darker().darker(), "Previous");
        previousSound.setAction(this::previous);

        addChild(nextSound);
        addChild(previousSound);
        addChild(pause);
        addChild(skipBack);
        addChild(skipForward);
        addChild(plusVolume);
        addChild(minusVolume);
        addChild(volume);
    }

    private float limiter = 0;

    private void updateLine(){
        var percentage = AudioManager.getCurrentPercentageExternal(filePaths[current]);
        lineX1 = getW() * (percentage/100);
        currentTime = percentage * maxDurationMs / 100;
        durationLabel.changeText(getFormattedDuration());
        NvContext.markSceneDirty();
    }

    @Override
    public void update(float dt) {
        if(error)
            return;

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


    @Override
    public void drawIntern(NvGraphic g) {
        if(error){
            g.drawRect(0,0,1000,1000,0,0,0);
            g.setRGB(1,1,1);
            g.drawText("Error: empty directory /audio_files", 150,500);
            return;
        }

        WaveRenderer.drawWaveform(g, waveBuffer, getW(), getH()*0.8f);
        g.drawLine(lineX1, 0f, lineX1, lineY2, 3);
    }
}