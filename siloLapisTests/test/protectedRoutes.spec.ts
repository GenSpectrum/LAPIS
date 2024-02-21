import chai from 'chai';
import { basePathProtected, lapisClientProtected } from './common';
import chaiAsPromised from 'chai-as-promised';

chai.use(chaiAsPromised);
const expect = chai.expect;

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
    const result = lapisClientProtected.postAggregated1({ aggregatedPostRequest: {} });

    await expect(result)
      .to.be.eventually.rejected.and.have.property('response')
      .that.has.property('status', 403);
  });

  it('should deny access, when wrong access key is provided', async () => {
    const invalidAccessKey = 'invalidKey';
    const result = lapisClientProtected.postAggregated1({
      aggregatedPostRequest: { accessKey: invalidAccessKey },
    });

    await expect(result)
      .to.be.eventually.rejected.and.have.property('response')
      .that.has.property('status', 403);
  });

  it('should deny access, when providing aggregated access key on non aggregated route', async () => {
    const result = lapisClientProtected.postDetails1({
      detailsPostRequest: { accessKey: aggregatedAccessKey },
    });

    await expect(result)
      .to.be.eventually.rejected.and.have.property('response')
      .that.has.property('status', 403);
  });

  it('should grant access, when providing aggregated access key', async () => {
    const result = lapisClientProtected.postAggregated1({
      aggregatedPostRequest: { accessKey: aggregatedAccessKey },
    });

    await expect(result).to.be.fulfilled;
  });

  it('should grant access, when providing full access key', async () => {
    const result = lapisClientProtected.postAggregated1({
      aggregatedPostRequest: { accessKey: fullAccessKey },
    });

    await expect(result).to.be.fulfilled;
  });

  it('should grant access, when providing full access key on non aggregated route', async () => {
    const result = lapisClientProtected.postDetails1({
      detailsPostRequest: { accessKey: fullAccessKey },
    });

    await expect(result).to.be.fulfilled;
  });
});
