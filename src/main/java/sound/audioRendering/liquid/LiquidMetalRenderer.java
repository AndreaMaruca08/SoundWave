package sound.audioRendering.liquid;

import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;

public class LiquidMetalRenderer extends AudioRenderer {

    private static final int POINTS=480;

    private float time=0;
    private float energy=0;

    private int lastPeak=-1;

    private final float[] px=new float[POINTS];
    private final float[] py=new float[POINTS];

    private float baseRadius=400;

    @Override
    public void renderInternal(NvGraphic g, short[] samples, float width, float height, float currentTime){
        updateAudio(currentTime);

        time+=0.03f+energy*0.1f;

        float cx=width/2f;
        float cy=height/2f;

        updateShape(cx,cy);

        drawShape(g);
    }


    private void updateAudio(float currentTime){
        int index=(int)((currentTime/1000f)/peakDuration);

        if(index>=0&&index<peaks.length&&index!=lastPeak){
            lastPeak=index;
            energy=peaks[index];
        }

        energy*=0.95f;
    }

    private void updateShape(float cx,float cy){

        for(int i=0;i<POINTS;i++){
            float idle=0.2f+energy;
            float angle=(float)(Math.PI*2*i/POINTS);
            float wave= (float)Math.sin(angle*4+time);
            float wave2= (float)Math.sin(angle*9-time*0.7f);
            float radius= baseRadius + wave*25*idle + wave2*10*idle + energy*80;

            px[i]=cx+(float)Math.cos(angle)*radius;
            py[i]=cy+(float)Math.sin(angle)*radius;
        }
    }

    private void drawShape(NvGraphic g){
        g.beginBatch();
        for(int i=0;i<POINTS;i++){

            int next=i+1;

            if(next>=POINTS)
                next=0;

            g.batchDrawLine(
                    px[i],
                    py[i],
                    px[next],
                    py[next],
                    2,
                    0.3f,
                    0.7f,
                    1f
            );
        }
        g.endBatch();
    }
}