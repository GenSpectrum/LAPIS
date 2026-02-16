import { expect } from 'chai';
import { basePath, queryClient } from './common';
import { And, Failure, Success } from './lapisClient/index';

describe('The /query/parse endpoint', () => {
  const url = `${basePath}/query/parse`;

  it('should successfully parse a valid query', async () => {
    const result = await queryClient.postParse({
      queryParseRequest: {
        queries: ['country = USA'],
      },
    });

    expect(result.data).to.be.an('array').with.lengthOf(1);

    const query = result.data[0];
    expect(query).to.have.property('type', 'success');
    expect(query).to.have.property('filter');
    expect(query).to.not.have.property('error');

    const success = query as Success;
    expect(success.filter).to.deep.include({
      type: 'StringEquals',
      column: 'country',
      value: 'USA',
    });
  });

  it('should return an error for an invalid query', async () => {
    const { data } = await queryClient.postParse({
      queryParseRequest: {
        queries: ['invalid syntax!!!'],
      },
    });

    expect(data).to.be.an('array').with.lengthOf(1);

    const query = data[0];
    expect(query).to.have.property('type', 'failure');
    expect(query).to.have.property('error');
    expect(query).to.not.have.property('filter');

    const failure = query as Failure;
    expect(failure.error).to.be.a('string').and.not.be.empty;
  });

  it('should handle multiple queries with mixed results', async () => {
    const { data } = await queryClient.postParse({
      queryParseRequest: {
        queries: ['country = Switzerland', 'bad query', 'age >= 30'],
      },
    });

    expect(data).to.be.an('array').with.lengthOf(3);

    // First query should succeed
    expect(data[0]).to.have.property('type', 'success');
    expect(data[0]).to.have.property('filter');
    expect(data[0]).to.not.have.property('error');

    // Second query should fail
    expect(data[1]).to.have.property('type', 'failure');
    expect(data[1]).to.have.property('error');
    expect(data[1]).to.not.have.property('filter');

    // Third query should succeed
    expect(data[2]).to.have.property('type', 'success');
    expect(data[2]).to.have.property('filter');
    expect(data[2]).to.not.have.property('error');
  });

  it('should return empty array for empty queries list', async () => {
    const { data } = await queryClient.postParse({
      queryParseRequest: {
        queries: [],
      },
    });

    expect(data).to.be.an('array').that.is.empty;
  });

  it('should include dataVersion in response', async () => {
    const { info } = await queryClient.postParse({
      queryParseRequest: {
        queries: ['country = USA'],
      },
    });

    expect(info).to.have.property('dataVersion');
    expect(info.dataVersion).to.match(/\d+/);
  });

  it('should parse complex queries with boolean operators', async () => {
    const { data } = await queryClient.postParse({
      queryParseRequest: {
        queries: ['country = USA & age >= 30'],
      },
    });

    expect(data).to.be.an('array').with.lengthOf(1);

    const query = data[0];
    expect(query).to.have.property('type', 'success');

    const success = query as Success;
    expect(success.filter).to.have.property('type', 'And');
    expect(success.filter).to.have.property('children');

    const and = success.filter as And;
    expect(and.children).to.be.an('array').with.lengthOf(2);
  });

  it('should return 400 for malformed JSON', async () => {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: '{"queries": [invalid json}',
    });

    expect(response.status).to.equal(400);
  });

  it('should return 400 for wrong type in queries field', async () => {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ queries: 'should be array' }),
    });

    expect(response.status).to.equal(400);
  });
});
