import { expect } from 'chai';
import { basePath, lapisClient } from './common';
import fs from 'fs';
import { AggregatedPostRequest, AggregatedResponse } from './lapisClient';

const queriesPath = __dirname + '/variantQueries';
const variantQueryFiles = fs.readdirSync(queriesPath).filter(f => f.endsWith('.json'));

type VariantQueryRequest = {
  requestName: string;
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

describe('Variant Query Tests', function () {
  this.timeout(10000); // Set timeout to 10 seconds for all tests in this suite

  variantQueryFiles
    .map(file => JSON.parse(fs.readFileSync(`${queriesPath}/${file}`).toString()))
    .forEach((testCase: VariantQueryTestCase) => {
      it(`should handle variant queries: ${testCase.testCaseName}`, async () => {
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

  it('should reject invalid variant queries with appropriate error', async () => {
    const urlParams = new URLSearchParams({
      variantQuery: 'not a valid variant query syntax!@#$',
    });

    const result = await fetch(basePath + '/sample/aggregated?' + urlParams.toString());
    expect(result.status).to.equal(400);
    const resultJson = await result.json();
    expect(resultJson.error.detail).to.include('Failed to parse variant query');
  });

  it('should enforce variantQuery to have exactly one value', async () => {
    const urlParams = new URLSearchParams();
    urlParams.append('variantQuery', '123A');
    urlParams.append('variantQuery', '123G');

    const result = await fetch(basePath + '/sample/aggregated?' + urlParams.toString());
    expect(result.status).to.equal(400);
    const resultJson = await result.json();
    expect(resultJson.error.detail).to.include('variantQuery must have exactly one value');
  });
});
