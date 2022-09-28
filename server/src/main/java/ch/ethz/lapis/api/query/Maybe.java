package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.entity.AAInsertion;
import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.NucInsertion;
import ch.ethz.lapis.api.entity.NucMutation;
import ch.ethz.lapis.api.exception.MalformedVariantQueryException;

public class Maybe extends Single {

    /**
     * The maybe operator does not do anything by itself. The maybe logic must be pushed down to the tips of the query
     * tree before calling the evalute() method.
     */
    private boolean pushedDown = false;

    @Override
    public boolean[] evaluate(Database database) {
        if (!pushedDown) {
            throw new RuntimeException("Unexpected error: The maybe operator must be pushed down before evaluating.");
        }
        return getValue().evaluate(database);
    }

    public Maybe setPushedDown(boolean pushedDown) {
        this.pushedDown = pushedDown;
        return this;
    }

    /**
     * This function transform the query and pushes down all maybe following the logic:
     * <ul>
     *     <li>maybe(maybe(A)) = maybe(A)</li>
     *     <li>maybe(!A) = !definitely(A)</li>
     *     <li>maybe(A & B) = maybe(A) & maybe(B)</li>
     *     <li>maybe(A | B) = maybe(A) | maybe(B)</li>
     *     <li>maybe([n-of: A, B, ...]) = [n-of: maybe(A), maybe(B), ...]</li>
     *     <li>
     *         exactly-n-of and insertions may <b>not</b> appear in a maybe() clause and will cause a
     *         MalformedVariantQueryException!
     *     </li>
     *     <li>
     *         ambiguity codes may <b>not</b> appear in a maybe() clause and will cause a
     *         MalformedVariantQueryException! It is not obvious what maybe(1234N) is supposed to mean.
     *     </li>
     * </ul>
     * where definitely() is the "normal" mode where unknowns are treated like false.
     * <p>
     * If there is no maybe in the query, the function won't change anything.
     */
    public static void pushDownMaybe(VariantQueryExpr expr) {
        pushDownMaybeInternal(expr, false);
    }

    private static void pushDownMaybeInternal(VariantQueryExpr expr, boolean inMaybeMode) {
        if (expr instanceof Maybe x) {
            // maybe(A) => go into maybe mode; maybe(maybe(A)) won't have an effect.
            x.setPushedDown(true);
            pushDownMaybeInternal(x.getValue(), true);
        } else if (expr instanceof Negation x) {
            pushDownMaybeInternal(x.getValue(), false);
        } else if (expr instanceof BiOp x) {
            pushDownMaybeInternal(x.getLeft(), inMaybeMode);
            pushDownMaybeInternal(x.getRight(), inMaybeMode);
        } else if (expr instanceof NOf x) {
            if (inMaybeMode && x.isExactMode()) {
                throw new MalformedVariantQueryException("exactly-n-of may not occur within a maybe().");
            }
            x.getSubExprs().forEach(e -> pushDownMaybeInternal(e, inMaybeMode));
        } else if (expr instanceof NucMutation x) {
            if (inMaybeMode && NucMutation.isAmbiguityCode(x.getMutation())) {
                throw new MalformedVariantQueryException(
                    "nucleotide ambiguity codes (N, X, Y, ...) may not occur within a maybe(). Please use the OR "
                        + "operator (\"|\") to query for different possible values."
                );
            }
            x.setApplyMaybe(true);
        } else if (expr instanceof AAMutation x) {
            if (inMaybeMode && AAMutation.isAmbiguityCode(x.getMutation())) {
                throw new MalformedVariantQueryException(
                    "AA ambiguity code (X) may not occur within a maybe(). Please use the OR "
                        + "operator (\"|\") to query for different possible values."
                );
            }
            x.setApplyMaybe(true);
        } else if (expr instanceof NucInsertion || expr instanceof AAInsertion) {
            if (inMaybeMode) {
                throw new MalformedVariantQueryException("insertions may not occur within a maybe().");
            }
        } else if (expr instanceof PangoQuery || expr instanceof GisaidClade ||expr instanceof NextstrainClade) {
          // Nothing to do
        } else {
            throw new RuntimeException("Unexpected error: unexpected instance of VariantQueryExpr: " +
                expr.getClass().getName());
        }
    }
}
