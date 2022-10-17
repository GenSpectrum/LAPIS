package ch.ethz.lapis.api.query;

import java.util.function.Consumer;

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
    public boolean[] evaluate(Database database) {
        boolean[] leftEvaluated = left.evaluate(database);
        boolean[] rightEvaluated = right.evaluate(database);
        boolean[] result = new boolean[leftEvaluated.length];
        if (opType == OpType.AND) {
            for (int i = 0; i < result.length; i++) {
                result[i] = leftEvaluated[i] && rightEvaluated[i];
            }
        } else if (opType == OpType.OR) {
            for (int i = 0; i < result.length; i++) {
                result[i] = leftEvaluated[i] || rightEvaluated[i];
            }
        }
        return result;
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

    @Override
    public Object clone() throws CloneNotSupportedException {
        BiOp o = (BiOp) super.clone();
        o.left = (VariantQueryExpr) o.left.clone();
        o.right = (VariantQueryExpr) o.right.clone();
        return o;
    }

    @Override
    public void traverseDFS(Consumer<QueryExpr> callback) {
        callback.accept(this);
        left.traverseDFS(callback);
        right.traverseDFS(callback);
    }
}
