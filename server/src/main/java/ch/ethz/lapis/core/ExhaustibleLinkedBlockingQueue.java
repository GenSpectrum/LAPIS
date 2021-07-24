package ch.ethz.lapis.core;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;


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
}
