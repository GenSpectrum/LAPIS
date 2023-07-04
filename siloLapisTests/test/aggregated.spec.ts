import { expect } from 'chai';
import { lapisClient } from './common';
import fs from 'fs';
import { AggregatedResponse, SequenceFiltersWithGroupByFields } from './lapisClient';

const queriesPath = __dirname + '/aggregatedQueries';
const aggregatedQueryFiles = fs.readdirSync(queriesPath);

type TestCase = {
  testCaseName: string;
  lapisRequest: SequenceFiltersWithGroupByFields;
  expected: AggregatedResponse[];
};

describe('The /aggregated endpoint', () => {
  aggregatedQueryFiles
    .map(file => JSON.parse(fs.readFileSync(`${queriesPath}/${file}`).toString()))
    .forEach((testCase: TestCase) =>
      it('should return data for the test case ' + testCase.testCaseName, async () => {
        const result = await lapisClient.postAggregated({
          sequenceFiltersWithGroupByFields: testCase.lapisRequest,
        });

        const resultWithoutUndefined = result.map((aggregatedResponse: AggregatedResponse) => {
          const responseWithoutUndefined: Partial<AggregatedResponse> = {};
          for (const [key, value] of Object.entries(aggregatedResponse)) {
            if (value !== undefined) {
              // @ts-ignore
              responseWithoutUndefined[key] = value;
            }
          }
          return responseWithoutUndefined;
        });

        expect(resultWithoutUndefined).to.have.deep.members(testCase.expected);
      })
    );
});
