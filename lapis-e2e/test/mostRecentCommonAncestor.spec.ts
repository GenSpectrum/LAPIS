import { expect } from 'chai';
import { basePath, lapisClient } from './common';

describe('The /mostRecentCommonAncestor endpoint', () => {
  it('should return MRCA with missing nodes if specified', async () => {
    const result = await lapisClient.postMostRecentCommonAncestor({
      mostRecentCommonAncestorRequest: {
        phyloTreeField: 'primaryKey',
        printNodesNotInTree: true,
        advancedQuery: 'primaryKey=key_2181005 OR primaryKey=key_2270139 OR primaryKey=key_1408408',
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.be.deep.equal({
      mrcaNode: 'NODE_0000043',
      missingNodeCount: 1,
      missingFromTree: 'key_1408408',
    });
  });

  it('should return MRCA without missing nodes if specified', async () => {
    const result = await lapisClient.postMostRecentCommonAncestor({
      mostRecentCommonAncestorRequest: {
        phyloTreeField: 'primaryKey',
        printNodesNotInTree: false,
        advancedQuery: 'primaryKey=key_2181005 OR primaryKey=key_2270139 OR primaryKey=key_1408408',
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.be.deep.equal({
      mrcaNode: 'NODE_0000043',
      missingNodeCount: 1,
      missingFromTree: undefined, // null is converted to undefined in TypeScript
    });
  });

  it('should return MRCA as null if filter has no nodes', async () => {
    const result = await lapisClient.postMostRecentCommonAncestor({
      mostRecentCommonAncestorRequest: {
        phyloTreeField: 'primaryKey',
        printNodesNotInTree: false,
        primaryKey: 'string',
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.be.deep.equal({
      mrcaNode: undefined,
      missingNodeCount: 0,
      missingFromTree: undefined,
    });
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      phyloTreeField: 'primaryKey',
      dataFormat: 'csv',
      printNodesNotInTree: 'true',
      advancedQuery: 'primaryKey=key_2181005 OR primaryKey=key_2270139 OR primaryKey=key_1408408',
    });

    const result = await fetch(basePath + '/sample/mostRecentCommonAncestor?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
mrcaNode,missingNodeCount,missingFromTree
NODE_0000043,1,key_1408408
    `.trim() + '\n'
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      phyloTreeField: 'primaryKey',
      dataFormat: 'tsv',
      printNodesNotInTree: 'true',
      advancedQuery: 'primaryKey=key_2181005 OR primaryKey=key_2270139 OR primaryKey=key_1408408',
    });

    const result = await fetch(basePath + '/sample/mostRecentCommonAncestor?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
mrcaNode	missingNodeCount	missingFromTree
NODE_0000043	1	key_1408408
    `.trim() + '\n'
    );
  });

  it('should throw an error for invalid Maybe request', async () => {
    const urlParams = new URLSearchParams({
      phyloTreeField: 'division',
    });

    const result = await fetch(basePath + '/sample/mostRecentCommonAncestor?' + urlParams.toString());

    expect(result.status).equals(400);
    expect(result.headers.get('Content-Type')).equals('application/json');
    const json = await result.json();
    expect(json.error.detail).to.include(
      "'division' is not a phylo tree field, known phylo tree fields are [primaryKey]"
    );
  });
});
