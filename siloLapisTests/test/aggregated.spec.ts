import { expect } from 'chai';
import { lapisClient } from './common';
import fs from 'fs';
import { SequenceFilters } from './lapisClient';

const queriesPath = __dirname + '/aggregatedQueries';
const aggregatedQueryFiles = fs.readdirSync(queriesPath);

type TestCase = {
  testCaseName: string;
  lapisRequest: SequenceFilters;
  expected: { count: number };
};

describe('The /aggregated endpoint', () => {
  aggregatedQueryFiles
    .map(file => JSON.parse(fs.readFileSync(`${queriesPath}/${file}`).toString()))
    .forEach((testCase: TestCase) =>
      it('should return data for the test case ' + testCase.testCaseName, async () => {
        const result = await lapisClient.postAggregated({ sequenceFilters: testCase.lapisRequest });

        expect(result).deep.equals(testCase.expected);
      })
    );
});
