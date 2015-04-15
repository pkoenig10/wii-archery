package game.state;

import game.Game;
import game.GameMoteFinder;
import game.controller.GameController;

import java.util.concurrent.Callable;

import motej.Extension;
import motej.Mote;
import motej.event.CoreButtonEvent;
import motej.event.CoreButtonListener;
import motej.event.ExtensionEvent;
import motej.event.ExtensionListener;
import motej.request.ReportModeRequest;
import motejx.extensions.motionplus.MotionPlusEvent;
import motejx.extensions.motionplus.MotionPlusListener;
import motejx.extensions.motionplusnunchuk.MotionPlusNunchuk;
import motejx.extensions.nunchuk.NunchukButtonEvent;
import motejx.extensions.nunchuk.NunchukButtonListener;
import tonegod.gui.controls.text.Label;
import tonegod.gui.core.Element;
import tonegod.gui.core.Screen;
import tonegod.gui.effects.BatchEffect;
import tonegod.gui.effects.Effect;
import tonegod.gui.effects.Effect.EffectEvent;
import tonegod.gui.effects.Effect.EffectType;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.font.LineWrapMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;

public class MoteFinderScreenState extends AbstractAppState implements
        ExtensionListener, CoreButtonListener, NunchukButtonListener,
        MotionPlusListener {

    private static final ColorRGBA BACKGROUND_COLOR = ColorRGBA.White;
    private static final ColorRGBA MOTE_FINDER_COLOR = ColorRGBA.Black;
    private static final ColorRGBA DISCONNECTED_COLOR = ColorRGBA.LightGray;
    private static final ColorRGBA CONNECTED_COLOR = ColorRGBA.Black;

    private static final float MOTE_FINDER_FONT_SIZE = 65;
    private static final float MOTE_FONT_SIZE = 55;

    private static final float CHECKMARK_SIZE = 55;

    private static final float MOTE_SPACING = 110;
    private static final int CHECKMARK_SPACING = 220;

    private static final float FADE_TIME = 1;

    private final Game game;
    private final Screen screen;
    private final ViewPort viewPort;
    private final AppSettings settings;

    private Mote mote;
    private MotionPlusNunchuk motionPlusNunchuk;

    private boolean moteConnected;
    private boolean nunchukConnected;
    private boolean motionPlusConnected;

    private Label moteFinderLabel;
    private Label moteLabel;
    private Label nunchukLabel;
    private Label motionPlusLabel;
    private Element moteCheckmark;
    private Element nunchukCheckmark;
    private Element motionPlusCheckmark;

    private BatchEffect fadeEffect;
    private boolean fadeStarted;

    public MoteFinderScreenState(Game game) {
        this.game = game;
        this.screen = game.getScreen();
        this.viewPort = game.getViewPort();
        this.settings = game.getContext().getSettings();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        moteConnected = false;
        nunchukConnected = false;
        motionPlusConnected = false;

        fadeEffect = new BatchEffect();
        fadeStarted = false;

        initMoteFinderScreen();

        viewPort.setBackgroundColor(BACKGROUND_COLOR);
        screen.addElement(moteFinderLabel);
        screen.addElement(moteLabel);
        screen.addElement(nunchukLabel);
        screen.addElement(motionPlusLabel);

        findMote();
    }

    @Override
    public void cleanup() {
        super.cleanup();

        screen.removeElement(moteFinderLabel);
        screen.removeElement(moteLabel);
        screen.removeElement(nunchukLabel);
        screen.removeElement(motionPlusLabel);
        screen.removeElement(moteCheckmark);
        screen.removeElement(nunchukCheckmark);
        screen.removeElement(motionPlusCheckmark);
    }

    @Override
    public void update(float tpf) {
        if (moteConnected && nunchukConnected && motionPlusConnected) {
            if (!fadeStarted) {
                game.setController(new GameController(game, mote,
                        motionPlusNunchuk));
                screen.getEffectManager().applyBatchEffect(fadeEffect);
                fadeStarted = true;
            } else {
                if (!fadeEffect.getIsActive()) {
                    game.doStart();
                }
            }

        }
    }

    private void initMoteFinderScreen() {
        moteFinderLabel = new Label(screen, "Mote Finder Label", new Vector2f(
                settings.getWidth() / 8, 0), new Vector2f(
                3 * settings.getWidth() / 4, settings.getHeight() / 2));
        moteFinderLabel.setFont("Interface/Fonts/text.fnt");
        moteFinderLabel.setFontSize(MOTE_FINDER_FONT_SIZE);
        moteFinderLabel.setFontColor(MOTE_FINDER_COLOR);
        moteFinderLabel.setTextAlign(Align.Center);
        moteFinderLabel.setTextVAlign(VAlign.Center);
        moteFinderLabel.setTextWrap(LineWrapMode.Word);
        moteFinderLabel
                .setText("Connect the Wii remote by pressing 1 and 2.\n\nPlace the Wii remote on a flat surface until the MotionPlus is connected.");
        addFadeEffect(moteFinderLabel);

        moteLabel = new Label(screen, "Mote Label", new Vector2f(0,
                settings.getHeight() / 2), new Vector2f(settings.getWidth(),
                MOTE_SPACING));
        moteLabel.setFont("Interface/Fonts/text.fnt");
        moteLabel.setFontSize(MOTE_FONT_SIZE);
        moteLabel.setFontColor(DISCONNECTED_COLOR);
        moteLabel.setTextAlign(Align.Center);
        moteLabel.setTextVAlign(VAlign.Center);
        moteLabel.setText("Wii Remote");
        addFadeEffect(moteLabel);

        nunchukLabel = new Label(screen, "Nunchuk Label", new Vector2f(0,
                settings.getHeight() / 2 + MOTE_SPACING), new Vector2f(
                settings.getWidth(), MOTE_SPACING));
        nunchukLabel.setFont("Interface/Fonts/text.fnt");
        nunchukLabel.setFontSize(MOTE_FONT_SIZE);
        nunchukLabel.setFontColor(DISCONNECTED_COLOR);
        nunchukLabel.setTextAlign(Align.Center);
        nunchukLabel.setTextVAlign(VAlign.Center);
        nunchukLabel.setText("Nunchuk");
        addFadeEffect(nunchukLabel);

        motionPlusLabel = new Label(screen, "MotionPlus Label", new Vector2f(0,
                settings.getHeight() / 2 + 2 * MOTE_SPACING), new Vector2f(
                settings.getWidth(), MOTE_SPACING));
        motionPlusLabel.setFont("Interface/Fonts/text.fnt");
        motionPlusLabel.setFontSize(MOTE_FONT_SIZE);
        motionPlusLabel.setFontColor(DISCONNECTED_COLOR);
        motionPlusLabel.setTextAlign(Align.Center);
        motionPlusLabel.setTextVAlign(VAlign.Center);
        motionPlusLabel.setText("MotionPlus");
        addFadeEffect(motionPlusLabel);

        moteCheckmark = new Element(screen, "Mote Checkmark", new Vector2f(
                settings.getWidth() / 2 - CHECKMARK_SPACING,
                settings.getHeight() / 2 + MOTE_SPACING / 2 - CHECKMARK_SIZE
                        / 2), new Vector2f(CHECKMARK_SIZE, CHECKMARK_SIZE),
                Vector4f.ZERO, "Interface/checkmark.png");
        addFadeEffect(moteCheckmark);

        nunchukCheckmark = new Element(screen, "Nunchuk Checkmark",
                new Vector2f(settings.getWidth() / 2 - CHECKMARK_SPACING,
                        settings.getHeight() / 2 + 3 * MOTE_SPACING / 2
                                - CHECKMARK_SIZE / 2), new Vector2f(
                        CHECKMARK_SIZE, CHECKMARK_SIZE), Vector4f.ZERO,
                "Interface/checkmark.png");
        addFadeEffect(nunchukCheckmark);

        motionPlusCheckmark = new Element(screen, "MotionPlus Checkmark",
                new Vector2f(settings.getWidth() / 2 - CHECKMARK_SPACING,
                        settings.getHeight() / 2 + 5 * MOTE_SPACING / 2
                                - CHECKMARK_SIZE / 2), new Vector2f(
                        CHECKMARK_SIZE, CHECKMARK_SIZE), Vector4f.ZERO,
                "Interface/checkmark.png");
        addFadeEffect(motionPlusCheckmark);
    }

    private void addFadeEffect(Element element) {
        Effect effect = new Effect(EffectType.FadeOut, EffectEvent.Show,
                FADE_TIME);
        effect.setElement(element);
        fadeEffect.addEffect(effect);
    }

    private void findMote() {
        new Thread() {
            @Override
            public void run() {
                try {
                    GameMoteFinder finder = new GameMoteFinder();
                    mote = finder.findMote();
                    if (mote != null) {
                        mote.addExtensionListener(MoteFinderScreenState.this);
                        mote.addCoreButtonListener(MoteFinderScreenState.this);
                        mote.activateMotionPlusNunchuk();
                        mote.setReportMode(ReportModeRequest.DATA_REPORT_0x32);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    game.stop();
                }
            }
        }.start();
    }

    private void moteConnected() {
        moteLabel.setFontColor(CONNECTED_COLOR);
        screen.addElement(moteCheckmark);
        moteConnected = true;
    }

    private void nunchukConnected() {
        nunchukLabel.setFontColor(CONNECTED_COLOR);
        screen.addElement(nunchukCheckmark);
        nunchukConnected = true;
    }

    private void motionPlusConnected() {
        motionPlusLabel.setFontColor(CONNECTED_COLOR);
        screen.addElement(motionPlusCheckmark);
        motionPlusConnected = true;
    }

    @Override
    public void extensionConnected(ExtensionEvent evt) {
        final Extension ext = evt.getExtension();
        if (ext instanceof MotionPlusNunchuk) {
            motionPlusNunchuk = (MotionPlusNunchuk) ext;
            motionPlusNunchuk.addMotionPlusEventListener(this);
            motionPlusNunchuk.addNunchukButtonListener(this);
        }
    }

    @Override
    public void extensionDisconnected(ExtensionEvent arg0) {
    }

    @Override
    public void buttonPressed(CoreButtonEvent arg0) {
        game.enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                moteConnected();
                return null;
            }
        });
        mote.removeCoreButtonListener(this);
    }

    @Override
    public void buttonPressed(NunchukButtonEvent arg0) {
        game.enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                nunchukConnected();
                return null;
            }
        });
        motionPlusNunchuk.removeNunchukButtonListener(this);
    }

    @Override
    public void speedChanged(MotionPlusEvent arg0) {
        game.enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                motionPlusConnected();
                return null;
            }
        });
        motionPlusNunchuk.removeMotionPlusListener(this);
    }

}
