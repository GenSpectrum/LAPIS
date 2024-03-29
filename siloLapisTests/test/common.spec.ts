import { expect } from 'chai';
import { basePath, expectIsGzipEncoded, expectIsZstdEncoded } from './common';

const routes = [
  { pathSegment: '/aggregated', servesFasta: false, expectedDownloadFilename: 'aggregated.json' },
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

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-disposition')).equals(
          `attachment; filename=${route.expectedDownloadFilename}`
        );
      });

      it('should return the lapis data version in the response', async () => {
        const response = await get();

        expect(response.status).equals(200);
        expect(response.headers.get('lapis-data-version')).to.match(/\d{10}/);
      });

      it('should return zstd compressed data when asking for compression', async () => {
        const urlParams = new URLSearchParams({ compression: 'zstd' });

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-type')).equals('application/zstd');
        expect(response.headers.get('content-encoding')).does.not.exist;
        expectIsZstdEncoded(await response.arrayBuffer());
      });

      it('should return zstd compressed data when accepting compression in header', async () => {
        const urlParams = new URLSearchParams();

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

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-type')).equals('application/gzip');
        expect(response.headers.get('content-encoding')).does.not.exist;
        expectIsGzipEncoded(await response.arrayBuffer());
      });

      it('should return gzip compressed data when accepting compression in header', async () => {
        const urlParams = new URLSearchParams();

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
    });
  }
});
