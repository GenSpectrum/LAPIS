import { expect } from 'chai';
import { lapisClient } from './common';
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
        const result = await lapisClient.postAggregated({
          aggregatedPostRequest: testCase.lapisRequest,
        });

        const resultWithoutUndefined = result.map((aggregatedResponse: AggregatedResponse) => {
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
    const result = await lapisClient.aggregated({
      nucleotideMutations: ['T1-', 'A23062T'],
      aminoAcidMutations: ['S:501Y', 'ORF1b:12'],
    });

    expect(result).to.have.length(1);
  });
});
