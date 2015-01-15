package game.state;

import game.Game;
import tonegod.gui.controls.text.Label;
import tonegod.gui.core.Screen;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.font.LineWrapMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

public class StartScreenState extends AbstractAppState {

    private static final ColorRGBA BACKGROUND_COLOR = ColorRGBA.DarkGray;
    private static final ColorRGBA START_TITLE_COLOR = ColorRGBA.White;
    private static final ColorRGBA START_COLOR = ColorRGBA.White;
    private static final ColorRGBA START_BUTTON_COLOR = ColorRGBA.Black;

    private static final float START_TITLE_FONT_SIZE = 75;
    private static final float START_FONT_SIZE = 50;
    private static final float START_BUTTON_FONT_SIZE = 75;

    private static final float START_TITLE_SPACING = 150;
    private static final float START_PADDING = 100;

    private static final Vector3f START_BUTTON_LOCATION = new Vector3f(0, 5,
            -12);
    private static final Vector3f START_BUTTON_ROTATION = new Vector3f(0.4f,
            0.4f, 0.4f);

    private final Game game;
    private final Node targetNode;
    private final Screen screen;
    private final Camera cam;
    private final ViewPort viewPort;
    private final AppSettings settings;

    private Label startTitleLabel;
    private Label startLabel;
    private Label startButtonLabel;

    public StartScreenState(Game game) {
        this.game = game;
        this.targetNode = game.getTargetNode();
        this.screen = game.getScreen();
        this.cam = game.getCamera();
        this.viewPort = game.getViewPort();
        this.settings = game.getContext().getSettings();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        viewPort.setBackgroundColor(BACKGROUND_COLOR);
        initStartScreen();
        screen.addElement(startTitleLabel);
        screen.addElement(startLabel);
        screen.addElement(startButtonLabel);
        initStartButton();
    }

    @Override
    public void cleanup() {
        super.cleanup();

        screen.removeElement(startTitleLabel);
        screen.removeElement(startLabel);
        screen.removeElement(startButtonLabel);
    }

    private void initStartScreen() {
        startTitleLabel = new Label(screen, "Score Title Label", Vector2f.ZERO,
                new Vector2f(settings.getWidth(), START_TITLE_SPACING));
        startTitleLabel.setFont("Interface/Fonts/text.fnt");
        startTitleLabel.setFontSize(START_TITLE_FONT_SIZE);
        startTitleLabel.setFontColor(START_TITLE_COLOR);
        startTitleLabel.setTextAlign(Align.Center);
        startTitleLabel.setTextVAlign(VAlign.Center);
        startTitleLabel.setTextWrap(LineWrapMode.Word);
        startTitleLabel.setText("HOW TO PLAY");

        startLabel = new Label(screen, "Score Label", new Vector2f(
                START_PADDING, START_TITLE_SPACING), new Vector2f(
                settings.getWidth() - 2 * START_PADDING, settings.getHeight()
                        / 2 - START_TITLE_SPACING));
        startLabel.setFont("Interface/Fonts/text.fnt");
        startLabel.setFontSize(START_FONT_SIZE);
        startLabel.setFontColor(START_COLOR);
        startLabel.setTextAlign(Align.Left);
        startLabel.setTextVAlign(VAlign.Center);
        startLabel.setTextWrap(LineWrapMode.Word);
        startLabel
                .setText("Press A on the Wii Remote to recenter the crosshair.\n\n"
                        + "To shoot:\n"
                        + "1)  Press and hold a button on the Nunchuk.  The crosshair will turn blue.\n"
                        + "2)  Quickly pull back the Nunchuk.\n"
                        + "3)  When the crosshair turns green, release the button to fire.\n\n"
                        + "Practice shooting on this screen.  Then shoot the start button to begin.");

        startButtonLabel = new Label(screen, "Start Button Label",
                new Vector2f(0, settings.getHeight()
                        - cam.getScreenCoordinates(START_BUTTON_LOCATION)
                                .getY() - START_BUTTON_FONT_SIZE / 2),
                new Vector2f(settings.getWidth(), START_BUTTON_FONT_SIZE));
        startButtonLabel.setFont("Interface/Fonts/text.fnt");
        startButtonLabel.setFontSize(START_BUTTON_FONT_SIZE);
        startButtonLabel.setFontColor(START_BUTTON_COLOR);
        startButtonLabel.setTextAlign(Align.Center);
        startButtonLabel.setTextVAlign(VAlign.Center);
        startButtonLabel.setText("START");
    }

    private void initStartButton() {
        Geometry startButton = new Geometry("Start Button", new Box(1, 1, 1));
        startButton.setMaterial(game.getTargetMat());
        startButton.setQueueBucket(Bucket.Transparent);
        startButton.setLocalTranslation(START_BUTTON_LOCATION);
        startButton.addControl(new StartButtonControl());
        targetNode.attachChild(startButton);
    }

    public void hit(CollisionResult collision) {
        collision.getGeometry().removeFromParent();
        game.doGame();
    }

    private class StartButtonControl extends AbstractControl {
        @Override
        protected void controlUpdate(float tpf) {
            Vector3f rotation = START_BUTTON_ROTATION.mult(tpf);
            spatial.rotate(rotation.getX(), rotation.getY(), rotation.getZ());
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
        }
    }
}
