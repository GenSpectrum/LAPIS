package ch.ethz.lapis.api.findaquery;

import ch.ethz.lapis.api.query.VariantQueryExpr;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @param root Points to the root of the query
 * @param current Points to a node within the query
 */
public record VariantQueryExprWithPointer(VariantQueryExpr root, VariantQueryExpr current) implements Cloneable {

    public Object clone() throws CloneNotSupportedException {
        VariantQueryExpr newRoot = (VariantQueryExpr) root.clone();

        // We need to find the node within "newRoot" that matches to "current". To do that, we will traverse through
        // the old "root" and count the number of steps until we reach "current". We will then traverse through
        // "newRoot" in the same way and go the same number of steps.
        AtomicInteger steps = new AtomicInteger();
        AtomicBoolean found = new AtomicBoolean(false);
        root.traverseDFS((e) -> {
            if (found.get()) {
                return;
            }
            if (e == current) { // Yes, we are looking for reference equality here!
                found.set(true);
            }
            steps.getAndIncrement();
        });
        if (!found.get()) {
            throw new RuntimeException("Unexpected error: the VariantQueryExprWithPointer is broken "
                + "because 'current' is not a node within 'root'!");
        }
        AtomicInteger steps2 = new AtomicInteger();
        AtomicReference<VariantQueryExpr> newCurrent = new AtomicReference<>(null);
        newRoot.traverseDFS((e) -> {
            if (steps2.get() == steps.get()) {
                newCurrent.set((VariantQueryExpr) e);
            }
        });
        if (newCurrent.get() == null) {
            throw new RuntimeException("Unexpected error: newCurrent wasn't found.");
        }
        return new VariantQueryExprWithPointer(newRoot, newCurrent.get());
    }

}
