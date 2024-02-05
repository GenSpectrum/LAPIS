import { expect } from 'chai';
import { basePath } from './common';

describe('The request id', () => {
  it('should be returned when explicitly specified', async () => {
    const requestID = 'hardcodedRequestIdInTheTest';

    // TODO(#627): bring back the old tests using the generated client
    // const result = await lapisClient.postAggregated1({
    //   aggregatedPostRequest: {},
    //   xRequestID: requestID,
    // });
    const result = await fetch(basePath + '/sample/aggregated', { headers: { 'X-Request-Id': requestID } });

    // expect(result.info.requestId).equals(requestID);
    expect((await result.json()).info.requestId).equals(requestID);
  });

  it('should be generated when none is specified', async () => {
    // const result = await lapisClient.postAggregated1({
    //   aggregatedPostRequest: {},
    // });
    const result = await fetch(basePath + '/sample/aggregated');

    // expect(result.info.requestId).length.is.at.least(1);
    expect((await result.json()).info.requestId).length.is.at.least(1);
  });
});
