package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.api.query.VariantQueryExpr;

import java.util.List;

public class NucInsertion implements VariantQueryExpr {
    private int position;
    private String query;

    public NucInsertion(int position, String query) {
        this.position = position;
        this.query = query;
    }

    /**
     * <p>Expected string representation: "ins_&lt;position&gt;:&lt;query&gt;".</p>
     *
     * <p>Examples: "ins_1234:ACT?GGT", "ins_2345:?AAT?". It is case insensitive.</p>
     */
    public static NucInsertion parse(String s) {
        String withoutPrefix = s.substring(4);
        String[] split = withoutPrefix.toUpperCase().split(":");
        return new NucInsertion(Integer.parseInt(split[0]), split[1]);
    }

    public int getPosition() {
        return position;
    }

    public NucInsertion setPosition(int position) {
        this.position = position;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public NucInsertion setQuery(String query) {
        this.query = query;
        return this;
    }

    @Override
    public boolean[] evaluate(Database database) {
        boolean[] matched = new boolean[database.size()];
        List<Integer> ids = database.getNucInsertionStore().find(position + ":" + query);
        for (Integer id : ids) {
            matched[id] = true;
        }
        return matched;
    }
}
