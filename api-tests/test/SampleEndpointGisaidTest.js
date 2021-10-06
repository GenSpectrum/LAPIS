const {
  isOkay,
  isNotOkay,
  checkPayloadFromSchema,

  apiUrl,
  server,
  openness,
} = require('./shared')


if (openness === 'gisaid') {
  describe('Endpoint sample/details', () => {
    const endpoint = '/sample/details';

    const params1 = '?aaMutations=S:n501y&pangoLineage=B.1.617.2*';
    it(params1 + ' - Delta with S:501Y - is forbidden', (done) => {
      isNotOkay(server.get(endpoint + params1))
        .end(done);
    });
  });

  describe('Endpoint sample/fasta', () => {
    const endpoint = '/sample/fasta';

    it(' - no params - is forbidden', (done) => {
      isNotOkay(server.get(endpoint))
        .end(done);
    });
  });

  describe('Endpoint sample/fasta-aligned', () => {
    const endpoint = '/sample/fasta-aligned';

    it(' - no params - is forbidden', (done) => {
      isNotOkay(server.get(endpoint))
        .end(done);
    });
  });
}
