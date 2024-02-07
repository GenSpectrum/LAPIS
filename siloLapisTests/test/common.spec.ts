import { expect } from 'chai';
import { basePath, expectIsZstdEncoded } from './common';

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

    function get(params?: URLSearchParams) {
      if (params === undefined) {
        return fetch(url);
      }

      return fetch(url + '?' + params.toString());
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

      it('should return zstd compressed data', async () => {
        const urlParams = new URLSearchParams({ compression: 'zstd' });

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-encoding')).equals('zstd');
        expectIsZstdEncoded(await response.arrayBuffer());
      });

      it('should return gzip compressed data', async () => {
        const urlParams = new URLSearchParams({ compression: 'gzip' });

        const response = await get(urlParams);

        expect(response.status).equals(200);
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
