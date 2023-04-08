package ch.ethz.lapis.api.query;

public class BiOp implements QueryExpr {

    public enum OpType {
        AND,
        OR
    }

    private final OpType opType;

    private QueryExpr left;

    private QueryExpr right;

    public BiOp(OpType opType) {
        this.opType = opType;
    }

    @Override
    public void putValue(QueryExpr value) {
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

    public QueryExpr getLeft() {
        return left;
    }

    public QueryExpr getRight() {
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
