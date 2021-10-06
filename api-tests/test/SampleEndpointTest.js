const supertest = require('supertest');
const {ajv} = require('./validators')
const {isOkay, checkPayloadFromSchema} = require('./shared')


const server = supertest.agent(process.env.API_URL);


describe('Endpoint sample/aggregated', () => {
  const endpoint = '/sample/aggregated';

  const params1 = '';
  it(params1 + ' - no parameters', (done) => {
    isOkay(server.get(endpoint + params1))
      .expect(checkPayloadFromSchema({
        elements: {
          properties: {
            count: {type: 'uint32'}
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
            date: {type: 'timestamp', nullable: true},
            division: {type: 'string', nullable: true},
            count: {type: 'uint32'}
          }
        }
      }))
      .end(done);
  });

  const params3 = '?country=Switzerland&aaMutations=s:501Y,S:e484k&fields=date,dateSubmitted,region,country,division,' +
    'location,regionExposure,countryExposure,divisionExposure,age,sex,host,samplingStrategy,pangoLineage,' +
    'nextstrainClade,gisaidClade,submittingLab,originatingLab,hospitalized,died,fullyVaccinated'
  it(params3 + ' - Switzerland and S:501Y,S:484K by all fields', (done) => {
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
            mutation: {type: 'string'},
            proportion: {type: 'float64'}
          }
        }
      }))
      .end(done);
  });

  const params2 = '?nucMutations=A23403G,3037t&dateFrom=2021-01-01';
  it(params2 + ' - two nucleotide mutations and after 2021-01-01', (done) => {
    isOkay(server.get(endpoint + params2))
      .expect(checkPayloadFromSchema({
        elements: {
          properties: {
            mutation: {type: 'string'},
            proportion: {type: 'float64'}
          }
        }
      }))
      .end(done);
  });
});


describe('Endpoint sample/nuc-mutations', () => {
  const endpoint = '/sample/nuc-mutations';

  const params1 = '';
  it(params1 + ' - no parameters', (done) => {
    isOkay(server.get(endpoint + params1))
      .expect(checkPayloadFromSchema({
        elements: {
          properties: {
            mutation: {type: 'string'},
            proportion: {type: 'float64'}
          }
        }
      }))
      .end(done);
  });

  const params2 = '?nucMutations=A23403G&aaMutation:orF1B:P314l&dateTo=2021-06-01&region=Europe';
  it(params2 + ' - one amino acid mutation, one nucleotide mutation, before 2021-06-01 and in Europe',
    (done) => {
      isOkay(server.get(endpoint + params2))
        .expect(checkPayloadFromSchema({
          elements: {
            properties: {
              mutation: {type: 'string'},
              proportion: {type: 'float64'}
            }
          }
        }))
        .end(done);
    });
});


describe('Endpoint sample/contributors', () => {
  const endpoint = '/sample/contributors';

  const params1 = '?country=United Kingdom';
  it(params1 + ' - United Kingdom', (done) => {
    isOkay(server.get(endpoint + params1).maxResponseSize(1000000000))
      .expect(checkPayloadFromSchema({
        elements: {
          properties: {
            genbankAccession: {type: 'string', nullable: true},
            sraAccession: {type: 'string', nullable: true},
            gisaidEpiIsl: {type: 'string', nullable: true},
            submittingLab: {type: 'string', nullable: true},
            originatingLab: {type: 'string', nullable: true},
            authors: {type: 'string', nullable: true},
          }
        }
      }))
      .end(done);
  });

});
