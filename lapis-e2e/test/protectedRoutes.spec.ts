import { expect } from 'chai';
import { basePathProtected, lapisClientProtected } from './common';

const aggregatedAccessKey = 'testAggregatedDataAccessKey';
const fullAccessKey = 'testFullAccessKey';

describe('Protected mode on GET requests', () => {
  it('should deny access, when no access key is provided', async () => {
    const result = await fetch(basePathProtected + '/sample/aggregated');

    expect(result.status).equals(403);
    expect(result.headers.get('Content-Type')).equals('application/json;charset=ISO-8859-1');
    expect((await result.json()).error).to.deep.equal({
      detail: 'An access key is required to access /sample/aggregated.',
      status: 403,
      title: 'Forbidden',
      type: 'about:blank',
    });
  });

  it('should deny access, when wrong access key is provided', async () => {
    const invalidAccessKey = 'invalidKey';
    const result = await fetch(`${basePathProtected}/sample/aggregated?accessKey=${invalidAccessKey}`);

    expect(result.status).equals(403);
    expect(result.headers.get('Content-Type')).equals('application/json;charset=ISO-8859-1');
    expect((await result.json()).error).to.deep.equal({
      detail: 'You are not authorized to access /sample/aggregated.',
      status: 403,
      title: 'Forbidden',
      type: 'about:blank',
    });
  });

  it('should deny access, when providing aggregated access key on non aggregated route', async () => {
    const result = await fetch(`${basePathProtected}/sample/details?accessKey=${aggregatedAccessKey}`);

    expect(result.status).equals(403);
    expect(result.headers.get('Content-Type')).equals('application/json;charset=ISO-8859-1');
    expect((await result.json()).error).to.deep.equal({
      detail: 'You are not authorized to access /sample/details.',
      status: 403,
      title: 'Forbidden',
      type: 'about:blank',
    });
  });

  it('should grant access, when providing aggregated access key', async () => {
    const result = await fetch(`${basePathProtected}/sample/aggregated?accessKey=${aggregatedAccessKey}`);

    expect(result.status).equals(200);
  });

  it('should grant access, when providing full access key', async () => {
    const result = await fetch(`${basePathProtected}/sample/aggregated?accessKey=${fullAccessKey}`);

    expect(result.status).equals(200);
  });

  it('should grant access, when providing full access key on non aggregated route', async () => {
    const result = await fetch(`${basePathProtected}/sample/details?accessKey=${fullAccessKey}`);

    expect(result.status).equals(200);
  });
});

describe('Protected mode on POST requests', () => {
  it('should deny access, when no access key is provided', async () => {
    const result = lapisClientProtected.postAggregated({ aggregatedPostRequest: {} });

    await expectResponseStatusIs(result, 403);
  });

  it('should deny access, when wrong access key is provided', async () => {
    const invalidAccessKey = 'invalidKey';
    const result = lapisClientProtected.postAggregated({
      aggregatedPostRequest: { accessKey: invalidAccessKey },
    });

    await expectResponseStatusIs(result, 403);
  });

  it('should deny access, when providing aggregated access key on non aggregated route', async () => {
    const result = lapisClientProtected.postDetails({
      detailsPostRequest: { accessKey: aggregatedAccessKey },
    });

    await expectResponseStatusIs(result, 403);
  });

  it('should grant access, when providing aggregated access key', async () => {
    const result = lapisClientProtected.postAggregated({
      aggregatedPostRequest: { accessKey: aggregatedAccessKey },
    });

    expect(await result).to.be.ok;
  });

  it('should grant access, when providing full access key', async () => {
    const result = lapisClientProtected.postAggregated({
      aggregatedPostRequest: { accessKey: fullAccessKey },
    });

    expect(await result).to.be.ok;
  });

  it('should grant access, when providing full access key on non aggregated route', async () => {
    const result = lapisClientProtected.postDetails({
      detailsPostRequest: { accessKey: fullAccessKey },
    });

    expect(await result).to.be.ok;
  });

  async function expectResponseStatusIs(response: Promise<any>, expectedStatus: number) {
    try {
      const success = await response;
      expect.fail('Expected response to be rejected, but was ' + JSON.stringify(success));
    } catch (e) {
      expect(e).to.have.property('response').that.has.property('status', expectedStatus);
    }
  }
});
