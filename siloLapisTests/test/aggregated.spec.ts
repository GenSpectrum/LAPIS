import { expect } from 'chai';
import { lapisClient, basePath } from './common';
import fs from 'fs';
import { AggregatedPostRequest } from './lapisClient/models/AggregatedPostRequest';
import { AggregatedResponse } from './lapisClient/models/AggregatedResponse';

const queriesPath = __dirname + '/aggregatedQueries';
const aggregatedQueryFiles = fs.readdirSync(queriesPath);

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

  it('should correctly handle mutliple mutation requests in GET requests', async () => {
    const result = await lapisClient.postAggregated1({
      aggregatedPostRequest: {
        nucleotideMutations: ['T1-', 'A23062T'],
        aminoAcidMutations: ['S:501Y', 'ORF1b:12'],
      },
    });

    expect(result.data).to.have.length(1);
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postAggregated1({
      aggregatedPostRequest: {
        orderBy: [{ field: 'division', type: 'ascending' }],
        fields: ['division'],
      },
    });

    expect(ascendingOrderedResult.data[0]).to.have.property('division', 'Aargau');

    const descendingOrderedResult = await lapisClient.postAggregated1({
      aggregatedPostRequest: {
        orderBy: [{ field: 'division', type: 'descending' }],
        fields: ['division'],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('division', 'Zürich');
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
    expect(resultWithLimit.data[1]).to.have.property('division', 'Basel-Land');

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

    const result = await fetch(basePath + '/aggregated?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
age,country,count
4,Switzerland,2
5,Switzerland,1
6,Switzerland,1
50,Switzerland,17
51,Switzerland,8
52,Switzerland,8
53,Switzerland,8
54,Switzerland,9
55,Switzerland,9
56,Switzerland,9
57,Switzerland,10
58,Switzerland,9
59,Switzerland,9
    `.trim()
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'country,age',
      orderBy: 'age',
      dataFormat: 'tsv',
    });

    const result = await fetch(basePath + '/aggregated?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
age	country	count
4	Switzerland	2
5	Switzerland	1
6	Switzerland	1
50	Switzerland	17
51	Switzerland	8
52	Switzerland	8
53	Switzerland	8
54	Switzerland	9
55	Switzerland	9
56	Switzerland	9
57	Switzerland	10
58	Switzerland	9
59	Switzerland	9
    `.trim()
    );
  });

  it('should return the lapis data version in the response', async () => {
    const result = await fetch(basePath + '/aggregated');

    expect(result.status).equals(200);
    expect(result.headers.get('lapis-data-version')).to.match(/\d{10}/);
  });
});
