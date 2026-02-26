import { expect } from 'chai';
import { lapisClient } from './common';
import fs from 'fs';
import { AggregatedPostRequest, AggregatedResponse } from './lapisClient';

const queriesPath = __dirname + '/advancedVariantQueries';
const advancedQueryFiles = fs.readdirSync(queriesPath).filter(f => f.endsWith('.json'));

type VariantQueryRequest = {
  requestName: string;
  description?: string;
  lapisRequest: AggregatedPostRequest;
  expected: AggregatedResponse[];
};

type VariantQueryTestCase = {
  testCaseName: string;
  description?: string;
  requests: VariantQueryRequest[];
};

function filterUndefinedValues(response: AggregatedResponse): AggregatedResponse {
  return Object.entries(response)
    .filter(([, value]) => value !== undefined)
    .reduce(
      (accumulatedObject, [key, value]) => ({
        [key]: value,
        ...accumulatedObject,
      }),
      {} as AggregatedResponse
    );
}

describe('Advanced Variant Query Tests', function () {
  advancedQueryFiles
    .map(file => JSON.parse(fs.readFileSync(`${queriesPath}/${file}`).toString()))
    .forEach((testCase: VariantQueryTestCase) => {
      it(`should handle advanced variant queries: ${testCase.testCaseName}`, async () => {
        for (const request of testCase.requests) {
          try {
            const result = await lapisClient.postAggregated({
              aggregatedPostRequest: request.lapisRequest,
            });

            const resultWithoutUndefined = result.data.map(filterUndefinedValues);

            expect(resultWithoutUndefined).to.have.deep.members(
              request.expected,
              `Failed for request: ${request.requestName}`
            );
          } catch (error) {
            throw new Error(
              `Variant query test failed for '${testCase.testCaseName}' ` +
                `at request '${request.requestName}': ${error instanceof Error ? error.message : String(error)}`
            );
          }
        }
      });
    });
});
