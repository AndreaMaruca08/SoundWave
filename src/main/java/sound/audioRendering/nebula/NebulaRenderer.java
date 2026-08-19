package sound.audioRendering.nebula;

import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;

import java.util.Random;

public class NebulaRenderer extends AudioRenderer {

    private static final int PARTICLES=5000;

    private float noiseTime=0;

    private final Random random=new Random();

    private Particle[] particles;
    private boolean initialized=false;

    private int lastPeak=-1;

    private float energy=0;

    @Override
    public void renderInternal(NvGraphic g, short[] samples, float width, float height, float currentTime){
        if(!initialized){
            createParticles(width,height);
            initialized=true;
        }

        updateAudio(currentTime);
        noiseTime+=0.002f+energy*0.01f;
        updateParticles(width,height);

        drawParticles(g);
    }

    private void updateAudio(float currentTime){

        int index=(int)((currentTime/1000f)/peakDuration);

        if(index>=0&&index<peaks.length&&index!=lastPeak){
            lastPeak=index;
            energy=peaks[index];
        }

        energy*=0.94f;
    }

    private void createParticles(float width,float height){

        particles=new Particle[PARTICLES];

        float cx=width/2f;
        float cy=height/2f;

        float radius=Math.min(width,height)*0.45f;

        for(int i=0;i<PARTICLES;i++){

            Particle p=new Particle();

            float angle=random.nextFloat()*(float)Math.PI*2;
            float distance=(float)Math.sqrt(random.nextFloat())*radius;

            p.x=cx+(float)Math.cos(angle)*distance;
            p.y=cy+(float)Math.sin(angle)*distance;

            p.vx=(random.nextFloat()-0.5f)*0.3f;
            p.vy=(random.nextFloat()-0.5f)*0.3f;

            p.size=0.8f+random.nextFloat()*2f;

            p.phase=random.nextFloat()*10;

            particles[i]=p;
        }
    }

    private void updateParticles(float width,float height){

        float cx=width/2f;
        float cy=height/2f;

        for(Particle p:particles){

            float dx=cx-p.x;
            float dy=cy-p.y;

            float distance=(float)Math.sqrt(dx*dx+dy*dy);

            if(distance<1)distance=1;

            float gravity=0.015f;

            p.vx+=dx/distance*gravity;
            p.vy+=dy/distance*gravity;


            if(energy>0){

                float explosion=energy*0.08f;

                p.vx-=dx/distance*explosion;
                p.vy-=dy/distance*explosion;
            }


            float noise=SimplexNoise.noise(
                    p.x*0.003f+noiseTime,
                    p.y*0.003f+noiseTime
            );

            float angle=noise*6.28f;

            p.vx+= (float) (Math.cos(angle)*0.003f);
            p.vy+= (float) (Math.sin(angle)*0.003f);


            p.vx*=0.985f;
            p.vy*=0.985f;


            p.x+=p.vx;
            p.y+=p.vy;


            if(p.x<0||p.x>width||p.y<0||p.y>height){
                resetParticle(p,width,height);
            }
        }
    }

    private void resetParticle(Particle p,float width,float height){

        p.x=width/2f+(random.nextFloat()-0.5f)*width*0.3f;
        p.y=height/2f+(random.nextFloat()-0.5f)*height*0.3f;

        p.vx=(random.nextFloat()-0.5f)*0.2f;
        p.vy=(random.nextFloat()-0.5f)*0.2f;
    }

    private void drawParticles(NvGraphic g){

        g.beginBatch();
        for(Particle p:particles){

            float glow=p.size*3+energy*8;

            g.batchDrawOval(
                    p.x,
                    p.y,
                    glow,
                    8,
                    0.15f,
                    0.25f,
                    0.5f
            );

            g.batchDrawOval(
                    p.x,
                    p.y,
                    p.size+energy*2,
                    8,
                    0.5f,
                    0.8f,
                    1f
            );
        }
        g.endBatch();
    }
}