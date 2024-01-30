import { expect } from 'chai';
import { basePath } from './common';

const routes = [
  { pathSegment: '/aggregated', expectedDownloadFilename: 'aggregated.json' },
  { pathSegment: '/details', expectedDownloadFilename: 'details.json' },
  { pathSegment: '/nucleotideMutations', expectedDownloadFilename: 'nucleotideMutations.json' },
  { pathSegment: '/aminoAcidMutations', expectedDownloadFilename: 'aminoAcidMutations.json' },
  { pathSegment: '/nucleotideInsertions', expectedDownloadFilename: 'nucleotideInsertions.json' },
  { pathSegment: '/aminoAcidInsertions', expectedDownloadFilename: 'aminoAcidInsertions.json' },
  {
    pathSegment: '/alignedNucleotideSequences',
    expectedDownloadFilename: 'alignedNucleotideSequences.fasta',
  },
  {
    pathSegment: '/alignedAminoAcidSequences/S',
    expectedDownloadFilename: 'alignedAminoAcidSequences.fasta',
  },
  {
    pathSegment: '/unalignedNucleotideSequences',
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
    });
  }
});
