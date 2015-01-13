package game;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import motej.Mote;
import motej.MoteFinder;
import motej.MoteFinderListener;

public class GameMoteFinder implements MoteFinderListener {

    private static final long FIND_MOTE_TIME = 30000;

    private final MoteFinder finder;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private Mote mote;

    public GameMoteFinder() {
        finder = MoteFinder.getMoteFinder();
        finder.addMoteFinderListener(this);
    }

    public Mote findMote() throws InterruptedException {
        lock.lock();

        try {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < FIND_MOTE_TIME) {
                System.out.println("Starting disc");
                finder.startDiscovery();
                condition.await(15, TimeUnit.SECONDS);
                finder.stopDiscovery();
                if (mote != null) {
                    break;
                }
                Thread.sleep(1000);
            }
        } finally {
            finder.stopDiscovery();
            lock.unlock();
        }

        return mote;
    }

    @Override
    public void moteFound(Mote mote) {
        lock.lock();

        try {
            this.mote = mote;
            mote.setPlayerLeds(new boolean[] { true, false, false, false });
        } finally {
            condition.signalAll();
            lock.unlock();
        }
    }
}
