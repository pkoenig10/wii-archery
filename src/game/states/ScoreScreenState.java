package game.states;

import game.Game;
import game.GameStats;
import tonegod.gui.controls.text.Label;
import tonegod.gui.core.Element;
import tonegod.gui.core.Screen;
import tonegod.gui.effects.Effect;
import tonegod.gui.effects.Effect.EffectEvent;
import tonegod.gui.effects.Effect.EffectType;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;

public class ScoreScreenState extends AbstractAppState {

    private static final float OVERLAY_INTENSITY = 0.4f;

    private static final ColorRGBA STATS_COLOR = ColorRGBA.DarkGray;
    private static final ColorRGBA SCORE_TEXT_COLOR = ColorRGBA.DarkGray;
    private static final ColorRGBA SCORE_COLOR = ColorRGBA.DarkGray;

    private static final float STATS_FONT_SIZE = 85;
    private static final float SCORE_TEXT_FONT_SIZE = 115;
    private static final float SCORE_FONT_SIZE = 230;

    private static final float STATS_SPACING_X = 80;
    private static final float STATS_SPACING_Y = 120;
    private static final float SCORE_TEXT_SPACING = 30;
    private static final float SCORE_SPACING = 20;

    private static final float BACKGROUND_EFFECT_TIME = 0.3f;
    private static final float SCORE_DELAY = 1.5f;
    private static final float SCORE_TIME = 12;

    private final Game game;
    private final Screen screen;
    private final AssetManager assetManager;
    private final ViewPort viewPort;
    private final AppSettings settings;

    private FilterPostProcessor colorOverlayProcessor;

    private AudioNode statsSound;
    private AudioNode scoreSound;

    private Element background;
    private Label scoreTextLabel;
    private Label scoreLabel;
    private Label accuracyTextLabel;
    private Label accuracyLabel;
    private Label percentTargetsHitTextLabel;
    private Label percentTargetsHitLabel;
    private Label trainingScoreTextLabel;
    private Label trainingScoreLabel;

    private Effect backgroundEffect;

    private boolean statsAdded;
    private boolean scoreAdded;

    private float time;

    private GameStats stats;

    public ScoreScreenState(Game game) {
        this.game = game;
        this.screen = game.getScreen();
        this.assetManager = game.getAssetManager();
        this.viewPort = game.getViewPort();
        this.settings = game.getContext().getSettings();

        initSound();
        initOverlay();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        statsAdded = false;
        scoreAdded = false;

        time = 0;

        initBackground();
        initScoreScreen();

        updateStats();

        viewPort.addProcessor(colorOverlayProcessor);
        screen.addElement(background);
        screen.getEffectManager().applyEffect(backgroundEffect);
        statsSound.playInstance();
    }

    @Override
    public void cleanup() {
        super.cleanup();

        viewPort.removeProcessor(colorOverlayProcessor);
        background.removeAllChildren();
        screen.removeElement(background);
        screen.removeElement(scoreTextLabel);
        screen.removeElement(scoreLabel);
        screen.removeElement(accuracyTextLabel);
        screen.removeElement(accuracyLabel);
        screen.removeElement(percentTargetsHitTextLabel);
        screen.removeElement(percentTargetsHitLabel);
        screen.removeElement(trainingScoreTextLabel);
        screen.removeElement(trainingScoreLabel);
    }

    @Override
    public void update(float tpf) {
        time += tpf;
        if (time > BACKGROUND_EFFECT_TIME && !statsAdded) {
            background.addChild(scoreTextLabel);
            background.addChild(scoreLabel);
            background.addChild(accuracyTextLabel);
            background.addChild(accuracyLabel);
            background.addChild(percentTargetsHitTextLabel);
            background.addChild(percentTargetsHitLabel);
            statsAdded = true;
        }
        if (time > BACKGROUND_EFFECT_TIME + SCORE_DELAY && !scoreAdded) {
            scoreSound.playInstance();
            background.addChild(trainingScoreTextLabel);
            background.addChild(trainingScoreLabel);
            scoreAdded = true;
        }
        if (time > SCORE_TIME) {
            game.doStart();
        }
    }

    private void initSound() {
        statsSound = new AudioNode(assetManager, "Sounds/stats.wav");
        statsSound.setPositional(false);

        scoreSound = new AudioNode(assetManager, "Sounds/score.wav");
        scoreSound.setPositional(false);
    }

    private void initOverlay() {
        colorOverlayProcessor = new FilterPostProcessor(assetManager);
        ColorOverlayFilter colorOverlay = new ColorOverlayFilter(new ColorRGBA(
                OVERLAY_INTENSITY, OVERLAY_INTENSITY, OVERLAY_INTENSITY, 1));
        colorOverlayProcessor.addFilter(colorOverlay);
    }

    private void initBackground() {
        background = new Element(screen, "Score Background", Vector2f.ZERO,
                new Vector2f(settings.getWidth(), settings.getHeight()),
                Vector4f.ZERO, "Interface/score_background.png");
        backgroundEffect = new Effect(EffectType.ZoomIn, EffectEvent.Show,
                BACKGROUND_EFFECT_TIME);
        backgroundEffect.setElement(background);
    }

    private void initScoreScreen() {

        scoreTextLabel = new Label(screen, "Score Text Label", new Vector2f(0,
                settings.getHeight() / 2 - 3 * STATS_SPACING_Y), new Vector2f(
                settings.getWidth() / 2, STATS_SPACING_Y));
        scoreTextLabel.setFont("Interface/Fonts/digital.fnt");
        scoreTextLabel.setFontSize(STATS_FONT_SIZE);
        scoreTextLabel.setFontColor(STATS_COLOR);
        scoreTextLabel.setTextAlign(Align.Right);
        scoreTextLabel.setTextVAlign(VAlign.Center);
        scoreTextLabel.setText("Score:");

        scoreLabel = new Label(screen, "Score Label", new Vector2f(
                settings.getWidth() / 2 + STATS_SPACING_X, settings.getHeight()
                        / 2 - 3 * STATS_SPACING_Y), new Vector2f(
                settings.getWidth() / 2 - STATS_SPACING_X, STATS_SPACING_Y));
        scoreLabel.setFont("Interface/Fonts/digital.fnt");
        scoreLabel.setFontSize(STATS_FONT_SIZE);
        scoreLabel.setFontColor(STATS_COLOR);
        scoreLabel.setTextAlign(Align.Left);
        scoreLabel.setTextVAlign(VAlign.Center);
        scoreLabel.setText("52");

        accuracyTextLabel = new Label(
                screen,
                "Accuracy Text Label",
                new Vector2f(0, settings.getHeight() / 2 - 2 * STATS_SPACING_Y),
                new Vector2f(settings.getWidth() / 2, STATS_SPACING_Y));
        accuracyTextLabel.setFont("Interface/Fonts/digital.fnt");
        accuracyTextLabel.setFontSize(STATS_FONT_SIZE);
        accuracyTextLabel.setFontColor(STATS_COLOR);
        accuracyTextLabel.setTextAlign(Align.Right);
        accuracyTextLabel.setTextVAlign(VAlign.Center);
        accuracyTextLabel.setText("Accuracy:");

        accuracyLabel = new Label(screen, "Accuracy Label", new Vector2f(
                settings.getWidth() / 2 + STATS_SPACING_X, settings.getHeight()
                        / 2 - 2 * STATS_SPACING_Y), new Vector2f(
                settings.getWidth() / 2 - STATS_SPACING_X, STATS_SPACING_Y));
        accuracyLabel.setFont("Interface/Fonts/digital.fnt");
        accuracyLabel.setFontSize(STATS_FONT_SIZE);
        accuracyLabel.setFontColor(STATS_COLOR);
        accuracyLabel.setTextAlign(Align.Left);
        accuracyLabel.setTextVAlign(VAlign.Center);
        accuracyLabel.setText("80%");

        percentTargetsHitTextLabel = new Label(screen,
                "Hit Percentage Text Label", new Vector2f(0,
                        settings.getHeight() / 2 - STATS_SPACING_Y),
                new Vector2f(settings.getWidth() / 2, STATS_SPACING_Y));
        percentTargetsHitTextLabel.setFont("Interface/Fonts/digital.fnt");
        percentTargetsHitTextLabel.setFontSize(STATS_FONT_SIZE);
        percentTargetsHitTextLabel.setFontColor(STATS_COLOR);
        percentTargetsHitTextLabel.setTextAlign(Align.Right);
        percentTargetsHitTextLabel.setTextVAlign(VAlign.Center);
        percentTargetsHitTextLabel.setText("% Targets Hit:");

        percentTargetsHitLabel = new Label(screen, "Hit Percentage Label",
                new Vector2f(settings.getWidth() / 2 + STATS_SPACING_X,
                        settings.getHeight() / 2 - STATS_SPACING_Y),
                new Vector2f(settings.getWidth() / 2 - STATS_SPACING_X,
                        STATS_SPACING_Y));
        percentTargetsHitLabel.setFont("Interface/Fonts/digital.fnt");
        percentTargetsHitLabel.setFontSize(STATS_FONT_SIZE);
        percentTargetsHitLabel.setFontColor(STATS_COLOR);
        percentTargetsHitLabel.setTextAlign(Align.Left);
        percentTargetsHitLabel.setTextVAlign(VAlign.Center);
        percentTargetsHitLabel.setText("56%");

        trainingScoreTextLabel = new Label(screen, "Training Score Text Label",
                new Vector2f(0, settings.getHeight() / 2 + SCORE_TEXT_SPACING),
                new Vector2f(settings.getWidth(), settings.getHeight() / 6));
        trainingScoreTextLabel.setFont("Interface/Fonts/score_text.fnt");
        trainingScoreTextLabel.setFontSize(SCORE_TEXT_FONT_SIZE);
        trainingScoreTextLabel.setFontColor(SCORE_TEXT_COLOR);
        trainingScoreTextLabel.setTextAlign(Align.Center);
        trainingScoreTextLabel.setTextVAlign(VAlign.Center);
        trainingScoreTextLabel.setText("Training Score:");

        trainingScoreLabel = new Label(screen, "Training Score Label",
                new Vector2f(0, settings.getHeight() / 2 + SCORE_TEXT_SPACING
                        + SCORE_TEXT_FONT_SIZE + SCORE_SPACING), new Vector2f(
                        settings.getWidth(), settings.getHeight() / 6));
        trainingScoreLabel.setFont("Interface/Fonts/score.fnt");
        trainingScoreLabel.setFontSize(SCORE_FONT_SIZE);
        trainingScoreLabel.setFontColor(SCORE_COLOR);
        trainingScoreLabel.setTextAlign(Align.Center);
        trainingScoreLabel.setTextVAlign(VAlign.Center);
    }

    public void setStats(GameStats stats) {
        this.stats = stats;
    }

    private void updateStats() {
        scoreLabel.setText(String.format("%d", stats.getScore()));
        accuracyLabel.setText(String.format("%d%%",
                (int) (stats.getAccuracy() * 100)));
        percentTargetsHitLabel.setText(String.format("%d%%",
                (int) (stats.getPercentTargetsHit() * 100)));

        int trainingScore = (int) Math.round(Math.pow(stats.getAccuracy()
                * stats.getPercentTargetsHit(), 0.5) * 11 + 1);
        trainingScoreLabel.setText(String.format("%d", trainingScore));
    }
}
