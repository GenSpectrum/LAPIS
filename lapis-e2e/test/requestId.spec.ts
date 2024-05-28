import { expect } from 'chai';
import { lapisClient } from './common';

describe('The request id', () => {
  it('should be returned when explicitly specified', async () => {
    const requestID = 'hardcodedRequestIdInTheTest';

    const result = await lapisClient.postAggregated1({
      aggregatedPostRequest: {},
      xRequestID: requestID,
    });

    expect(result.info.requestId).equals(requestID);
  });

  it('should be generated when none is specified', async () => {
    const result = await lapisClient.postAggregated1({
      aggregatedPostRequest: {},
    });

    expect(result.info.requestId).length.is.at.least(1);
  });
});
