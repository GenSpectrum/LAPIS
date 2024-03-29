/*
 * This file is generated by jOOQ.
 */
package org.jooq.lapis.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.lapis.tables.YTree;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class YTreeRecord extends UpdatableRecordImpl<YTreeRecord> implements Record2<Long, byte[]> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>y_tree.timestamp</code>.
     */
    public void setTimestamp(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>y_tree.timestamp</code>.
     */
    public Long getTimestamp() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>y_tree.bytes</code>.
     */
    public void setBytes(byte[] value) {
        set(1, value);
    }

    /**
     * Getter for <code>y_tree.bytes</code>.
     */
    public byte[] getBytes() {
        return (byte[]) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Long, byte[]> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Long, byte[]> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return YTree.Y_TREE.TIMESTAMP;
    }

    @Override
    public Field<byte[]> field2() {
        return YTree.Y_TREE.BYTES;
    }

    @Override
    public Long component1() {
        return getTimestamp();
    }

    @Override
    public byte[] component2() {
        return getBytes();
    }

    @Override
    public Long value1() {
        return getTimestamp();
    }

    @Override
    public byte[] value2() {
        return getBytes();
    }

    @Override
    public YTreeRecord value1(Long value) {
        setTimestamp(value);
        return this;
    }

    @Override
    public YTreeRecord value2(byte[] value) {
        setBytes(value);
        return this;
    }

    @Override
    public YTreeRecord values(Long value1, byte[] value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached YTreeRecord
     */
    public YTreeRecord() {
        super(YTree.Y_TREE);
    }

    /**
     * Create a detached, initialised YTreeRecord
     */
    public YTreeRecord(Long timestamp, byte[] bytes) {
        super(YTree.Y_TREE);

        setTimestamp(timestamp);
        setBytes(bytes);
    }
}
