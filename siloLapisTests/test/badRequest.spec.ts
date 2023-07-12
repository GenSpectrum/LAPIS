import { expect } from 'chai';
import { basePath } from './common';

describe('Error handling: BadRequest', () => {
  it('should return a 400 JSON response for a bad request', async () => {
    const badRequestForAggregated = 'nucleotideMutations=someInvalidMutation';
    const result = await fetch(basePath + '/aggregated?' + badRequestForAggregated);

    expect(result.status).equals(400);
    expect(result.headers.get('Content-Type')).equals('application/problem+json');
    expect(await result.json()).to.deep.equal({
      detail: "Failed to convert 'nucleotideMutations' with value: 'someInvalidMutation'",
      instance: '/aggregated',
      status: 400,
      title: 'Bad Request',
      type: 'about:blank',
    });
  });
});
