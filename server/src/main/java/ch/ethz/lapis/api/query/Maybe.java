package ch.ethz.lapis.api.query;

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
     *     <li>maybe(!A) = !definitely(A)</li>
     *     <li>maybe(A & B) = maybe(A) & maybe(B)</li>
     *     <li>maybe(A | B) = maybe(A) | maybe(B)</li>
     *     <li>maybe([n-of: A, B, ...]) = [n-of: maybe(A), maybe(B), ...]</li>
     *     <li>maybe(maybe(A)) = maybe(A)</li>
     *     <li>
     *         exactly-n-of and insertions may <b>not</b> appear in a maybe() clause and will cause a
     *         MalformedVariantQueryException!
     *     </li>
     * </ul>
     * where definitely() is the "normal" mode where unknowns are treated like false.
     */
    public static void pushDownMaybe(VariantQueryExpr expr) {
        // TODO
    }
}
