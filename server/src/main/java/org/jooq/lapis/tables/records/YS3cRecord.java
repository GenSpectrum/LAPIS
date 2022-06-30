/*
 * This file is generated by jOOQ.
 */
package org.jooq.lapis.tables.records;


import org.jooq.Field;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.TableRecordImpl;
import org.jooq.lapis.tables.YS3c;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class YS3cRecord extends TableRecordImpl<YS3cRecord> implements Record5<String, String, Boolean, Boolean, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>y_s3c.gisaid_epi_isl</code>.
     */
    public void setGisaidEpiIsl(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>y_s3c.gisaid_epi_isl</code>.
     */
    public String getGisaidEpiIsl() {
        return (String) get(0);
    }

    /**
     * Setter for <code>y_s3c.sra_accession</code>.
     */
    public void setSraAccession(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>y_s3c.sra_accession</code>.
     */
    public String getSraAccession() {
        return (String) get(1);
    }

    /**
     * Setter for <code>y_s3c.hospitalized</code>.
     */
    public void setHospitalized(Boolean value) {
        set(2, value);
    }

    /**
     * Getter for <code>y_s3c.hospitalized</code>.
     */
    public Boolean getHospitalized() {
        return (Boolean) get(2);
    }

    /**
     * Setter for <code>y_s3c.died</code>.
     */
    public void setDied(Boolean value) {
        set(3, value);
    }

    /**
     * Getter for <code>y_s3c.died</code>.
     */
    public Boolean getDied() {
        return (Boolean) get(3);
    }

    /**
     * Setter for <code>y_s3c.fully_vaccinated</code>.
     */
    public void setFullyVaccinated(Boolean value) {
        set(4, value);
    }

    /**
     * Getter for <code>y_s3c.fully_vaccinated</code>.
     */
    public Boolean getFullyVaccinated() {
        return (Boolean) get(4);
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<String, String, Boolean, Boolean, Boolean> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<String, String, Boolean, Boolean, Boolean> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return YS3c.Y_S3C.GISAID_EPI_ISL;
    }

    @Override
    public Field<String> field2() {
        return YS3c.Y_S3C.SRA_ACCESSION;
    }

    @Override
    public Field<Boolean> field3() {
        return YS3c.Y_S3C.HOSPITALIZED;
    }

    @Override
    public Field<Boolean> field4() {
        return YS3c.Y_S3C.DIED;
    }

    @Override
    public Field<Boolean> field5() {
        return YS3c.Y_S3C.FULLY_VACCINATED;
    }

    @Override
    public String component1() {
        return getGisaidEpiIsl();
    }

    @Override
    public String component2() {
        return getSraAccession();
    }

    @Override
    public Boolean component3() {
        return getHospitalized();
    }

    @Override
    public Boolean component4() {
        return getDied();
    }

    @Override
    public Boolean component5() {
        return getFullyVaccinated();
    }

    @Override
    public String value1() {
        return getGisaidEpiIsl();
    }

    @Override
    public String value2() {
        return getSraAccession();
    }

    @Override
    public Boolean value3() {
        return getHospitalized();
    }

    @Override
    public Boolean value4() {
        return getDied();
    }

    @Override
    public Boolean value5() {
        return getFullyVaccinated();
    }

    @Override
    public YS3cRecord value1(String value) {
        setGisaidEpiIsl(value);
        return this;
    }

    @Override
    public YS3cRecord value2(String value) {
        setSraAccession(value);
        return this;
    }

    @Override
    public YS3cRecord value3(Boolean value) {
        setHospitalized(value);
        return this;
    }

    @Override
    public YS3cRecord value4(Boolean value) {
        setDied(value);
        return this;
    }

    @Override
    public YS3cRecord value5(Boolean value) {
        setFullyVaccinated(value);
        return this;
    }

    @Override
    public YS3cRecord values(String value1, String value2, Boolean value3, Boolean value4, Boolean value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached YS3cRecord
     */
    public YS3cRecord() {
        super(YS3c.Y_S3C);
    }

    /**
     * Create a detached, initialised YS3cRecord
     */
    public YS3cRecord(String gisaidEpiIsl, String sraAccession, Boolean hospitalized, Boolean died, Boolean fullyVaccinated) {
        super(YS3c.Y_S3C);

        setGisaidEpiIsl(gisaidEpiIsl);
        setSraAccession(sraAccession);
        setHospitalized(hospitalized);
        setDied(died);
        setFullyVaccinated(fullyVaccinated);
    }
}