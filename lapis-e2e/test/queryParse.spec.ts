import { expect } from 'chai';
import { basePath } from './common';

describe('The /query/parse endpoint', () => {
  const url = `${basePath}/query/parse`;

  async function postQueries(queries: string[]) {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ queries }),
    });
    return { response, data: await response.json() };
  }

  it('should successfully parse a valid query', async () => {
    const { response, data } = await postQueries(['country = USA']);

    expect(response.status).to.equal(200);
    expect(data).to.have.property('data');
    expect(data.data).to.be.an('array').with.lengthOf(1);

    const result = data.data[0];
    expect(result).to.have.property('type', 'success');
    expect(result).to.have.property('filter');
    expect(result).to.not.have.property('error');

    expect(result.filter).to.deep.include({
      type: 'StringEquals',
      column: 'country',
      value: 'USA',
    });
  });

  it('should return an error for an invalid query', async () => {
    const { response, data } = await postQueries(['invalid syntax !!!']);

    expect(response.status).to.equal(200);
    expect(data).to.have.property('data');
    expect(data.data).to.be.an('array').with.lengthOf(1);

    const result = data.data[0];
    expect(result).to.have.property('type', 'failure');
    expect(result).to.have.property('error');
    expect(result).to.not.have.property('filter');

    expect(result.error).to.be.a('string').and.not.be.empty;
  });

  it('should handle multiple queries with mixed results', async () => {
    const { response, data } = await postQueries(['country = Switzerland', 'bad query', 'age >= 30']);

    expect(response.status).to.equal(200);
    expect(data).to.have.property('data');
    expect(data.data).to.be.an('array').with.lengthOf(3);

    // First query should succeed
    expect(data.data[0]).to.have.property('type', 'success');
    expect(data.data[0]).to.have.property('filter');
    expect(data.data[0]).to.not.have.property('error');

    // Second query should fail
    expect(data.data[1]).to.have.property('type', 'failure');
    expect(data.data[1]).to.have.property('error');
    expect(data.data[1]).to.not.have.property('filter');

    // Third query should succeed
    expect(data.data[2]).to.have.property('type', 'success');
    expect(data.data[2]).to.have.property('filter');
    expect(data.data[2]).to.not.have.property('error');
  });

  it('should return empty array for empty queries list', async () => {
    const { response, data } = await postQueries([]);

    expect(response.status).to.equal(200);
    expect(data).to.have.property('data');
    expect(data.data).to.be.an('array').that.is.empty;
  });

  it('should include dataVersion in response', async () => {
    const { response, data } = await postQueries(['country = USA']);

    expect(response.status).to.equal(200);
    expect(data).to.have.property('info');
    expect(data.info).to.have.property('dataVersion');
    expect(data.info.dataVersion).to.match(/\d+/);
  });

  it('should parse complex queries with boolean operators', async () => {
    const { response, data } = await postQueries(['country = USA & age >= 30']);

    expect(response.status).to.equal(200);
    expect(data.data).to.be.an('array').with.lengthOf(1);

    const result = data.data[0];
    expect(result).to.have.property('type', 'success');
    expect(result.filter).to.have.property('type', 'And');
    expect(result.filter).to.have.property('children');
    expect(result.filter.children).to.be.an('array').with.lengthOf(2);
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
