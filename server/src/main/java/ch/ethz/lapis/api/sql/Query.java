package ch.ethz.lapis.api.sql;

import lombok.Data;
import net.sf.jsqlparser.expression.Expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Query implements Cloneable {

    private String table;
    private Map<String, String> aliasToExpression = new HashMap<>();
    private List<String> selectExpressions = new ArrayList<>();
    private Expression whereExpression;
    private List<String> groupByColumns = new ArrayList<>();
    private Expression havingExpression;
    private String orderByExpression;
    private boolean orderByAsc = true;
    private Integer offset;
    private Integer limit;

    @Override
    protected Query clone() {
        try {
            return (Query) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
