package ch.ethz.lapis.api.findaquery;

import ch.ethz.lapis.api.query.BiOp;
import ch.ethz.lapis.api.query.NOf;
import ch.ethz.lapis.api.query.QueryEngine;
import ch.ethz.lapis.api.query.QueryExpr;
import ch.ethz.lapis.api.query.Single;
import ch.ethz.lapis.api.query.VariantQueryExpr;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VariantQueryExprWithPointerTest {

    @Test
    public void testClone() throws CloneNotSupportedException {
        VariantQueryExpr originalRoot = QueryEngine.parseVariantQueryExpr(
            "BA.1 & maybe(!S:69- | C913T) & [2-of: S:501Y & E:12Y, S:484K, ORF1a:12N]");
        VariantQueryExpr originalCurrent = ((NOf) ((BiOp) ((Single) originalRoot).getValue()).getRight()).getSubExprs()
            .get(0);
        VariantQueryExprWithPointer originalWithPointer = new VariantQueryExprWithPointer(originalRoot,
            originalCurrent);
        List<QueryExpr> originalExprs = new ArrayList<>();
        originalRoot.traverseDFS(originalExprs::add);

        VariantQueryExprWithPointer clonedWithPointer = (VariantQueryExprWithPointer) originalWithPointer.clone();
        AtomicBoolean foundClonedCurrentWithinClonedQuery = new AtomicBoolean(false);
        clonedWithPointer.root().traverseDFS(expr -> {
            for (QueryExpr originalExpr : originalExprs) {
                Assertions.assertNotSame(expr, originalExpr, "An expression in the cloned query "
                    + "must not be the reference-equal to an expression in the original query.");
            }
            if (expr == clonedWithPointer.current()) {
                foundClonedCurrentWithinClonedQuery.set(true);
            }
        });
        Assertions.assertTrue(foundClonedCurrentWithinClonedQuery.get(), "The cloned current object must be "
            + "part of the cloned query.");
        Assertions.assertEquals(originalCurrent.toQueryString(), clonedWithPointer.current().toQueryString(),
            "The cloned current object must be (semantically) equal to the original one.");
    }

}
