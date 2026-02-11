import { expect } from 'chai';
import { basePath, expectIsGzipEncoded, expectIsZstdEncoded, sequenceData } from './common';

type ServesType = 'SEQUENCES' | 'TREE' | 'METADATA';

interface Route {
  pathSegment: string;
  servesType: ServesType;
  expectedDownloadFilename: string;
}

const routes: Route[] = [
  { pathSegment: '/aggregated', servesType: 'METADATA', expectedDownloadFilename: 'aggregated.json' },
  {
    pathSegment: '/mostRecentCommonAncestor',
    servesType: 'METADATA',
    expectedDownloadFilename: 'mostRecentCommonAncestor.json',
  },
  { pathSegment: '/details', servesType: 'METADATA', expectedDownloadFilename: 'details.json' },
  {
    pathSegment: '/nucleotideMutations',
    servesType: 'METADATA',
    expectedDownloadFilename: 'nucleotideMutations.json',
  },
  {
    pathSegment: '/aminoAcidMutations',
    servesType: 'METADATA',
    expectedDownloadFilename: 'aminoAcidMutations.json',
  },
  {
    pathSegment: '/nucleotideInsertions',
    servesType: 'METADATA',
    expectedDownloadFilename: 'nucleotideInsertions.json',
  },
  {
    pathSegment: '/aminoAcidInsertions',
    servesType: 'METADATA',
    expectedDownloadFilename: 'aminoAcidInsertions.json',
  },
  {
    pathSegment: '/alignedNucleotideSequences',
    servesType: 'SEQUENCES',
    expectedDownloadFilename: 'alignedNucleotideSequences.fasta',
  },
  {
    pathSegment: '/alignedAminoAcidSequences/S',
    servesType: 'SEQUENCES',
    expectedDownloadFilename: 'alignedAminoAcidSequences.fasta',
  },
  {
    pathSegment: '/unalignedNucleotideSequences',
    servesType: 'SEQUENCES',
    expectedDownloadFilename: 'unalignedNucleotideSequences.fasta',
  },
  {
    pathSegment: '/phyloSubtree',
    servesType: 'TREE',
    expectedDownloadFilename: 'phyloSubtree.nwk',
  },
];

const extensionMap: Record<ServesType, string> = {
  SEQUENCES: 'fasta',
  TREE: 'nwk',
  METADATA: 'json',
};

function expectedContentType(type: ServesType): string {
  switch (type) {
    case 'SEQUENCES':
      return 'text/x-fasta;charset=UTF-8';
    case 'TREE':
      return 'text/x-nh;charset=UTF-8';
    case 'METADATA':
      return 'application/json';
  }
}

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
        if (route.pathSegment === '/mostRecentCommonAncestor' || route.pathSegment === '/phyloSubtree') {
          urlParams.set('phyloTreeField', 'usherTree');
        }

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-disposition')).equals(
          `attachment; filename=${route.expectedDownloadFilename}; filename*=UTF-8''${route.expectedDownloadFilename}`
        );
      });

      it('should return the data with Content-Disposition with custom file name', async () => {
        const urlParams = new URLSearchParams({ downloadAsFile: 'true', downloadFileBasename: 'custom' });
        if (route.pathSegment === '/mostRecentCommonAncestor' || route.pathSegment === '/phyloSubtree') {
          urlParams.set('phyloTreeField', 'usherTree');
        }

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-disposition')).equals(
          `attachment; filename=custom.${extensionMap[route.servesType]}; filename*=UTF-8''custom.${extensionMap[route.servesType]}`
        );
      });

      it('should return the lapis data version header', async () => {
        const urlParams = new URLSearchParams();
        if (route.pathSegment === '/mostRecentCommonAncestor' || route.pathSegment === '/phyloSubtree') {
          urlParams.set('phyloTreeField', 'usherTree');
        }
        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('lapis-data-version')).to.match(/\d{10}/);
      });

      if (route.servesType === 'SEQUENCES') {
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
      }
      if (route.servesType === 'METADATA') {
        it('should return the lapis data version header for CSV data', async () => {
          const urlParams = new URLSearchParams({ dataFormat: 'csv' });
          if (route.pathSegment === '/mostRecentCommonAncestor') {
            urlParams.set('phyloTreeField', 'usherTree');
          }
          const response = await get(urlParams);

          expect(response.status).equals(200);
          expect(response.headers.get('lapis-data-version')).to.match(/\d{10}/);
        });

        it('should return the lapis data version header for TSV data', async () => {
          const urlParams = new URLSearchParams({ dataFormat: 'tsv' });
          if (route.pathSegment === '/mostRecentCommonAncestor') {
            urlParams.set('phyloTreeField', 'usherTree');
          }
          const response = await get(urlParams);

          expect(response.status).equals(200);
          expect(response.headers.get('lapis-data-version')).to.match(/\d{10}/);
        });
      }

      if (route.servesType === 'TREE') {
        it('should return the lapis data version header for nwk data', async () => {
          const urlParams = new URLSearchParams({ dataFormat: 'newick' });
          urlParams.set('phyloTreeField', 'usherTree');
          const response = await get(urlParams);

          expect(response.status).equals(200);
          expect(response.headers.get('lapis-data-version')).to.match(/\d{10}/);
        });
      }

      it('should return zstd compressed data when asking for compression', async () => {
        const urlParams = new URLSearchParams({ compression: 'zstd' });
        if (route.pathSegment === '/mostRecentCommonAncestor' || route.pathSegment === '/phyloSubtree') {
          urlParams.set('phyloTreeField', 'usherTree');
        }

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-type')).equals('application/zstd');
        expect(response.headers.get('content-encoding')).does.not.exist;
        expectIsZstdEncoded(await response.arrayBuffer());
      });

      it('should return zstd compressed data when accepting compression in header', async () => {
        const urlParams = new URLSearchParams();
        if (route.pathSegment === '/mostRecentCommonAncestor' || route.pathSegment === '/phyloSubtree') {
          urlParams.set('phyloTreeField', 'usherTree');
        }

        const response = await get(urlParams, { headers: { 'Accept-Encoding': 'zstd' } });

        expect(response.status).equals(200);
        expect(response.headers.get('content-type')).to.equal(expectedContentType(route.servesType));
        expect(response.headers.get('content-encoding')).equals('zstd');

        // fetch automatically decompresses zstd responses as of node 24
        if (route.servesType === 'SEQUENCES') {
          expect(await response.text()).to.match(/^>key_/);
        } else if (route.servesType === 'TREE') {
          expect(await response.text()).to.match(/NODE_\d+;$/);
        } else {
          const body = await response.json();
          expect(body.data).is.an('array');
        }
      });

      it('should return gzip compressed data when asking for compression', async () => {
        const urlParams = new URLSearchParams({ compression: 'gzip' });
        if (route.pathSegment === '/mostRecentCommonAncestor' || route.pathSegment === '/phyloSubtree') {
          urlParams.set('phyloTreeField', 'usherTree');
        }

        const response = await get(urlParams);

        expect(response.status).equals(200);
        expect(response.headers.get('content-type')).equals('application/gzip');
        expect(response.headers.get('content-encoding')).does.not.exist;
        expectIsGzipEncoded(await response.arrayBuffer());
      });

      it('should return gzip compressed data when accepting compression in header', async () => {
        const urlParams = new URLSearchParams();
        if (route.pathSegment === '/mostRecentCommonAncestor' || route.pathSegment === '/phyloSubtree') {
          urlParams.set('phyloTreeField', 'usherTree');
        }

        const response = await get(urlParams, { headers: { 'Accept-Encoding': 'gzip' } });

        expect(response.status).equals(200);
        expect(response.headers.get('content-type')).to.equal(expectedContentType(route.servesType));
        expect(response.headers.get('content-encoding')).equals('gzip');

        if (route.servesType === 'SEQUENCES') {
          expect(await response.text()).to.match(/^>key_/);
        } else if (route.servesType === 'TREE') {
          expect(await response.text()).to.match(/NODE_\d+;$/);
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
        if (route.pathSegment === '/mostRecentCommonAncestor' || route.pathSegment === '/phyloSubtree') {
          formUrlEncodedData.set('phyloTreeField', 'usherTree');
        }

        const response = await fetch(url, {
          method: 'POST',
          body: formUrlEncodedData,
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        });

        let body = await response.text();
        expect(response.status, 'body was ' + body).equals(200);
        expect(response.headers.get('content-type')).to.equal(expectedContentType(route.servesType));
        expect(body).not.to.be.empty;
      });

      if (route.servesType === 'METADATA') {
        it('should return info', async () => {
          const urlParams = new URLSearchParams();
          if (route.pathSegment === '/mostRecentCommonAncestor') {
            urlParams.set('phyloTreeField', 'usherTree');
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
