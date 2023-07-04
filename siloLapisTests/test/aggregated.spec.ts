import { expect } from 'chai';
import { lapisClient } from './common';
import fs from 'fs';
import {AggregatedResponse, SequenceFiltersWithFields} from './lapisClient';

const queriesPath = __dirname + '/aggregatedQueries';
const aggregatedQueryFiles = fs.readdirSync(queriesPath);

type TestCase = {
    testCaseName: string;
    lapisRequest: SequenceFiltersWithFields;
    expected: AggregatedResponse[];
};

describe('The /aggregated endpoint', () => {
  aggregatedQueryFiles
    .map(file => JSON.parse(fs.readFileSync(`${queriesPath}/${file}`).toString()))
    .forEach((testCase: TestCase) =>
      it('should return data for the test case ' + testCase.testCaseName, async () => {
        const result = await lapisClient.postAggregated({
          sequenceFiltersWithFields: testCase.lapisRequest,
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
});
