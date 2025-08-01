import { expect } from 'chai';
import { basePath, expectIsGzipEncoded, expectIsZstdEncoded, sequenceData } from './common';

const routes = [
  { pathSegment: '/aggregated', servesFasta: false, expectedDownloadFilename: 'aggregated.json' },
  {
    pathSegment: '/mostRecentCommonAncestor',
    servesFasta: false,
    expectedDownloadFilename: 'mostRecentCommonAncestor.json',
  },
  { pathSegment: '/details', servesFasta: false, expectedDownloadFilename: 'details.json' },
  {
    pathSegment: '/nucleotideMutations',
    servesFasta: false,
    expectedDownloadFilename: 'nucleotideMutations.json',
  },
  {
    pathSegment: '/aminoAcidMutations',
    servesFasta: false,
    expectedDownloadFilename: 'aminoAcidMutations.json',
  },
  {
    pathSegment: '/nucleotideInsertions',
    servesFasta: false,
    expectedDownloadFilename: 'nucleotideInsertions.json',
  },
  {
    pathSegment: '/aminoAcidInsertions',
    servesFasta: false,
    expectedDownloadFilename: 'aminoAcidInsertions.json',
  },
  {
    pathSegment: '/alignedNucleotideSequences',
    servesFasta: true,
    expectedDownloadFilename: 'alignedNucleotideSequences.fasta',
  },
  {
    pathSegment: '/alignedAminoAcidSequences/S',
    servesFasta: true,
    expectedDownloadFilename: 'alignedAminoAcidSequences.fasta',
  },
  {
    pathSegment: '/unalignedNucleotideSequences',
    servesFasta: true,
    expectedDownloadFilename: 'unalignedNucleotideSequences.fasta',
  },
];

describe('All endpoints', () => {
  for (const route of routes) {
    const url = `${basePath}/sample${route.pathSegment}`;

    function get(params?: URLSearchParams, requestInit?: RequestInit) {
      if (params === undefined) {
        return fetch(url, requestInit);
      }

      return fetch(url + '?' + params.toString(), requestInit);
    }

    describe(`(${route.pathSegment})`, () => {
      it('should return the data with Content-Disposition when asking for download', async () => {
        const urlParams = new URLSearchParams({ downloadAsFile: 'true' });
        if (route.pathSegment === '/mostRecentCommonAncestor') {
          urlParams.set('phyloTreeField', 'primaryKey');
        }

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-disposition')).equals(
          `attachment; filename=${route.expectedDownloadFilename}`
        );
      });

      it('should return the data with Content-Disposition with custom file name', async () => {
        const urlParams = new URLSearchParams({ downloadAsFile: 'true', downloadFileBasename: 'custom' });
        if (route.pathSegment === '/mostRecentCommonAncestor') {
          urlParams.set('phyloTreeField', 'primaryKey');
        }

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-disposition')).equals(
          `attachment; filename=custom.${route.servesFasta ? 'fasta' : 'json'}`
        );
      });

      it('should return the lapis data version header', async () => {
        const urlParams = new URLSearchParams();
        if (route.pathSegment === '/mostRecentCommonAncestor') {
          urlParams.set('phyloTreeField', 'primaryKey');
        }
        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('lapis-data-version')).to.match(/\d{10}/);
      });

      if (route.servesFasta) {
        it('should return sequences in fasta format', async () => {
          const response = await get(new URLSearchParams({ dataFormat: 'fasta' }));

          const { primaryKeys, sequences } = sequenceData(await response.text());

          expect(primaryKeys).to.have.length(100);
          expect(sequences).to.have.length(100);
        });

        it('should return sequences in ndjson format', async () => {
          const response = await get(new URLSearchParams({ dataFormat: 'ndjson' }));

          const lines = (await response.text()).split('\n').filter(line => line.length > 0);

          expect(lines).to.have.length(100);
          expect(JSON.parse(lines[0])).to.have.property('primaryKey');
        });

        it('should return bad request for invalid fasta header template', async () => {
          const response = await get(
            new URLSearchParams({
              dataFormat: 'fasta',
              fastaHeaderTemplate: '{unknown field}',
            })
          );

          expect(response.status).equals(400);
          expect((await response.json()).error.detail).to.include('Invalid FASTA header template');
        });
      } else {
        it('should return the lapis data version header for CSV data', async () => {
          const urlParams = new URLSearchParams({ dataFormat: 'csv' });
          if (route.pathSegment === '/mostRecentCommonAncestor') {
            urlParams.set('phyloTreeField', 'primaryKey');
          }
          const response = await get(urlParams);

          expect(response.status).equals(200);
          expect(response.headers.get('lapis-data-version')).to.match(/\d{10}/);
        });

        it('should return the lapis data version header for TSV data', async () => {
          const urlParams = new URLSearchParams({ dataFormat: 'tsv' });
          if (route.pathSegment === '/mostRecentCommonAncestor') {
            urlParams.set('phyloTreeField', 'primaryKey');
          }
          const response = await get(urlParams);

          expect(response.status).equals(200);
          expect(response.headers.get('lapis-data-version')).to.match(/\d{10}/);
        });
      }

      it('should return zstd compressed data when asking for compression', async () => {
        const urlParams = new URLSearchParams({ compression: 'zstd' });
        if (route.pathSegment === '/mostRecentCommonAncestor') {
          urlParams.set('phyloTreeField', 'primaryKey');
        }

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-type')).equals('application/zstd');
        expect(response.headers.get('content-encoding')).does.not.exist;
        expectIsZstdEncoded(await response.arrayBuffer());
      });

      it('should return zstd compressed data when accepting compression in header', async () => {
        const urlParams = new URLSearchParams();
        if (route.pathSegment === '/mostRecentCommonAncestor') {
          urlParams.set('phyloTreeField', 'primaryKey');
        }

        const response = await get(urlParams, { headers: { 'Accept-Encoding': 'zstd' } });

        expect(response.status).equals(200);
        if (route.servesFasta) {
          expect(response.headers.get('content-type')).equals('text/x-fasta;charset=UTF-8');
        } else {
          expect(response.headers.get('content-type')).equals('application/json');
        }
        expect(response.headers.get('content-encoding')).equals('zstd');
        expectIsZstdEncoded(await response.arrayBuffer());
      });

      it('should return gzip compressed data when asking for compression', async () => {
        const urlParams = new URLSearchParams({ compression: 'gzip' });
        if (route.pathSegment === '/mostRecentCommonAncestor') {
          urlParams.set('phyloTreeField', 'primaryKey');
        }

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-type')).equals('application/gzip');
        expect(response.headers.get('content-encoding')).does.not.exist;
        expectIsGzipEncoded(await response.arrayBuffer());
      });

      it('should return gzip compressed data when accepting compression in header', async () => {
        const urlParams = new URLSearchParams();
        if (route.pathSegment === '/mostRecentCommonAncestor') {
          urlParams.set('phyloTreeField', 'primaryKey');
        }

        const response = await get(urlParams, { headers: { 'Accept-Encoding': 'gzip' } });

        expect(response.status).equals(200);
        if (route.servesFasta) {
          expect(response.headers.get('content-type')).equals('text/x-fasta;charset=UTF-8');
        } else {
          expect(response.headers.get('content-type')).equals('application/json');
        }
        expect(response.headers.get('content-encoding')).equals('gzip');

        if (route.servesFasta) {
          expect(await response.text()).to.match(/^>key_/);
        } else {
          const body = await response.json();
          expect(body.data).is.an('array');
        }
      });

      it('should accept form url encoded requests', async () => {
        const formUrlEncodedData = new URLSearchParams({
          pangoLineage: 'B.1.1.7',
          country: 'Switzerland',
        });
        if (route.pathSegment === '/mostRecentCommonAncestor') {
          formUrlEncodedData.set('phyloTreeField', 'primaryKey');
        }

        const response = await fetch(url, {
          method: 'POST',
          body: formUrlEncodedData,
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        });

        let body = await response.text();
        expect(response.status, 'body was ' + body).equals(200);
        if (route.servesFasta) {
          expect(response.headers.get('content-type')).equals('text/x-fasta;charset=UTF-8');
        } else {
          expect(response.headers.get('content-type')).equals('application/json');
        }
        expect(body).not.to.be.empty;
      });

      if (!route.servesFasta) {
        it('should return info', async () => {
          const urlParams = new URLSearchParams();
          if (route.pathSegment === '/mostRecentCommonAncestor') {
            urlParams.set('phyloTreeField', 'primaryKey');
          }
          const response = await get(urlParams);

          const info = (await response.json()).info;

          expect(info).to.have.property('dataVersion').and.to.match(/\d+/);
          expect(info).to.have.property('lapisVersion').and.to.be.not.empty;
          expect(info).to.have.property('requestId').and.to.be.not.empty;
          expect(info).to.have.property('requestInfo').and.to.be.not.empty;
          expect(info).to.have.property('reportTo').and.to.be.not.empty;
        });
      }
    });
  }
});
