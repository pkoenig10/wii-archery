package game.state;

import game.Game;
import game.GameStats;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import tonegod.gui.controls.text.Label;
import tonegod.gui.core.Screen;
import tonegod.gui.effects.Effect;
import tonegod.gui.effects.Effect.EffectEvent;
import tonegod.gui.effects.Effect.EffectType;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.collision.CollisionResult;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.light.Light;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

public class GameRunningState extends AbstractAppState {

    private static enum State {
        START, GAME, END
    }

    private static final int START_COUNTDOWN_TIME = 3;
    private static final float START_COUNTDOWN_FONT_SIZE = 700;
    private static final ColorRGBA START_COUNTDOWN_COLOR = ColorRGBA.White;
    private static final float START_COUNTDOWN_EFFECT_TIME = 1.5f;

    private static final int GAME_COUNTDOWN_TIME = 5;
    private static final float GAME_COUNTDOWN_FONT_SIZE = 700;
    private static final ColorRGBA GAME_COUNTDOWN_COLOR = new ColorRGBA(1f, 1f,
            1f, 0.5f);
    private static final float GAME_COUNTDOWN_EFFECT_TIME = 2.5f;

    private static final float HUD_TEXT_FONT_SIZE = 75;
    private static final float HUD_FONT_SIZE = 140;
    private static final ColorRGBA HUD_FONT_COLOR = ColorRGBA.White;

    private static final int HUD_PADDING = 30;
    private static final int HUD_SPACING_X = 300;
    private static final int HUD_SPACING_Y = 0;

    private static final float HIT_FONT_SIZE = 70;
    private static final ColorRGBA HIT_FONT_COLOR = ColorRGBA.White;
    private static final float HIT_TIME = 1;
    private static final float HIT_SPEED = 30;

    private static final int TARGET_MIN = 2;
    private static final int TARGET_MAX = 5;
    private static final float TARGET_TIME_MIN = 1;
    private static final float TARGET_TIME_MAX = 2;
    private static final float TARGET_START_PADDING = 3;
    private static final float TARGET_END_PADDING = 4;
    private static final float TARGET_SIZE = 1.5f;
    private static final float TARGET_SPEED = 20;

    private static final int GAME_TIME = 30;
    private static final float START_DELAY = 1;
    private static final float END_DELAY = 1.5f;

    private static final int BONUS_MAX = 5;

    private final Game game;
    private final Node floorNode;
    private final Node targetNode;
    private final Screen screen;
    private final AssetManager assetManager;
    private final AppStateManager stateManager;
    private final Camera cam;
    private final AppSettings settings;

    private final Random random = new Random();

    private final Map<Spatial, Light> targetLightMap = new HashMap<Spatial, Light>();

    private State state;

    private CountdownState startCountdownState;
    private CountdownState endCountdownState;

    private AudioNode targetSound;
    private AudioNode startCountdownSound;
    private AudioNode startCountdownEndedSound;
    private AudioNode gameCountdownSound;
    private AudioNode gameCountdownEndedSound;

    private float targetStartXMin;
    private float targetStartXMax;
    private float targetStartYMin;
    private float targetStartYMax;
    private float targetStartZMin;
    private float targetStartZMax;
    private float targetEndX;
    private float targetEndYMin;
    private float targetEndYMax;
    private float targetEndZ;

    private Label scoreTextLabel;
    private Label scoreLabel;
    private Label timeTextLabel;
    private Label timeLabel;

    private boolean startCountdownStarted;
    private boolean gameCountdownStarted;

    private float time;
    private float targetTime;
    private float targetWaitTime;

    private int score;
    private int bonus;

    private int shotsHit;
    private int shotsTotal;
    private int targetsHit;
    private int targetsTotal;

    public GameRunningState(Game game) {
        this.game = game;
        this.floorNode = game.getFloorNode();
        this.targetNode = game.getTargetNode();
        this.screen = game.getScreen();
        this.assetManager = game.getAssetManager();
        this.stateManager = game.getStateManager();
        this.cam = game.getCamera();
        this.settings = game.getContext().getSettings();

        initSound();
        initConstants();

        startCountdownState = new CountdownState(game, this,
                START_COUNTDOWN_TIME, START_COUNTDOWN_FONT_SIZE,
                START_COUNTDOWN_COLOR, START_COUNTDOWN_EFFECT_TIME,
                startCountdownSound);
        endCountdownState = new CountdownState(game, this, GAME_COUNTDOWN_TIME,
                GAME_COUNTDOWN_FONT_SIZE, GAME_COUNTDOWN_COLOR,
                GAME_COUNTDOWN_EFFECT_TIME, gameCountdownSound);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        startCountdownStarted = false;
        gameCountdownStarted = false;

        time = 0;
        targetTime = 0;
        targetWaitTime = 0;

        score = 0;
        bonus = 0;

        shotsHit = 0;
        shotsTotal = 0;
        targetsHit = 0;
        targetsTotal = 0;

        initHud();

        updateScore(0);
        updateTime(GAME_TIME);

        screen.addElement(scoreTextLabel);
        screen.addElement(scoreLabel);
        screen.addElement(timeTextLabel);
        screen.addElement(timeLabel);

        doStart();
    }

    @Override
    public void cleanup() {
        super.cleanup();

        screen.removeElement(scoreTextLabel);
        screen.removeElement(scoreLabel);
        screen.removeElement(timeTextLabel);
        screen.removeElement(timeLabel);
    }

    @Override
    public void update(float tpf) {
        time += tpf;
        switch (state) {
        case START:
            if (time > START_DELAY && !startCountdownStarted) {
                stateManager.attach(startCountdownState);
                startCountdownStarted = true;
            }
            break;
        case GAME:
            float gameTime = GAME_TIME - time;
            updateTime(gameTime);
            if (gameTime < GAME_COUNTDOWN_TIME && !gameCountdownStarted) {
                stateManager.attach(endCountdownState);
                gameCountdownStarted = true;
            }
            targetTime += tpf;
            if (targetTime > targetWaitTime) {
                initTargets();
                targetTime = 0;
                targetWaitTime = (random.nextFloat() * (TARGET_TIME_MAX - TARGET_TIME_MIN))
                        + TARGET_TIME_MIN;
            }
            break;
        case END:
            if (time > END_DELAY) {
                game.setStats(new GameStats(score, shotsHit, shotsTotal,
                        targetsHit, targetsTotal));
                screen.removeElement(scoreTextLabel);
                screen.removeElement(scoreLabel);
                screen.removeElement(timeTextLabel);
                screen.removeElement(timeLabel);
                game.doScore();
            }
            break;
        }
    }

    private void initSound() {
        targetSound = new AudioNode(assetManager, "Sounds/target.wav");
        targetSound.setPositional(false);
        targetSound.setVolume(0.25f);

        startCountdownSound = new AudioNode(assetManager,
                "Sounds/start_countdown.wav");
        startCountdownSound.setPositional(false);

        startCountdownEndedSound = new AudioNode(assetManager,
                "Sounds/start_countdown_ended.wav");
        startCountdownEndedSound.setPositional(false);

        gameCountdownSound = new AudioNode(assetManager,
                "Sounds/game_countdown.wav");
        gameCountdownSound.setPositional(false);

        gameCountdownEndedSound = new AudioNode(assetManager,
                "Sounds/game_countdown_ended.wav");
        gameCountdownEndedSound.setPositional(false);
    }

    private void initConstants() {
        targetStartZMin = -game.getRoomZ() + TARGET_START_PADDING;
        targetStartZMax = -((game.getRoomX() * cam.getFrustumNear() / cam
                .getFrustumRight()) - 1) - TARGET_START_PADDING;

        targetStartXMin = -game.getRoomX() + TARGET_START_PADDING;
        targetStartXMax = game.getRoomX() - TARGET_START_PADDING;

        targetStartYMin = TARGET_START_PADDING;
        targetStartYMax = cam.getWorldCoordinates(
                new Vector2f(0, settings.getHeight()),
                cam.getViewToProjectionZ(-targetStartZMax + 1)).getY()
                - TARGET_START_PADDING;

        targetEndX = TARGET_END_PADDING;

        targetEndYMin = cam.getWorldCoordinates(new Vector2f(0, 0), 0).getY();
        targetEndYMax = cam.getWorldCoordinates(
                new Vector2f(0, settings.getHeight()), 0).getY();

        targetEndZ = 0;
    }

    private void initHud() {
        scoreTextLabel = new Label(screen, "Score Text Label", new Vector2f(0,
                HUD_PADDING), new Vector2f(HUD_SPACING_X, HUD_TEXT_FONT_SIZE));
        scoreTextLabel.setFont("Interface/Fonts/digital.fnt");
        scoreTextLabel.setFontSize(HUD_TEXT_FONT_SIZE);
        scoreTextLabel.setFontColor(HUD_FONT_COLOR);
        scoreTextLabel.setTextAlign(Align.Center);
        scoreTextLabel.setTextVAlign(VAlign.Center);
        scoreTextLabel.setText("Score:");

        scoreLabel = new Label(screen, "Score Label", new Vector2f(0,
                HUD_PADDING + HUD_TEXT_FONT_SIZE + HUD_SPACING_Y),
                new Vector2f(HUD_SPACING_X, HUD_FONT_SIZE));
        scoreLabel.setFont("Interface/Fonts/digital.fnt");
        scoreLabel.setFontSize(HUD_FONT_SIZE);
        scoreLabel.setFontColor(HUD_FONT_COLOR);
        scoreLabel.setTextAlign(Align.Center);
        scoreLabel.setTextVAlign(VAlign.Center);

        timeTextLabel = new Label(screen, "Time Text Label", new Vector2f(
                settings.getWidth() - HUD_SPACING_X, HUD_PADDING),
                new Vector2f(HUD_SPACING_X, HUD_TEXT_FONT_SIZE));
        timeTextLabel.setFont("Interface/Fonts/digital.fnt");
        timeTextLabel.setFontSize(HUD_TEXT_FONT_SIZE);
        timeTextLabel.setFontColor(HUD_FONT_COLOR);
        timeTextLabel.setTextAlign(Align.Center);
        timeTextLabel.setTextVAlign(VAlign.Center);
        timeTextLabel.setText("Time:");

        timeLabel = new Label(screen, "Time Label", new Vector2f(
                settings.getWidth() - HUD_SPACING_X, HUD_PADDING
                        + HUD_TEXT_FONT_SIZE + HUD_SPACING_Y), new Vector2f(
                HUD_SPACING_X, HUD_FONT_SIZE));
        timeLabel.setFont("Interface/Fonts/digital.fnt");
        timeLabel.setFontSize(HUD_FONT_SIZE);
        timeLabel.setFontColor(HUD_FONT_COLOR);
        timeLabel.setTextAlign(Align.Center);
        timeLabel.setTextVAlign(VAlign.Center);
    }

    private void initTargets() {
        if (targetNode.getChildren().size() < TARGET_MAX) {
            initTarget();
            while (targetNode.getChildren().size() < TARGET_MIN) {
                initTarget();
            }
        }
    }

    private void initTarget() {
        Geometry target = new Geometry("Target", new Box(TARGET_SIZE,
                TARGET_SIZE, TARGET_SIZE));
        target.setMaterial(game.getTargetMat());
        target.setQueueBucket(Bucket.Transparent);

        float startX = (random.nextFloat() * (targetStartXMax - targetStartXMin))
                + targetStartXMin;
        float startY = (random.nextFloat() * (targetStartYMax - targetStartYMin))
                + targetStartYMin;
        float startZ = (random.nextFloat() * (targetStartZMax - targetStartZMin))
                + targetStartZMin;
        Vector3f start = new Vector3f(startX, startY, startZ);
        target.setLocalTranslation(start);

        float endX = random.nextBoolean() ? targetEndX : -targetEndX;
        float endY = (random.nextFloat() * (targetEndYMax - targetEndYMin))
                + targetEndYMin;
        float endZ = targetEndZ;
        Vector3f end = new Vector3f(endX, endY, endZ);

        Quaternion rotation = new Quaternion(new float[] { random.nextFloat(),
                random.nextFloat(), random.nextFloat() });
        target.setLocalRotation(rotation);

        SpotLight targetLight = new SpotLight();
        targetLight.setSpotRange(6);
        targetLight.setSpotInnerAngle(15 * FastMath.DEG_TO_RAD);
        targetLight.setSpotOuterAngle(30 * FastMath.DEG_TO_RAD);
        targetLight.setColor(game.getTargetLightColor());
        targetLight.setDirection(new Vector3f(0, -1, 0));

        target.addControl(new TargetControl(start, end, targetLight));

        targetSound.playInstance();

        targetLightMap.put(target, targetLight);
        floorNode.addLight(targetLight);
        targetNode.attachChild(target);
    }

    private void initHit(Vector3f loc) {
        Vector3f loc2d = cam.getScreenCoordinates(loc);
        Label hitLabel = new Label(screen, new Vector2f(loc2d.getX()
                - HIT_FONT_SIZE, settings.getHeight() - loc2d.getY() - 2
                * HIT_FONT_SIZE), new Vector2f(2 * HIT_FONT_SIZE,
                2 * HIT_FONT_SIZE));
        hitLabel.setFont("Interface/Fonts/digital.fnt");
        hitLabel.setFontSize(HIT_FONT_SIZE);
        hitLabel.setFontColor(HIT_FONT_COLOR);
        hitLabel.setTextAlign(Align.Center);
        hitLabel.setTextVAlign(VAlign.Center);
        hitLabel.setText(String.format("+%d", bonus));
        hitLabel.addControl(new HitTextControl());
        screen.addElement(hitLabel);
    }

    private void updateScore(int score) {
        scoreLabel.setText(String.format("%d", score));
    }

    private void updateTime(float gameTime) {
        timeLabel.setText(String.format("%02d", (int) FastMath.ceil(gameTime)));
    }

    public void hit(CollisionResult collision) {
        removeTarget(collision.getGeometry());

        bonus = Math.min(bonus + 1, BONUS_MAX);
        score += bonus;
        updateScore(score);
        if (state == State.GAME) {
            shotsHit++;
            shotsTotal++;
            targetsHit++;
            targetsTotal++;
        }
        initHit(collision.getContactPoint());

        if (targetNode.getChildren().size() == 0) {
            initTargets();
        }
    }

    public void miss() {
        bonus = 0;
        if (state == State.GAME) {
            shotsTotal++;
        }
    }

    private void removeTarget(Spatial target) {
        floorNode.removeLight(targetLightMap.get(target));
        target.removeFromParent();
        targetLightMap.remove(target);
    }

    public void countdownEnded() {
        switch (state) {
        case START:
            startCountdownEndedSound.playInstance();
            doGame();
            break;
        case GAME:
            gameCountdownEndedSound.playInstance();
            doEnd();
            break;
        default:
            break;
        }
    }

    public void doStart() {
        time = 0;
        state = State.START;
    }

    public void doGame() {
        time = 0;
        state = State.GAME;
    }

    public void doEnd() {
        updateTime(0);
        for (Spatial target : targetNode.getChildren()) {
            removeTarget(target);
        }
        time = 0;
        state = State.END;
    }

    private class TargetControl extends AbstractControl {

        private Vector3f velocity;
        private Vector3f rotation;
        private SpotLight light;

        public TargetControl(Vector3f start, Vector3f end, SpotLight light) {
            this.velocity = end.subtract(start).normalizeLocal()
                    .multLocal(TARGET_SPEED);
            this.rotation = new Vector3f(random.nextFloat(),
                    random.nextFloat(), random.nextFloat());
            this.light = light;
        }

        @Override
        protected void controlUpdate(float tpf) {
            spatial.move(velocity.mult(tpf));
            spatial.rotate(rotation.getX() * tpf, rotation.getY() * tpf,
                    rotation.getZ() * tpf);
            Vector3f loc = spatial.getLocalTranslation();
            light.setPosition(new Vector3f(loc.getX(), 5, loc.getZ()));
            if (spatial.getLocalTranslation().getZ() > TARGET_SIZE) {
                removeTarget(spatial);
                targetsTotal++;
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
        }
    }

    private class HitTextControl extends AbstractControl {

        private boolean fadeStarted = false;
        private float time = 0;

        @Override
        protected void controlUpdate(float tpf) {
            time += tpf;
            if (time < HIT_TIME) {
                spatial.move(0, HIT_SPEED * tpf, 0);
                if (time > HIT_TIME / 2 && !fadeStarted) {
                    Effect fadeEffect = new Effect(EffectType.FadeOut,
                            EffectEvent.Show, HIT_TIME / 2);
                    fadeEffect.setElement((Label) spatial);
                    screen.getEffectManager().applyEffect(fadeEffect);
                    fadeStarted = true;
                }
            } else {
                screen.removeElement((Label) spatial);
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
        }
    }
}
