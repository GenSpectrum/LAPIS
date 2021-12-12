package ch.ethz.lapis.transform;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * Given a list of k strings of length n (aligned sequences), this class produces n strings of length k. The
 * first string contains the first character of the input strings, etc.
 *
 * Example:
 *   Input: ["AAT", "ATT"]
 *   Output: ["AA", "AT", "TT"]
 */
public class SequenceRowToColumnTransformer {

    private final int numberWorkers;

    /**
     * This value defines how many positions should be processed at the same time, i.e., how many strings of length k
     * has to be hold in memory. This is the core value to balance the needed RAM, CPU and wall-clock time.
     */
    private final int positionRangeSize;

    /*
     * This is the number of sequences that a worker will process per batch. It usually does not have a large effect on
     * the performance.
     */
    private final int batchSize = 20000;

    public SequenceRowToColumnTransformer(int numberWorkers, int positionRangeSize) {
        this.numberWorkers = numberWorkers;
        this.positionRangeSize = positionRangeSize;
    }

    /**
     *
     * @param compressedSequences The compressed sequences
     * @param decompressor A function to decompress a sequence
     * @param consumer A function that takes the position of the first entry (index starts with 1) in the result set as
     *                 the first argument and a list of transformed and compressed columnar strings as the second
     *                 argument.
     * @param compressor A function to compress the transformed string
     */
    public <S, T> void transform(
        List<S> compressedSequences,
        Function<S, String> decompressor,
        BiConsumer<Integer, List<T>> consumer,
        Function<String, T> compressor,
        char unknownCode
    ) {
        try {
            if (compressedSequences.isEmpty()) {
                return;
            }
            int sequenceLength = decompressor.apply(compressedSequences.get(0)).length();
            int numberIterations = (int) Math.ceil(sequenceLength * 1.0 / positionRangeSize);
            int numberTasksPerIteration = (int) Math.ceil(compressedSequences.size() * 1.0 / batchSize);
            ExecutorService executor = Executors.newFixedThreadPool(numberWorkers);

            for (int iteration = 0; iteration < numberIterations; iteration++) {
                final int startPos = positionRangeSize * iteration;
                final int endPos = Math.min(positionRangeSize * (iteration + 1), sequenceLength);
                int countPos = endPos - startPos;
                char[][] transformedData = new char[countPos][compressedSequences.size()];
                System.out.println(LocalDateTime.now() + " Position " + startPos + " - " + endPos);

                List<Callable<List<Void>>> tasks = new ArrayList<>();
                for (int taskIndex = 0; taskIndex < numberTasksPerIteration; taskIndex++) {
                    final int startSeq = batchSize * taskIndex;
                    final int endSeq = Math.min(batchSize * (taskIndex + 1), compressedSequences.size());

                    tasks.add(() -> {
                        try {
                            System.out.println(
                                LocalDateTime.now() + "     Sequences " + startSeq + " - " + endSeq + " - Start");
                            for (int seqIndex = startSeq; seqIndex < endSeq; seqIndex++) {
                                S compressed = compressedSequences.get(seqIndex);
                                if (compressed == null) {
                                    for (int i = startPos; i < endPos; i++) {
                                        transformedData[i - startPos][seqIndex] = unknownCode;
                                    }
                                } else {
                                    String decompressed = decompressor.apply(compressed);
                                    char[] seq = decompressed.toCharArray();
                                    for (int i = startPos; i < endPos; i++) {
                                        transformedData[i - startPos][seqIndex] = seq[i];
                                    }
                                }
                            }
                            System.out.println(LocalDateTime.now() + "     Sequences " + startSeq + " - " + endSeq + " - End");
                            return null;
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw e;
                        }
                    });
                }

                executor.invokeAll(tasks)
                    .forEach(f -> {
                        try {
                            f.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });

                // Transform char arrays to string, compress them and insert
                // This will be done in parallel again.
                int finalizationBatchSize = 20;
                int numberFinalizationTasks = (int) Math.ceil(countPos * 1.0 / finalizationBatchSize);
                List<Callable<Void>> tasks2 = new ArrayList<>();
                for (int finalizationIndex = 0; finalizationIndex < numberFinalizationTasks; finalizationIndex++) {
                    final int finalizationPosStart = startPos + finalizationBatchSize * finalizationIndex;
                    final int finalizationPosEnd = Math.min(startPos + finalizationBatchSize * (finalizationIndex + 1),
                        endPos);

                    tasks2.add(() -> {
                        System.out.println(LocalDateTime.now() + "     Start compressing and inserting " +
                            finalizationPosStart + " - " + finalizationPosEnd);
                        List<T> results = new ArrayList<>();
                        for (int posIndex = finalizationPosStart; posIndex < finalizationPosEnd; posIndex++) {
                            String transformed = String.valueOf(transformedData[posIndex - startPos]);
                            T compressed = compressor.apply(transformed);
                            results.add(compressed);
                        }
                        consumer.accept(finalizationPosStart + 1, results);
                        return null;
                    });
                }
                List<Future<Void>> futures = executor.invokeAll(tasks2);
                try {
                    for (Future<Void> future : futures) {
                        future.get();
                    }
                } catch (ExecutionException e) {
                    executor.shutdown();
                    executor.awaitTermination(3, TimeUnit.MINUTES);
                    throw new RuntimeException(e);
                }
            }
            executor.shutdown();
            executor.awaitTermination(3, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
