package ch.ethz.lapis.api.query;

import java.util.BitSet;

public class BiOp implements VariantQueryExpr {

    public enum OpType {
        AND,
        OR
    }

    private final OpType opType;

    private VariantQueryExpr left;

    private VariantQueryExpr right;

    public BiOp(OpType opType) {
        this.opType = opType;
    }

    @Override
    public void putValue(VariantQueryExpr value) {
        if (left == null) {
            left = value;
        } else {
            right = value;
        }
    }

    @Override
    public BitSet evaluate(Database database) {
        BitSet leftEvaluated = left.evaluate(database);
        BitSet rightEvaluated = right.evaluate(database);
        if (opType == OpType.AND) {
            leftEvaluated.and(rightEvaluated);
        } else if (opType == OpType.OR) {
            leftEvaluated.or(rightEvaluated);
        }
        return leftEvaluated;
    }

    public OpType getOpType() {
        return opType;
    }

    public VariantQueryExpr getLeft() {
        return left;
    }

    public VariantQueryExpr getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "BiOp{" +
            "opType=" + opType +
            ", left=" + left +
            ", right=" + right +
            '}';
    }
}
