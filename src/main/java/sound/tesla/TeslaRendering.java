package sound.tesla;

import nv.core.graphic.NvGraphic;
import sound.audioRendering.AudioRenderer;
import sound.audioRendering.WavePlayer;

import java.util.Random;

public class TeslaRendering extends AudioRenderer {

    private static final int NODE_COUNT = 26;
    private static final int MAX_BOLTS = 40;
    private static final int BOLT_SEGMENTS = 10;
    private static final int MAX_BRANCHES_PER_BOLT = 3;

    private final Random random = new Random();

    private Node[] nodes;
    private Bolt[] bolts;

    private float centerX, centerY;
    private float fieldRadius;

    private int lastPeakIndex = -1;
    private float coilCharge = 0f;
    private float coilRotation = 0f;
    private float flicker = 1f;
    private float boltAccumulator = 0f;

    private static class Node {
        float baseX, baseY;
        float x, y;
        float charge;
        float phase;
        boolean isDischarging;
        float dischargeTimer;
    }

    private static class Bolt {
        float x1, y1, x2, y2;
        float life;
        float thickness;
        float seed;
        float hueShift;
        boolean active;
    }

    public TeslaRendering(WavePlayer player) {
        centerX = player.getW() / 2f;
        centerY = (player.getH() * 0.8f) / 2f;
        fieldRadius = Math.min(player.getW(), player.getH() * 0.8f) * 0.42f;

        createNodes();
        createBolts();
        reload(player.waveBuffer);
    }

    private void createNodes() {
        nodes = new Node[NODE_COUNT];
        for (int i = 0; i < NODE_COUNT; i++) {
            Node n = new Node();
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float dist = fieldRadius * (0.35f + random.nextFloat() * 0.65f);
            n.baseX = centerX + (float) Math.cos(angle) * dist;
            n.baseY = centerY + (float) Math.sin(angle) * dist * 0.7f;
            n.x = n.baseX;
            n.y = n.baseY;
            n.charge = random.nextFloat() * 0.3f;
            n.phase = random.nextFloat() * (float) (Math.PI * 2);
            nodes[i] = n;
        }
    }

    private void createBolts() {
        bolts = new Bolt[MAX_BOLTS];
        for (int i = 0; i < MAX_BOLTS; i++) {
            bolts[i] = new Bolt();
            bolts[i].active = false;
        }
    }

    @Override
    public void renderInternal(
            NvGraphic g,
            short[] samples,
            float width,
            float height,
            float currentTime
    ) {
        int index = (int) ((currentTime / 1000f) / peakDuration);

        float audioPower = 0;
        float impulse = 0;

        if (index >= 0 && index < peaks.length) {
            audioPower = peaks[index];

            if (index != lastPeakIndex) {
                lastPeakIndex = index;
                impulse = (float) Math.max(0, audioPower - average);
                coilCharge += impulse * 2.5f;
            }
        }

        flicker = 0.75f + random.nextFloat() * 0.25f + audioPower * 0.5f;

        updateNodes(audioPower);
        spawnBolts(audioPower, impulse);
        updateBolts();

        drawField(g, audioPower);
        drawBolts(g);
        drawNodes(g, audioPower);
        drawCoilCore(g);

        coilCharge *= 0.88f;
        coilRotation += 0.01f + audioPower * 0.06f;
    }

    private void updateNodes(float audioPower) {
        for (Node n : nodes) {
            n.phase += 0.05f + audioPower * 0.4f;
            n.charge *= 0.9f;

            float jitter = (audioPower * 6f + coilCharge * 4f);
            n.x = n.baseX + (float) Math.sin(n.phase * 1.3f) * jitter;
            n.y = n.baseY + (float) Math.cos(n.phase * 1.7f) * jitter;

            if (random.nextFloat() < 0.01f + audioPower * 0.05f) {
                n.charge = 1f;
            }
        }
    }

    private void spawnBolts(float audioPower, float impulse) {
        boltAccumulator += 0.4f + audioPower * 2.5f + impulse * 6f;

        while (boltAccumulator >= 1f) {
            boltAccumulator -= 1f;
            fireRandomBolt(audioPower);
        }

        if (impulse > 0.1f) {
            int coilBolts = 3 + (int) (impulse * 15f);
            for (int i = 0; i < coilBolts; i++) {
                fireCoilBolt();
            }
        }
    }

    private void fireRandomBolt(float audioPower) {
        if (nodes.length < 2) return;

        Node from = nodes[random.nextInt(nodes.length)];
        Node to = nodes[random.nextInt(nodes.length)];
        if (from == to) return;

        Bolt b = findFreeBolt();
        if (b == null) return;

        b.active = true;
        b.x1 = from.x;
        b.y1 = from.y;
        b.x2 = to.x;
        b.y2 = to.y;
        b.life = 1f;
        b.thickness = 1f + audioPower * 2.5f;
        b.seed = random.nextFloat() * 1000f;
        b.hueShift = random.nextFloat();

        from.charge = 1f;
        to.charge = 1f;
    }

    private void fireCoilBolt() {
        Bolt b = findFreeBolt();
        if (b == null) return;

        float angle = random.nextFloat() * (float) (Math.PI * 2);
        float dist = fieldRadius * (0.7f + random.nextFloat() * 0.5f);

        b.active = true;
        b.x1 = centerX;
        b.y1 = centerY;
        b.x2 = centerX + (float) Math.cos(angle) * dist;
        b.y2 = centerY + (float) Math.sin(angle) * dist * 0.7f;
        b.life = 1f;
        b.thickness = 1.5f + coilCharge * 2f;
        b.seed = random.nextFloat() * 1000f;
        b.hueShift = random.nextFloat() * 0.3f;
    }

    private Bolt findFreeBolt() {
        for (Bolt b : bolts) {
            if (!b.active) return b;
        }
        return null;
    }

    private void updateBolts() {
        for (Bolt b : bolts) {
            if (!b.active) continue;
            b.life -= 0.15f + random.nextFloat() * 0.1f;
            if (b.life <= 0f) b.active = false;
        }
    }

    private void drawField(NvGraphic g, float audioPower) {
        int rings = 3;
        for (int r = 0; r < rings; r++) {
            float ringRadius = fieldRadius * (0.5f + r * 0.25f);
            int segments = 40;
            float wobble = audioPower * 10f + coilCharge * 8f;

            float prevX = 0, prevY = 0;
            for (int s = 0; s <= segments; s++) {
                float a = (float) (s * 2 * Math.PI / segments) + coilRotation * (r % 2 == 0 ? 1 : -1);
                float jitter = (float) Math.sin(a * 5f + coilRotation * 10f) * wobble * 0.3f;
                float rad = ringRadius + jitter;

                float x = centerX + (float) Math.cos(a) * rad;
                float y = centerY + (float) Math.sin(a) * rad * 0.7f;

                if (s > 0) {
                    float intensity = Math.min(1f, 0.15f + audioPower * 0.5f + coilCharge * 0.3f) * flicker;
                    g.drawLine(prevX, prevY, x, y, 1f,
                            intensity * 0.3f, intensity * 0.5f, intensity);
                }

                prevX = x;
                prevY = y;
            }
        }
    }

    private void drawBolts(NvGraphic g) {
        for (Bolt b : bolts) {
            if (!b.active) continue;
            drawJaggedBolt(g, b.x1, b.y1, b.x2, b.y2, b.life, b.thickness, b.seed, b.hueShift, MAX_BRANCHES_PER_BOLT);
        }
    }

    private void drawJaggedBolt(NvGraphic g, float x1, float y1, float x2, float y2,
                                 float life, float thickness, float seed, float hueShift, int branchesLeft) {

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001f) return;

        float perpX = -dy / length;
        float perpY = dx / length;

        float prevX = x1;
        float prevY = y1;

        for (int s = 1; s <= BOLT_SEGMENTS; s++) {
            float t = (float) s / BOLT_SEGMENTS;

            float baseX = x1 + dx * t;
            float baseY = y1 + dy * t;

            float wobbleAmount = length * 0.08f * (1f - t * 0.5f) * life;
            float wobble = (float) Math.sin(seed + s * 7.3f) * wobbleAmount;

            float px = baseX + perpX * wobble;
            float py = baseY + perpY * wobble;

            float rr = 0.55f + hueShift * 0.3f;
            float gg = 0.75f + hueShift * 0.15f;
            float bb = 1f;

            float alpha = life * flicker;

            g.drawLine(prevX, prevY, px, py, thickness * life, rr * alpha, gg * alpha, bb * alpha);

            if (branchesLeft > 0 && random.nextFloat() < 0.12f && t > 0.2f && t < 0.85f) {
                float branchAngle = random.nextFloat() * (float) (Math.PI * 2);
                float branchLength = length * (0.15f + random.nextFloat() * 0.25f);
                float bx = px + (float) Math.cos(branchAngle) * branchLength;
                float by = py + (float) Math.sin(branchAngle) * branchLength;

                drawJaggedBolt(g, px, py, bx, by, life * 0.7f, thickness * 0.6f,
                        seed + s * 3.1f, hueShift, branchesLeft - 1);
            }

            prevX = px;
            prevY = py;
        }
    }

    private void drawNodes(NvGraphic g, float audioPower) {
        g.beginBatch();
        for (Node n : nodes) {
            float brightness = Math.min(1f, 0.25f + n.charge * 0.9f + audioPower * 0.3f) * flicker;
            float size = 2f + n.charge * 4f + audioPower * 3f;

            g.batchDrawOval(n.x, n.y, size, 8,
                    0.5f * brightness, 0.75f * brightness, brightness);
        }
        g.endBatch();
    }

    private void drawCoilCore(NvGraphic g) {
        float pulseSize = 14f + coilCharge * 22f + (float) Math.sin(coilRotation * 4f) * 2f;

        g.beginBatch();
        g.batchDrawOval(centerX, centerY, pulseSize * 3f, 32,
                0.15f * flicker, 0.25f * flicker, 0.45f * flicker);
        g.batchDrawOval(centerX, centerY, pulseSize * 1.7f, 28,
                0.35f * flicker, 0.55f * flicker, 0.85f * flicker);
        g.batchDrawOval(centerX, centerY, pulseSize * 0.8f, 20,
                Math.min(1f, 0.8f * flicker), Math.min(1f, 0.9f * flicker), flicker);
        g.endBatch();
    }
}