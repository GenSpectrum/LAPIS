const supertest = require('supertest');
const { ajv } = require('./validators')
const { isOkay, checkPayloadFromSchema } = require('./shared')


const server = supertest.agent(process.env.API_URL);


describe('Endpoint sample/aggregated', () => {
  const endpoint = '/sample/aggregated';

  const params1 = '';
  it(params1 + ' - no parameters', (done) => {
    isOkay(server.get(endpoint + params1))
      .expect(checkPayloadFromSchema({
        elements: {
          properties: {
            count: {
              type: 'uint32'
            }
          }
        }
      }))
      .end(done);
  });

  const params2 = '?country=Switzerland&pangoLineage=B.1.1.7&fields=date,division'
  it(params2 + ' - Switzerland and B.1.1.7 by date and division', (done) => {
    isOkay(server.get(endpoint + params2))
      .expect(checkPayloadFromSchema({
        elements: {
          properties: {
            date: {
              type: 'timestamp', nullable: true
            },
            division: {
              type: 'string', nullable: true
            },
            count: {
              type: 'uint32'
            }
          }
        }
      }))
      .end(done);
  });

  const params3 = '?country=Switzerland&aaMutations=s:501Y,S:e484k'
  it(params3 + ' - Switzerland and S:501Y,S:484K', (done) => {
    isOkay(server.get(endpoint + params3))
      .end(done);
  });
});


describe('Endpoint sample/aa-mutations', () => {
  const endpoint = '/sample/aa-mutations';

  const params1 = '';
  it(params1 + ' - no parameters', (done) => {
    isOkay(server.get(endpoint + params1))
      .expect(checkPayloadFromSchema({
        elements: {
          properties: {
            mutation: {
              type: 'string'
            },
            proportion: {
              type: 'float64'
            }
          }
        }
      }))
      .end(done);
  });
});
