/*
 * This file is generated by jOOQ.
 */
package org.jooq.lapis.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.lapis.tables.YMainSequenceStaging;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class YMainSequenceStagingRecord extends UpdatableRecordImpl<YMainSequenceStagingRecord> implements Record10<Integer, byte[], byte[], String, String, String, String, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>y_main_sequence_staging.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>y_main_sequence_staging.seq_original_compressed</code>.
     */
    public void setSeqOriginalCompressed(byte[] value) {
        set(1, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.seq_original_compressed</code>.
     */
    public byte[] getSeqOriginalCompressed() {
        return (byte[]) get(1);
    }

    /**
     * Setter for <code>y_main_sequence_staging.seq_aligned_compressed</code>.
     */
    public void setSeqAlignedCompressed(byte[] value) {
        set(2, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.seq_aligned_compressed</code>.
     */
    public byte[] getSeqAlignedCompressed() {
        return (byte[]) get(2);
    }

    /**
     * Setter for <code>y_main_sequence_staging.aa_mutations</code>.
     */
    public void setAaMutations(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.aa_mutations</code>.
     */
    public String getAaMutations() {
        return (String) get(3);
    }

    /**
     * Setter for <code>y_main_sequence_staging.aa_unknowns</code>.
     */
    public void setAaUnknowns(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.aa_unknowns</code>.
     */
    public String getAaUnknowns() {
        return (String) get(4);
    }

    /**
     * Setter for <code>y_main_sequence_staging.nuc_substitutions</code>.
     */
    public void setNucSubstitutions(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.nuc_substitutions</code>.
     */
    public String getNucSubstitutions() {
        return (String) get(5);
    }

    /**
     * Setter for <code>y_main_sequence_staging.nuc_deletions</code>.
     */
    public void setNucDeletions(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.nuc_deletions</code>.
     */
    public String getNucDeletions() {
        return (String) get(6);
    }

    /**
     * Setter for <code>y_main_sequence_staging.nuc_insertions</code>.
     */
    public void setNucInsertions(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.nuc_insertions</code>.
     */
    public String getNucInsertions() {
        return (String) get(7);
    }

    /**
     * Setter for <code>y_main_sequence_staging.nuc_unknowns</code>.
     */
    public void setNucUnknowns(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.nuc_unknowns</code>.
     */
    public String getNucUnknowns() {
        return (String) get(8);
    }

    /**
     * Setter for <code>y_main_sequence_staging.aa_insertions</code>.
     */
    public void setAaInsertions(String value) {
        set(9, value);
    }

    /**
     * Getter for <code>y_main_sequence_staging.aa_insertions</code>.
     */
    public String getAaInsertions() {
        return (String) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, byte[], byte[], String, String, String, String, String, String, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<Integer, byte[], byte[], String, String, String, String, String, String, String> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.ID;
    }

    @Override
    public Field<byte[]> field2() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.SEQ_ORIGINAL_COMPRESSED;
    }

    @Override
    public Field<byte[]> field3() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.SEQ_ALIGNED_COMPRESSED;
    }

    @Override
    public Field<String> field4() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.AA_MUTATIONS;
    }

    @Override
    public Field<String> field5() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.AA_UNKNOWNS;
    }

    @Override
    public Field<String> field6() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.NUC_SUBSTITUTIONS;
    }

    @Override
    public Field<String> field7() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.NUC_DELETIONS;
    }

    @Override
    public Field<String> field8() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.NUC_INSERTIONS;
    }

    @Override
    public Field<String> field9() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.NUC_UNKNOWNS;
    }

    @Override
    public Field<String> field10() {
        return YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.AA_INSERTIONS;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public byte[] component2() {
        return getSeqOriginalCompressed();
    }

    @Override
    public byte[] component3() {
        return getSeqAlignedCompressed();
    }

    @Override
    public String component4() {
        return getAaMutations();
    }

    @Override
    public String component5() {
        return getAaUnknowns();
    }

    @Override
    public String component6() {
        return getNucSubstitutions();
    }

    @Override
    public String component7() {
        return getNucDeletions();
    }

    @Override
    public String component8() {
        return getNucInsertions();
    }

    @Override
    public String component9() {
        return getNucUnknowns();
    }

    @Override
    public String component10() {
        return getAaInsertions();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public byte[] value2() {
        return getSeqOriginalCompressed();
    }

    @Override
    public byte[] value3() {
        return getSeqAlignedCompressed();
    }

    @Override
    public String value4() {
        return getAaMutations();
    }

    @Override
    public String value5() {
        return getAaUnknowns();
    }

    @Override
    public String value6() {
        return getNucSubstitutions();
    }

    @Override
    public String value7() {
        return getNucDeletions();
    }

    @Override
    public String value8() {
        return getNucInsertions();
    }

    @Override
    public String value9() {
        return getNucUnknowns();
    }

    @Override
    public String value10() {
        return getAaInsertions();
    }

    @Override
    public YMainSequenceStagingRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value2(byte[] value) {
        setSeqOriginalCompressed(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value3(byte[] value) {
        setSeqAlignedCompressed(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value4(String value) {
        setAaMutations(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value5(String value) {
        setAaUnknowns(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value6(String value) {
        setNucSubstitutions(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value7(String value) {
        setNucDeletions(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value8(String value) {
        setNucInsertions(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value9(String value) {
        setNucUnknowns(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord value10(String value) {
        setAaInsertions(value);
        return this;
    }

    @Override
    public YMainSequenceStagingRecord values(Integer value1, byte[] value2, byte[] value3, String value4, String value5, String value6, String value7, String value8, String value9, String value10) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached YMainSequenceStagingRecord
     */
    public YMainSequenceStagingRecord() {
        super(YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING);
    }

    /**
     * Create a detached, initialised YMainSequenceStagingRecord
     */
    public YMainSequenceStagingRecord(Integer id, byte[] seqOriginalCompressed, byte[] seqAlignedCompressed, String aaMutations, String aaUnknowns, String nucSubstitutions, String nucDeletions, String nucInsertions, String nucUnknowns, String aaInsertions) {
        super(YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING);

        setId(id);
        setSeqOriginalCompressed(seqOriginalCompressed);
        setSeqAlignedCompressed(seqAlignedCompressed);
        setAaMutations(aaMutations);
        setAaUnknowns(aaUnknowns);
        setNucSubstitutions(nucSubstitutions);
        setNucDeletions(nucDeletions);
        setNucInsertions(nucInsertions);
        setNucUnknowns(nucUnknowns);
        setAaInsertions(aaInsertions);
    }
}
