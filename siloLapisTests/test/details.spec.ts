import { expect } from 'chai';
import { basePath, lapisClient } from './common';
import fs from 'fs';
import { SequenceFilters } from './lapisClient';

describe('The /details endpoint', () => {
  it('should return details with specified fields', async () => {
    const result = await lapisClient.postDetails1({
      detailsPostRequest: {
        pangoLineage: 'B.1.617.2',
        fields: ['pangoLineage', 'division'],
      },
    });

    expect(result.data).to.have.length(2);
    expect(result.data[0]).to.be.deep.equal({
      aaInsertions: undefined,
      insertions: undefined,
      age: undefined,
      country: undefined,
      date: undefined,
      division: 'Zürich',
      gisaidEpiIsl: undefined,
      pangoLineage: 'B.1.617.2',
      qcValue: undefined,
      region: undefined,
    });
  });

  it('should return details with all fields when no explicit fields were specified', async () => {
    const result = await lapisClient.postDetails1({
      detailsPostRequest: {
        pangoLineage: 'B.1.617.2',
      },
    });

    expect(result.data).to.have.length(2);
    expect(result.data[0]).to.be.deep.equal({
      aaInsertions: undefined,
      insertions: '25701:CCC',
      age: 54,
      country: 'Switzerland',
      date: '2021-07-19',
      division: 'Zürich',
      gisaidEpiIsl: 'EPI_ISL_3128796',
      pangoLineage: 'B.1.617.2',
      qcValue: 0.96,
      region: 'Europe',
    });
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postDetails1({
      detailsPostRequest: {
        orderBy: [{ field: 'division', type: 'ascending' }],
        fields: ['division'],
      },
    });

    expect(ascendingOrderedResult.data[0].division).to.be.undefined;
    expect(ascendingOrderedResult.data[1].division).to.be.undefined;
    expect(ascendingOrderedResult.data[2]).to.have.property('division', 'Aargau');

    const descendingOrderedResult = await lapisClient.postDetails1({
      detailsPostRequest: {
        orderBy: [{ field: 'division', type: 'descending' }],
        fields: ['division'],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('division', 'Zürich');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postDetails1({
      detailsPostRequest: {
        orderBy: [{ field: 'gisaid_epi_isl', type: 'ascending' }],
        fields: ['gisaid_epi_isl'],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('gisaidEpiIsl', 'EPI_ISL_1001920');

    const resultWithLimitAndOffset = await lapisClient.postDetails1({
      detailsPostRequest: {
        orderBy: [{ field: 'gisaid_epi_isl', type: 'ascending' }],
        fields: ['gisaid_epi_isl'],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'gisaid_epi_isl,pangoLineage,division',
      orderBy: 'gisaid_epi_isl',
      limit: '3',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
division,gisaid_epi_isl,pangoLineage
Vaud,EPI_ISL_1001493,B.1.177.44
Bern,EPI_ISL_1001920,B.1.177
Solothurn,EPI_ISL_1002052,B.1
    `.trim()
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'gisaid_epi_isl,pangoLineage,division',
      orderBy: 'gisaid_epi_isl',
      limit: '3',
      dataFormat: 'tsv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
division	gisaid_epi_isl	pangoLineage
Vaud	EPI_ISL_1001493	B.1.177.44
Bern	EPI_ISL_1001920	B.1.177
Solothurn	EPI_ISL_1002052	B.1
    `.trim()
    );
  });

  it('should correctly handle nucleotide insertion requests', async () => {
    const expectedResultWithNucleotideInsertion = {
      aaInsertions: undefined,
      age: 57,
      country: 'Switzerland',
      date: '2021-05-12',
      division: 'Zürich',
      gisaidEpiIsl: 'EPI_ISL_3578231',
      insertions: '25701:CCC,5959:TAT',
      pangoLineage: 'B.1.1.28.1',
      qcValue: 0.93,
      region: 'Europe',
    };

    const result = await lapisClient.postDetails1({
      detailsPostRequest: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.deep.equal(expectedResultWithNucleotideInsertion);
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const expectedResultWithAminoAcidInsertion = {
      aaInsertions: 'S:143:T,ORF1a:3602:FEP',
      insertions: undefined,
      age: 52,
      country: 'Switzerland',
      date: '2021-07-04',
      division: 'Vaud',
      gisaidEpiIsl: 'EPI_ISL_3259931',
      pangoLineage: 'B.1.617.2.43',
      qcValue: 0.98,
      region: 'Europe',
    };

    const result = await lapisClient.postDetails1({
      detailsPostRequest: {
        aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.deep.equal(expectedResultWithAminoAcidInsertion);
  });

  it('should return the lapis data version in the response', async () => {
    const result = await fetch(basePath + '/sample/details');

    expect(result.status).equals(200);
    expect(result.headers.get('lapis-data-version')).to.match(/\d{10}/);
  });
});
