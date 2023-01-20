package ch.ethz.lapis.core;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * ...and it's back! It's at least the fourth project that I use this small piece of code :) (Chaoran)
 */
@Slf4j
public class DatabaseReaderQueueBuilder<T> {

    private final PreparedStatement statement;
    private final Function<ResultSet, T> convertFunction;
    private final int capacity;
    private final int fetchSize;

    /**
     * @param statement       The connection of "statement" must not be used by any other object.
     * @param convertFunction "rowToJobFunction" must not call next() on the result set or manipulate it in any other
     *                        way.
     * @param capacity        The maximal capacity of the queue
     * @param fetchSize       Please see java.sql.Statement.setFetchSize()
     */
    public DatabaseReaderQueueBuilder(
        PreparedStatement statement,
        Function<ResultSet, T> convertFunction,
        int capacity,
        int fetchSize
    ){
        this.statement = statement;
        this.convertFunction = convertFunction;
        this.capacity = capacity;
        this.fetchSize = fetchSize;
    }


    public ExhaustibleBlockingQueue<T> build() throws SQLException {
        ExhaustibleBlockingQueue<T> queue = new ExhaustibleLinkedBlockingQueue<>(this.capacity);
        Connection conn = this.statement.getConnection();
        conn.setAutoCommit(false);
        statement.setFetchSize(this.fetchSize);
        Thread fetcherThread = new Thread(() -> {
            try (ResultSet rs = statement.executeQuery()) {
                log.info("DatabaseReaderQueueBuilder: Start fetching");
                while (rs.next()) {
                    T job = this.convertFunction.apply(rs);
                    queue.put(job);
                }
                queue.setExhausted(true);
                log.info("DatabaseReaderQueueBuilder: Finished");
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (InterruptedException ignored) {
            } finally {
                try {
                    conn.setAutoCommit(true);
                    statement.close();
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        });
        fetcherThread.start();
        return queue;
    }
}
