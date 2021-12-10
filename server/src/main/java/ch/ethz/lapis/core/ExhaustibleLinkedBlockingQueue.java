package ch.ethz.lapis.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class ExhaustibleLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> implements ExhaustibleBlockingQueue<E> {

    private boolean exhausted = false;


    public ExhaustibleLinkedBlockingQueue() {
    }


    public ExhaustibleLinkedBlockingQueue(int capacity) {
        super(capacity);
    }


    public ExhaustibleLinkedBlockingQueue(Collection<? extends E> c) {
        super(c);
    }


    @Override
    public boolean isExhausted() {
        return exhausted;
    }


    @Override
    public void setExhausted(boolean exhausted) {
        this.exhausted = exhausted;
    }


    /**
     * Takes a queue of elements and batch them in a new thread.
     */
    public static <E> ExhaustibleBlockingQueue<List<E>> batchElements(
        ExhaustibleBlockingQueue<E> inputQueue,
        int batchSize
    ) {
        return batchElements(inputQueue, batchSize, new ExhaustibleLinkedBlockingQueue<>());
    }


    /**
     * Takes a queue of elements and batch them in a new thread.
     */
    public static <E> ExhaustibleBlockingQueue<List<E>> batchElements(
        ExhaustibleBlockingQueue<E> inputQueue,
        int batchSize,
        int batchQueueCapacity
    ) {
        return batchElements(inputQueue, batchSize, new ExhaustibleLinkedBlockingQueue<>(batchQueueCapacity));
    }


    private static <E> ExhaustibleBlockingQueue<List<E>> batchElements(
        ExhaustibleBlockingQueue<E> inputQueue,
        int batchSize,
        ExhaustibleBlockingQueue<List<E>> outputQueue
    ) {
        new Thread(() -> {
            try {
                List<E> batch = new ArrayList<>();
                while (!inputQueue.isEmpty() || !inputQueue.isExhausted()) {
                    E el = inputQueue.poll(3, TimeUnit.SECONDS);
                    if (el == null) {
                        continue;
                    }
                    batch.add(el);
                    if (batch.size() >= batchSize) {
                        outputQueue.offer(batch, 365, TimeUnit.DAYS);
                        batch = new ArrayList<>();
                    }
                }
                if (!batch.isEmpty()) {
                    outputQueue.offer(batch, 365, TimeUnit.DAYS);
                }
                outputQueue.setExhausted(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return outputQueue;
    }
}
