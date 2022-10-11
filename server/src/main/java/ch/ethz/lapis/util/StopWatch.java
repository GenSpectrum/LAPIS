package ch.ethz.lapis.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class StopWatch {

    public record Round(String label, long durationInMs) {
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
            total += round.durationInMs();
            stringParts.add(round.label() + "=" + round.durationInMs() + "ms");
        }
        stringParts.add("total=" + total + "ms");
        return String.join(", ", stringParts);
    }
}
