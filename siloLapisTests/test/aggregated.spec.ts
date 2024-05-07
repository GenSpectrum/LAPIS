import { expect } from 'chai';
import { basePath, lapisClient } from './common';
import fs from 'fs';
import { AggregatedPostRequest, AggregatedResponse } from './lapisClient';

const queriesPath = __dirname + '/aggregatedQueries';
const aggregatedQueryFiles = fs.readdirSync(queriesPath);

function getAggregated(params?: URLSearchParams) {
  const aggregatedEndpoint = '/sample/aggregated';
  if (params === undefined) {
    return fetch(basePath + aggregatedEndpoint);
  }

  return fetch(basePath + aggregatedEndpoint + '?' + params.toString());
}

type TestCase = {
  testCaseName: string;
  lapisRequest: AggregatedPostRequest;
  expected: AggregatedResponse[];
};

describe('The /aggregated endpoint', () => {
  aggregatedQueryFiles
    .map(file => JSON.parse(fs.readFileSync(`${queriesPath}/${file}`).toString()))
    .forEach((testCase: TestCase) =>
      it('should return data for the test case ' + testCase.testCaseName, async () => {
        const result = await lapisClient.postAggregated1({
          aggregatedPostRequest: testCase.lapisRequest,
        });

        const resultWithoutUndefined = result.data.map(aggregatedResponse => {
          return Object.entries(aggregatedResponse)
            .filter(([, value]) => value !== undefined)
            .reduce(
              (accumulatedObject, [key, value]) => ({
                [key]: value,
                ...accumulatedObject,
              }),
              {} as AggregatedResponse
            );
        });

        expect(resultWithoutUndefined).to.have.deep.members(testCase.expected);
      })
    );

  it('should correctly handle multiple mutation requests in GET requests', async () => {
    const urlParams = new URLSearchParams({
      nucleotideMutations: 'T1-,A23062T',
      aminoAcidMutations: 'S:501Y,ORF1b:12',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data[0]).to.have.property('count', 0);
  });

  it('should correctly handle nucleotide insertion requests in GET requests', async () => {
    const urlParams = new URLSearchParams({
      nucleotideInsertions: 'ins_25701:CC?,ins_5959:?AT',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data[0]).to.have.property('count', 1);
  });

  it('should correctly handle amino acid insertion requests in GET requests', async () => {
    const urlParams = new URLSearchParams({
      aminoAcidInsertions: 'ins_S:143:T,ins_ORF1a:3602:F?P',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data[0]).to.have.property('count', 1);
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postAggregated1({
      aggregatedPostRequest: {
        orderBy: [{ field: 'division', type: 'ascending' }],
        fields: ['division'],
      },
    });

    expect(ascendingOrderedResult.data[0].division).to.be.undefined;
    expect(ascendingOrderedResult.data[1]).to.have.property('division', 'Aargau');

    const descendingOrderedResult = await lapisClient.postAggregated1({
      aggregatedPostRequest: {
        orderBy: [{ field: 'division', type: 'descending' }],
        fields: ['division'],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('division', 'Zürich');
  });

  it('should return bad request for non existing fields on GET request', async () => {
    const urlParams = new URLSearchParams({
      fields: 'notAField',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(400);
    const resultJson = await result.json();
    expect(resultJson.error.detail).to.include('Unknown field: notAField, known values are [primaryKey,');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postAggregated1({
      aggregatedPostRequest: {
        orderBy: [{ field: 'division', type: 'ascending' }],
        fields: ['division'],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('division', 'Aargau');

    const resultWithLimitAndOffset = await lapisClient.postAggregated1({
      aggregatedPostRequest: {
        orderBy: [{ field: 'division', type: 'ascending' }],
        fields: ['division'],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'country,age',
      orderBy: 'age',
      dataFormat: 'csv',
    });

    const result = await getAggregated(urlParams);

    expect(await result.text()).to.be.equal(
      String.raw`
age,country,count
,Switzerland,2
4,Switzerland,2
5,Switzerland,1
6,Switzerland,1
50,Switzerland,17
51,Switzerland,7
52,Switzerland,8
53,Switzerland,8
54,Switzerland,9
55,Switzerland,8
56,Switzerland,9
57,Switzerland,10
58,Switzerland,9
59,Switzerland,9
    `.trim() + '\n'
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'country,age',
      orderBy: 'age',
      dataFormat: 'tsv',
    });

    const result = await getAggregated(urlParams);

    expect(await result.text()).to.be.equal(
      String.raw`
age	country	count
	Switzerland	2
4	Switzerland	2
5	Switzerland	1
6	Switzerland	1
50	Switzerland	17
51	Switzerland	7
52	Switzerland	8
53	Switzerland	8
54	Switzerland	9
55	Switzerland	8
56	Switzerland	9
57	Switzerland	10
58	Switzerland	9
59	Switzerland	9
    `.trim() + '\n'
    );
  });

  it('should handle null values for boolean filters in GET requests', async () => {
    const urlParams = new URLSearchParams({
      test_boolean_column: '',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data[0]).to.have.property('count', 33);
  });

  it('should handle null values for int filters in GET requests', async () => {
    const urlParams = new URLSearchParams({
      age: '',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data[0]).to.have.property('count', 2);
  });

  it('should handle null values for float filters in GET requests', async () => {
    const urlParams = new URLSearchParams({
      qc_value: '',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data[0]).to.have.property('count', 2);
  });

  it('should handle null values for string filters in GET requests', async () => {
    const urlParams = new URLSearchParams({
      region: '',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data[0]).to.have.property('count', 1);
  });

  it('should throw for null values for pango lineage filters in GET requests', async () => {
    const urlParams = new URLSearchParams({
      pangoLineage: '',
    });

    const result = await getAggregated(urlParams);

    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data[0]).to.have.property('count', 1);
  });
});
