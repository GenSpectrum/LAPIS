package ch.ethz.lapis.api.entity.query;

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
