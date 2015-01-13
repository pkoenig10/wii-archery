package game;

public class GameStats {

    private final int score;
    private final int shotsHit;
    private final int shotsTotal;
    private final int targetsHit;
    private final int targetsTotal;

    public GameStats(int score, int shotsHit, int shotsTotal, int targetsHit,
            int targetsTotal) {
        this.score = score;
        this.shotsHit = shotsHit;
        this.shotsTotal = shotsTotal;
        this.targetsHit = targetsHit;
        this.targetsTotal = targetsTotal;
    }

    public int getScore() {
        return score;
    }

    public float getAccuracy() {
        return shotsTotal != 0 ? (float) shotsHit / shotsTotal : 0;
    }

    public float getPercentTargetsHit() {
        return targetsTotal != 0 ? (float) targetsHit / targetsTotal : 0;
    }
}
