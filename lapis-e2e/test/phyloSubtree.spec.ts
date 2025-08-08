import { expect } from 'chai';
import { basePath, lapisClient } from './common';

describe('The /phyloSubtree endpoint', () => {
  it('should return a newick', async () => {
    const urlParams = new URLSearchParams({
      phyloTreeField: 'primaryKey',
      advancedQuery: 'primaryKey=key_2181005 OR primaryKey=key_2270139',
    });

    const result = await fetch(`${basePath}/sample/phyloSubtree?${urlParams}`);

    const text = await result.text();
    expect(result.status, text).to.equal(200);
    expect(text.split('\n')[0]).to.equal('(key_2270139:1e-06,key_2181005:0.00010033)NODE_0000043;');
  });

  it('should return empty string as newick if filter has no nodes', async () => {
    const urlParams = new URLSearchParams({
      phyloTreeField: 'primaryKey',
      advancedQuery: 'primaryKey=string',
    });

    const result = await fetch(`${basePath}/sample/phyloSubtree?${urlParams}`);

    const text = await result.text();
    expect(result.status, text).to.equal(200);
    expect(text.split('\n')[0]).to.equal('');
  });

  it('should throw an error for phyloSubtree requests with invalid phyloTreeField', async () => {
    const urlParams = new URLSearchParams({
      phyloTreeField: 'division',
    });

    const result = await fetch(basePath + '/sample/phyloSubtree?' + urlParams.toString());

    expect(result.status).equals(400);
    expect(result.headers.get('Content-Type')).equals('application/json');
    const json = await result.json();
    expect(json.error.detail).to.include(
      "'division' is not a phylo tree field, known phylo tree fields are [primaryKey]"
    );
  });
});
