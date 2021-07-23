package ch.ethz.y.api;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;

public class JooqHelper {
    public static DSLContext getDSLCtx(Connection connection) {
        return DSL.using(connection, SQLDialect.POSTGRES);
    }
}
