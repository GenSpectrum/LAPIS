package ch.ethz.lapis.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class StopWatch {

    public static class Round {
        private final String label;
        private final long durationInMs;

        public Round(String label, long durationInMs) {
            this.label = label;
            this.durationInMs = durationInMs;
        }

        public String getLabel() {
            return label;
        }

        public long getDurationInMs() {
            return durationInMs;
        }
    }

    private List<Round> finishedRounds;
    private boolean running;
    private String currentRoundLabel;
    private Instant currentRoundStart;

    public StopWatch() {
        reset();
    }

    public void start(String label) {
        running = true;
        currentRoundLabel = label;
        currentRoundStart = Instant.now();
    }

    public void round(String label) {
        if (!running) {
            throw new RuntimeException("Please start the StopWatch first.");
        }
        Instant current = Instant.now();
        finishedRounds.add(new Round(currentRoundLabel, Duration.between(currentRoundStart, current).toMillis()));
        currentRoundLabel = label;
        currentRoundStart = Instant.now();
    }

    public void stop() {
        if (!running) {
            throw new RuntimeException("Please start the StopWatch first.");
        }
        Instant current = Instant.now();
        finishedRounds.add(new Round(currentRoundLabel, Duration.between(currentRoundStart, current).toMillis()));
        running = false;
    }

    public void reset() {
        finishedRounds = new ArrayList<>();
        running = false;
        currentRoundLabel = null;
        currentRoundStart = null;
    }

    public List<Round> getResults() {
        return finishedRounds;
    }

    public String getFormattedResultString() {
        return resultsToFormattedString(finishedRounds);
    }

    public static String resultsToFormattedString(List<Round> results) {
        long total = 0;
        List<String> stringParts = new ArrayList<>();
        for (Round round : results) {
            total += round.getDurationInMs();
            stringParts.add(round.getLabel() + "=" + round.getDurationInMs() + "ms");
        }
        stringParts.add("total=" + total + "ms");
        return String.join(", ", stringParts);
    }
}
