package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.api.query.VariantQueryExpr;
import ch.ethz.lapis.util.ReferenceGenomeData;

import java.util.List;

public class AAInsertion implements VariantQueryExpr {
    private String gene;
    private int position;
    private String query;

    public AAInsertion(String gene, int position, String query) {
        this.gene  = gene;
        this.position = position;
        this.query = query;
    }

    /**
     * <p>Expected string representation: "ins_&lt;gene&gt;:&lt;position&gt;:&lt;query&gt;".</p>
     *
     * <p>Examples: "ins_S:12:EN", "ins_ORRF1a:12:?NY?". It is case insensitive.</p>
     */
    public static AAInsertion parse(String s) {
        String withoutPrefix = s.substring(4);
        String[] split = withoutPrefix.toUpperCase().split(":");
        return new AAInsertion(split[0], Integer.parseInt(split[1]), split[2]);
    }

    public String getGene() {
        return gene;
    }

    public AAInsertion setGene(String gene) {
        this.gene = gene;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public AAInsertion setPosition(int position) {
        this.position = position;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public AAInsertion setQuery(String query) {
        this.query = query;
        return this;
    }

    @Override
    public boolean[] evaluate(Database database) {
        boolean[] matched = new boolean[database.size()];
        String capitalizedGeneName = ReferenceGenomeData.getInstance().getCorrectlyCapitalizedGeneName(gene);
        List<Integer> ids = database.getAaInsertionStores().get(capitalizedGeneName)
            .find(position + ":" + query);
        for (Integer id : ids) {
            matched[id] = true;
        }
        return matched;
    }
}
