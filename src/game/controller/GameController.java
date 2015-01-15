package game.controller;

import game.Game;

import java.util.concurrent.Callable;

import motej.Mote;
import motej.event.AccelerometerEvent;
import motej.event.AccelerometerListener;
import motej.event.CoreButtonEvent;
import motej.event.CoreButtonListener;
import motej.request.ReportModeRequest;
import motejx.extensions.motionplus.MotionPlusEvent;
import motejx.extensions.motionplus.MotionPlusListener;
import motejx.extensions.motionplusnunchuk.MotionPlusNunchuk;
import motejx.extensions.nunchuk.Nunchuk;
import motejx.extensions.nunchuk.NunchukButtonEvent;
import motejx.extensions.nunchuk.NunchukButtonListener;

public class GameController implements CoreButtonListener,
        NunchukButtonListener, AccelerometerListener<Nunchuk>,
        MotionPlusListener {

    private static final int DRAWING_ACCELERATION = 140;
    private static final int READY_ACCELERATION = 130;

    private static final long READY_TIMEOUT = 1000;

    private final Game game;

    private Mote mote;
    private MotionPlusNunchuk motionPlusNunchuk;

    private boolean crosshairReset = false;
    private ShootState shootState = ShootState.NOT_SHOOTING;
    private double rollVal = 0;
    private double pitchVal = 0;
    private long lastLoadTime = 0;
    private MotionPlusEvent lastMotionPlusEvent;

    public GameController(Game game, Mote mote,
            MotionPlusNunchuk motionPlusNunchuk) {
        this.game = game;
        this.mote = mote;
        this.motionPlusNunchuk = motionPlusNunchuk;

        mote.addCoreButtonListener(this);
        motionPlusNunchuk.addMotionPlusEventListener(this);
        motionPlusNunchuk.addAccelerometerListener(this);
        motionPlusNunchuk.addNunchukButtonListener(this);
        mote.setReportMode(ReportModeRequest.DATA_REPORT_0x32);
    }

    public double getRollVal() {
        return rollVal;
    }

    public double getPitchVal() {
        return pitchVal;
    }

    private void updateShootState(ShootState shootState) {
        this.shootState = shootState;
        game.updateCrosshair(shootState);
    }

    @Override
    public void buttonPressed(CoreButtonEvent evt) {
        if (evt.isButtonAPressed()) {
            if (!crosshairReset) {
                crosshairReset = true;
                mote.rumble(100);
                rollVal = 0;
                pitchVal = 0;
            }
        } else {
            crosshairReset = false;
        }
    }

    @Override
    public void speedChanged(MotionPlusEvent evt) {
        if (lastMotionPlusEvent != null) {
            double duration = (double) (evt.getEventTime() - lastMotionPlusEvent
                    .getEventTime()) / 1000;
            rollVal += evt.getRollLeftSpeed() * duration;
            pitchVal += evt.getPitchDownSpeed() * duration;
        }

        lastMotionPlusEvent = evt;
    }

    @Override
    public void accelerometerChanged(AccelerometerEvent<Nunchuk> evt) {
        if (shootState == ShootState.LOADING
                && evt.getY() > DRAWING_ACCELERATION) {
            updateShootState(ShootState.DRAWING);
        }
        if (shootState == ShootState.DRAWING && evt.getY() < READY_ACCELERATION) {
            updateShootState(ShootState.READY);
        }
        if ((shootState == ShootState.LOADING || shootState == ShootState.DRAWING)
                && System.currentTimeMillis() - lastLoadTime > READY_TIMEOUT) {
            updateShootState(ShootState.READY);
        }
    }

    @Override
    public void buttonPressed(NunchukButtonEvent evt) {
        if (evt.isButtonCPressed() || evt.isButtonZPressed()) {
            if (shootState == ShootState.NOT_SHOOTING) {
                updateShootState(ShootState.LOADING);
                lastLoadTime = System.currentTimeMillis();
            }
        } else {
            if (shootState == ShootState.READY) {
                game.enqueue(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        game.shoot();
                        return null;
                    }
                });
            }
            updateShootState(ShootState.NOT_SHOOTING);
        }
    }
}
