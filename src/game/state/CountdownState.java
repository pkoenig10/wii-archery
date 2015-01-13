package game.state;

import game.Game;
import tonegod.gui.controls.text.Label;
import tonegod.gui.core.Screen;
import tonegod.gui.effects.Effect;
import tonegod.gui.effects.Effect.EffectEvent;
import tonegod.gui.effects.Effect.EffectType;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.audio.AudioNode;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;

public class CountdownState extends AbstractAppState {

    private final Game game;
    private final GameRunningState gameRunningState;
    private final Screen screen;
    private final AppStateManager stateManager;
    private final AppSettings settings;

    private final int countdownCount;
    private final float countdownFontSize;
    private final ColorRGBA countdownFontColor;
    private final float countdownEffectTime;
    private final AudioNode countdownSound;

    private Label countdownLabel;

    private float time;
    private int count;

    public CountdownState(Game game, GameRunningState gameRunningState,
            int countdownCount, float countdownFontSize,
            ColorRGBA countdownFontColor, float countdownEffectTime,
            AudioNode countdownSound) {
        this.game = game;
        this.gameRunningState = gameRunningState;
        this.screen = game.getScreen();
        this.stateManager = game.getStateManager();
        this.settings = game.getContext().getSettings();

        this.countdownCount = countdownCount;
        this.countdownFontSize = countdownFontSize;
        this.countdownFontColor = countdownFontColor;
        this.countdownEffectTime = countdownEffectTime;
        this.countdownSound = countdownSound;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        time = 0;
        count = countdownCount;

        initCountdown();
    }

    @Override
    public void update(float tpf) {
        time += tpf;
        if (time >= 1) {
            count--;
            screen.removeElement(countdownLabel);
            if (count != 0) {
                initCountdown();
            } else {
                gameRunningState.countdownEnded();
                stateManager.detach(this);
            }
            time -= 1;
        }
    }

    private void initCountdown() {
        countdownLabel = new Label(screen, Vector2f.ZERO, new Vector2f(
                settings.getWidth(), settings.getHeight()));
        countdownLabel.setFont("Interface/Fonts/digital.fnt");
        countdownLabel.setFontSize(countdownFontSize);
        countdownLabel.setFontColor(countdownFontColor);
        countdownLabel.setTextAlign(Align.Center);
        countdownLabel.setTextVAlign(VAlign.Center);
        countdownLabel.setText(String.format("%d", count));
        screen.addElement(countdownLabel);

        Effect countdownEffect = new Effect(EffectType.ZoomOut,
                EffectEvent.Show, countdownEffectTime);
        countdownEffect.setElement(countdownLabel);
        screen.getEffectManager().applyEffect(countdownEffect);

        if (countdownSound != null) {
            countdownSound.playInstance();
        }
    }

}
