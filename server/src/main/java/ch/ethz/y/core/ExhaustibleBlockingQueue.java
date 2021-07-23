package ch.ethz.y.core;

import java.util.concurrent.BlockingQueue;


/**
 * ...and it's back! It's at least the fourth project that I use this small piece of code :) (Chaoran)
 */
public interface ExhaustibleBlockingQueue<E> extends BlockingQueue<E> {
    boolean isExhausted();
    void setExhausted(boolean exhausted);
}
