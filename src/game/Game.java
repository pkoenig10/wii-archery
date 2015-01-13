package game;

import game.controller.GameController;
import game.controller.ShootState;
import game.mesh.FloorQuad;
import game.state.GameRunningState;
import game.state.MoteFinderScreenState;
import game.state.ScoreScreenState;
import game.state.StartScreenState;
import tonegod.gui.core.Screen;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapFont;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.ui.Picture;

public class Game extends SimpleApplication {

    private static enum State {
        MOTE_FINDER, START, GAME, SCORE
    }

    private static final int ROLL_MAX = 20;
    private static final int PITCH_MAX = 10;

    private static final float CAMERA_HEIGHT = 8;

    private static final float ROOM_DEPTH = 100;

    private static final int CROSSHAIR_SIZE = 100;

    private static final ColorRGBA LIGHT_COLOR = ColorRGBA.Cyan;
    private static final ColorRGBA LIGHT_GLOW_COLOR = ColorRGBA.Cyan.mult(0.9f);

    private static final ColorRGBA TARGET_COLOR = new ColorRGBA(1, 0.7f, 0,
            0.6f);
    private static final ColorRGBA TARGET_GLOW_COLOR = new ColorRGBA(1, 0.9f,
            0.2f, 0.95f);
    private static final ColorRGBA TARGET_LIGHT_COLOR = TARGET_COLOR.mult(2);

    private Screen screen;

    private GameController controller;

    private State state;

    private MoteFinderScreenState moteFinderScreenState;
    private StartScreenState startScreenState;
    private GameRunningState gameRunningState;
    private ScoreScreenState scoreScreenState;

    private final Node floorNode = new Node("Floor Node");
    private final Node roomNode = new Node("Room Node");
    private Node crosshairNode = new Node("Crosshair Node");
    private final Node targetNode = new Node("Targets Node");

    private AudioNode shootSound;
    private AudioNode hitSound;

    private int roomX;
    private int roomY;
    private int roomZ;

    private BitmapFont textFont;
    private BitmapFont digitalFont;

    private Material floorMat;
    private Material wallMat;
    private Material lightMat;
    private Material targetMat;
    private Material debrisMat;

    private Picture crosshair;
    private float crosshairX;
    private float crosshairY;

    @Override
    public void simpleInitApp() {
        screen = new Screen(this);
        guiNode.addControl(screen);

        initCamera();
        initLight();
        initSound();
        initConstants();
        initFonts();
        initMaterials();
        initGlow();
        initCrosshair();
        initRoom();

        moteFinderScreenState = new MoteFinderScreenState(this);
        startScreenState = new StartScreenState(this);
        gameRunningState = new GameRunningState(this);
        scoreScreenState = new ScoreScreenState(this);

        inputManager.addMapping("Action", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(actionListener, "Action");

        rootNode.attachChild(targetNode);

        doMoteFinder();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (controller != null) {
            crosshairX = (float) (((controller.getRollVal() + ROLL_MAX) * settings
                    .getWidth()) / (2 * ROLL_MAX));
            crosshairY = (float) (((-controller.getPitchVal() + PITCH_MAX) * settings
                    .getHeight()) / (2 * PITCH_MAX));
        } else {
            Vector2f cursorPosition = inputManager.getCursorPosition();
            crosshairX = cursorPosition.getX();
            crosshairY = cursorPosition.getY();
        }
        crosshair.setPosition(crosshairX - CROSSHAIR_SIZE / 2, crosshairY
                - CROSSHAIR_SIZE / 2);
    }

    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Action") && !keyPressed) {
                switch (state) {
                case MOTE_FINDER:
                    doStart();
                    break;
                case START:
                case GAME:
                    shoot();
                    break;
                default:
                    break;
                }
            }
        }
    };

    private void initCamera() {
        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(0, CAMERA_HEIGHT, 1));
        inputManager.setCursorVisible(false);
    }

    private void initLight() {
        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White);
        rootNode.addLight(ambientLight);

        DirectionalLight directionalLight = new DirectionalLight();
        directionalLight.setDirection(new Vector3f(0, -1, -1));
        directionalLight.setColor(ColorRGBA.White.mult(0.5f));
        rootNode.addLight(directionalLight);
    }

    private void initSound() {
        listener.setLocation(cam.getLocation());

        shootSound = new AudioNode(assetManager, "Sounds/shoot.wav");
        shootSound.setPositional(false);

        hitSound = new AudioNode(assetManager, "Sounds/hit.wav");
        hitSound.setPositional(false);

    }

    private void initConstants() {
        Vector3f room = cam.getWorldCoordinates(
                new Vector2f(0.9f * settings.getWidth(), settings.getHeight()),
                cam.getViewToProjectionZ(ROOM_DEPTH + 1));
        roomX = (int) FastMath.ceil(room.getX());
        roomY = (int) FastMath.ceil(room.getY());
        roomZ = (int) ROOM_DEPTH;
    }

    private void initFonts() {
        textFont = assetManager.loadFont("Interface/Fonts/text.fnt");
        digitalFont = assetManager.loadFont("Interface/Fonts/digital.fnt");
    }

    private void initMaterials() {
        floorMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        Texture floorTex = assetManager.loadTexture("Textures/floor.png");
        floorTex.setWrap(WrapMode.Repeat);
        floorMat.setTexture("DiffuseMap", floorTex);
        floorMat.setBoolean("UseMaterialColors", true);

        floorMat.setColor("Ambient", ColorRGBA.DarkGray);
        floorMat.setColor("Diffuse", ColorRGBA.White);
        floorMat.setBoolean("VertexLighting", true);

        wallMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        Texture wallTex = assetManager.loadTexture("Textures/wall.png");
        wallTex.setWrap(WrapMode.Repeat);
        wallMat.setTexture("DiffuseMap", wallTex);
        wallMat.setBoolean("UseMaterialColors", true);
        wallMat.setColor("Ambient", ColorRGBA.LightGray);
        wallMat.setColor("Diffuse", ColorRGBA.White);

        lightMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        lightMat.setBoolean("UseMaterialColors", true);
        lightMat.setColor("Ambient", LIGHT_COLOR);
        lightMat.setColor("Diffuse", LIGHT_COLOR);
        lightMat.setColor("GlowColor", LIGHT_GLOW_COLOR);

        targetMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        targetMat.setBoolean("UseMaterialColors", true);
        targetMat.setColor("Ambient", TARGET_COLOR);
        targetMat.setColor("Diffuse", TARGET_COLOR);

        targetMat.setColor("GlowColor", TARGET_GLOW_COLOR);
        targetMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        debrisMat = new Material(assetManager,
                "Common/MatDefs/Misc/Particle.j3md");
        Texture debrisTex = assetManager.loadTexture("Textures/debris.png");
        debrisMat.setTexture("Texture", debrisTex);
    }

    private void initGlow() {
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBlurScale(3);
        bloom.setBloomIntensity(0.85f);
        bloom.setDownSamplingFactor(2);
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);
    }

    private void initCrosshair() {
        crosshair = new Picture("Crosshair");
        crosshair.setImage(assetManager,
                "Interface/crosshair_not_shooting.png", true);
        crosshair.setWidth(CROSSHAIR_SIZE);
        crosshair.setHeight(CROSSHAIR_SIZE);
        crosshairNode.attachChild(crosshair);
    }

    private void initRoom() {
        FloorQuad floorQuad = new FloorQuad(2 * roomX, roomZ);
        floorQuad.scaleTextureCoordinates(new Vector2f(3, 3));
        Geometry floor = new Geometry("Floor", floorQuad);
        floor.setMaterial(floorMat);
        floor.setLocalTranslation(-roomX, 0, 0);
        floor.rotate(-FastMath.PI / 2, 0, 0);
        floorNode.attachChild(floor);
        roomNode.attachChild(floorNode);

        Quad leftWallQuad = new Quad(roomZ, roomY);
        leftWallQuad.scaleTextureCoordinates(new Vector2f(1, .5f));
        Geometry leftWall = new Geometry("LeftWall", leftWallQuad);
        leftWall.setMaterial(wallMat);
        leftWall.setLocalTranslation(-roomX, 0, 0);
        leftWall.rotate(0, FastMath.PI / 2, 0);
        roomNode.attachChild(leftWall);

        Quad rightWallQuad = new Quad(roomZ, roomY);
        rightWallQuad.scaleTextureCoordinates(new Vector2f(1, .5f));
        Geometry rightWall = new Geometry("RightWall", rightWallQuad);
        rightWall.setMaterial(wallMat);
        rightWall.setLocalTranslation(roomX, 0, -roomZ);
        rightWall.rotate(0, -FastMath.PI / 2, 0);
        roomNode.attachChild(rightWall);

        Quad backWallQuad = new Quad(2 * roomX, roomY);
        backWallQuad.scaleTextureCoordinates(new Vector2f(1, .5f));
        Geometry backWall = new Geometry("BackWall", backWallQuad);
        backWall.setMaterial(wallMat);
        backWall.setLocalTranslation(-roomX, 0, -roomZ);
        roomNode.attachChild(backWall);

        Geometry leftLight = new Geometry("Box", new Box(0.5f, 0.5f, roomZ / 2));
        leftLight.setLocalTranslation(-roomX, 0, -roomZ / 2);
        leftLight.setMaterial(lightMat);
        roomNode.attachChild(leftLight);

        Geometry rightLight = new Geometry("Box",
                new Box(0.5f, 0.5f, roomZ / 2));
        rightLight.setLocalTranslation(roomX, 0, -roomZ / 2);
        rightLight.setMaterial(lightMat);
        roomNode.attachChild(rightLight);

        Geometry backLight = new Geometry("Box", new Box(roomX, 0.5f, 0.5f));
        backLight.setLocalTranslation(0, 0, -roomZ);
        backLight.setMaterial(lightMat);
        roomNode.attachChild(backLight);
    }

    private void initDebris(Vector3f loc) {
        ParticleEmitter debris = new ParticleEmitter("Debris",
                ParticleMesh.Type.Triangle, 8);
        debris.setMaterial(debrisMat);

        debris.setImagesX(10);
        debris.setImagesY(1);
        debris.setSelectRandomImage(true);

        debris.setNumParticles(8);

        debris.setParticlesPerSec(0);

        debris.setStartSize(0.75f);
        debris.setEndSize(1.25f);

        debris.setStartColor(TARGET_COLOR);
        debris.setEndColor(new ColorRGBA(TARGET_COLOR.getRed(), TARGET_COLOR
                .getGreen(), TARGET_COLOR.getBlue(), 0.1f));

        debris.getParticleInfluencer()
                .setInitialVelocity(new Vector3f(2, 4, 0));
        debris.getParticleInfluencer().setVelocityVariation(1);

        debris.setLowLife(0.7f);
        debris.setHighLife(1.4f);

        debris.setRotateSpeed(3);
        debris.setRandomAngle(true);

        debris.setGravity(0, 10, 0);

        debris.setLocalTranslation(loc);
        rootNode.attachChild(debris);
        debris.emitAllParticles();
    }

    public void updateCrosshair(ShootState shootState) {
        switch (shootState) {
        case NOT_SHOOTING:
            crosshair.setImage(assetManager,
                    "Interface/crosshair_not_shooting.png", true);
            break;
        case DRAWING:
        case LOADING:
            crosshair.setImage(assetManager, "Interface/crosshair_drawing.png",
                    true);
            break;
        case READY:
            crosshair.setImage(assetManager, "Interface/crosshair_ready.png",
                    true);
            break;
        }
    }

    public void shoot() {
        shootSound.playInstance();

        CollisionResults results = new CollisionResults();
        Vector2f click2d = new Vector2f(crosshairX, crosshairY);
        Vector3f click3d = cam.getWorldCoordinates(click2d, 0);
        Vector3f dir = cam.getWorldCoordinates(click2d, 1).subtractLocal(
                click3d);
        Ray ray = new Ray(click3d, dir);
        targetNode.collideWith(ray, results);
        if (results.size() > 0) {
            hit(results.getClosestCollision());
        } else {
            miss();
        }
    };

    private void hit(CollisionResult collision) {
        hitSound.playInstance();
        initDebris(collision.getContactPoint());

        switch (state) {
        case START:
            startScreenState.hit(collision);
            break;
        case GAME:
            gameRunningState.hit(collision);
            break;
        default:
            break;
        }
    }

    private void miss() {
        switch (state) {
        case GAME:
            gameRunningState.miss();
        default:
            break;
        }
    }

    public void setStats(GameStats stats) {
        scoreScreenState.setStats(stats);
    }

    public void doMoteFinder() {
        stateManager.attach(moteFinderScreenState);
        state = State.MOTE_FINDER;
    }

    public void doStart() {
        roomNode.removeFromParent();
        guiNode.attachChild(crosshairNode);
        stateManager.detach(moteFinderScreenState);
        stateManager.detach(scoreScreenState);
        stateManager.attach(startScreenState);
        state = State.START;
    }

    public void doGame() {
        rootNode.attachChild(roomNode);
        stateManager.detach(startScreenState);
        stateManager.attach(gameRunningState);
        state = State.GAME;
    }

    public void doScore() {
        crosshairNode.removeFromParent();
        stateManager.detach(gameRunningState);
        stateManager.attach(scoreScreenState);
        state = State.SCORE;
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public Screen getScreen() {
        return screen;
    }

    public Node getFloorNode() {
        return floorNode;
    }

    public Node getTargetNode() {
        return targetNode;
    }

    public int getRoomX() {
        return roomX;
    }

    public int getRoomY() {
        return roomY;
    }

    public int getRoomZ() {
        return roomZ;
    }

    public BitmapFont getTextFont() {
        return textFont;
    }

    public BitmapFont getDigitalFont() {
        return digitalFont;
    }

    public ColorRGBA getTargetLightColor() {
        return TARGET_LIGHT_COLOR;
    }

    public Material getTargetMat() {
        return targetMat;
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Game");

        Game game = new Game();
        game.setSettings(settings);
        game.start();
    }
}