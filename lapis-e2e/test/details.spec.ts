import { expect } from 'chai';
import { basePath, lapisClient } from './common';

describe('The /details endpoint', () => {
  it('should return details with specified fields', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        pangoLineage: 'B.1.617.2',
        fields: ['pangoLineage', 'division'],
      },
    });

    expect(result.data).to.have.length(2);
    expect(result.data[0]).to.be.deep.equal({
      division: 'Zürich',
      pangoLineage: 'B.1.617.2',
    });
  });

  it('should return details with all fields when no explicit fields were specified', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        pangoLineage: 'B.1.617.2',
      },
    });

    expect(result.data).to.have.length(2);
    expect(result.data[0]).to.be.deep.equal({
      age: 54,
      country: 'Switzerland',
      date: '2021-07-19',
      division: 'Zürich',
      primaryKey: 'key_3128796',
      pangoLineage: 'B.1.617.2',
      qc_value: 0.96,
      region: 'Europe',
      test_boolean_column: false,
    });
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'division', type: 'ascending' }],
        fields: ['division'],
      },
    });

    expect(ascendingOrderedResult.data[0].division).to.be.null;
    expect(ascendingOrderedResult.data[1].division).to.be.null;
    expect(ascendingOrderedResult.data[2]).to.have.property('division', 'Aargau');

    const descendingOrderedResult = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'division', type: 'descending' }],
        fields: ['division'],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('division', 'Zürich');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'primaryKey', type: 'ascending' }],
        fields: ['primaryKey'],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('primaryKey', 'key_1001920');

    const resultWithLimitAndOffset = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'primaryKey', type: 'ascending' }],
        fields: ['primaryKey'],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'division,pangoLineage,primaryKey',
      orderBy: 'primaryKey',
      limit: '3',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
division,pangoLineage,primaryKey
Vaud,B.1.177.44,key_1001493
Bern,B.1.177,key_1001920
Solothurn,B.1,key_1002052
    `.trim() + '\n'
    );
  });

  it('should return only CSV header when no data', async () => {
    const urlParams = new URLSearchParams({
      country: 'this country does not exist',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      'primaryKey,date,region,country,pangoLineage,division,age,qc_value,test_boolean_column\n'
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'division,pangoLineage,primaryKey',
      orderBy: 'primaryKey',
      limit: '3',
      dataFormat: 'tsv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
division	pangoLineage	primaryKey
Vaud	B.1.177.44	key_1001493
Bern	B.1.177	key_1001920
Solothurn	B.1	key_1002052
    `.trim() + '\n'
    );
  });

  it('should correctly handle nucleotide insertion requests', async () => {
    const expectedResultWithNucleotideInsertion = {
      age: 57,
      country: 'Switzerland',
      date: '2021-05-12',
      division: 'Zürich',
      primaryKey: 'key_3578231',
      pangoLineage: 'P.1',
      qc_value: 0.93,
      region: 'Europe',
      test_boolean_column: null,
    };

    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.deep.equal(expectedResultWithNucleotideInsertion);
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const expectedResultWithAminoAcidInsertion = {
      age: 52,
      country: 'Switzerland',
      date: '2021-07-04',
      division: 'Vaud',
      primaryKey: 'key_3259931',
      pangoLineage: 'AY.43',
      qc_value: 0.98,
      region: 'Europe',
      test_boolean_column: true,
    };

    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.deep.equal(expectedResultWithAminoAcidInsertion);
  });

  it("should provide a way to get a plain list of primary keys for CoV-Spectrum's UShER integration", async () => {
    const urlParams = new URLSearchParams({
      dataFormat: 'CSV-WITHOUT-HEADERS',
      fields: 'primaryKey',
      limit: '3',
      orderBy: 'primaryKey',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(result.headers.get('content-type')).equals('text/plain');
    expect(await result.text()).to.be.equal(
      String.raw`
key_1001493
key_1001920
key_1002052
    `.trim() + '\n'
    );
  });

  it('should order by random', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'random' }, { field: 'division' }],
        fields: ['primaryKey', 'division'],
      },
    });

    expect(result).to.have.nested.property('data[0].division', null);
  });
});
