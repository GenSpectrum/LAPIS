package ch.ethz.lapis.api.query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class NOf implements VariantQueryExpr {

    private final boolean exactMode;
    private final int n;
    private final List<VariantQueryExpr> subExprs = new ArrayList<>();

    public NOf(boolean exactMode, int n) {
        this.exactMode = exactMode;
        this.n = n;
    }


    @Override
    public void putValue(VariantQueryExpr value) {
        subExprs.add(value);
    }

    @Override
    public boolean[] evaluate(Database database) {
        if (subExprs.size() > Short.MAX_VALUE) {
            throw new RuntimeException("More than " + Short.MAX_VALUE + " in the n-of statement - seriously? Why??");
        }
        short[] trueCount = new short[database.size()];
        for (VariantQueryExpr subExpr : subExprs) {
            boolean[] subResult = subExpr.evaluate(database);
            for (int i = 0; i < subResult.length; i++) {
                if (subResult[i]) {
                    trueCount[i]++;
                }
            }
        }
        boolean[] result = new boolean[database.size()];
        if (!exactMode) {
            for (int i = 0; i < trueCount.length; i++) {
                result[i] = trueCount[i] >= n;
            }
        } else {
            for (int i = 0; i < trueCount.length; i++) {
                result[i] = trueCount[i] == n;
            }
        }
        return result;
    }

    public boolean isExactMode() {
        return exactMode;
    }

    public int getN() {
        return n;
    }

    public List<VariantQueryExpr> getSubExprs() {
        return subExprs;
    }

    @Override
    public String toString() {
        return "NOf{" +
            "exactMode=" + exactMode +
            ", n=" + n +
            ", subExprs=" + subExprs +
            '}';
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        NOf o = new NOf(exactMode, n);
        subExprs.forEach(o::putValue);
        return o;
    }

    @Override
    public void traverseDFS(Consumer<QueryExpr> callback) {
        callback.accept(this);
        subExprs.forEach(o -> o.traverseDFS(callback));
    }

    @Override
    public String toQueryString() {
        String s = "[";
        if (exactMode) {
            s += "exactly-";
        }
        s += n
            + "-of: "
            + subExprs.stream().map(VariantQueryExpr::toQueryString).collect(Collectors.joining(", "))
            + "]";
        return s;
    }
}
