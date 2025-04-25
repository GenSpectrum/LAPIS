import { expect } from 'chai';
import { basePath, lapisClient, lapisClientMultiSegmented } from './common';

function getNucleotideInsertions(params?: URLSearchParams) {
  const nucleotideInsertionsEndpoint = '/sample/nucleotideInsertions';
  if (params === undefined) {
    return fetch(basePath + nucleotideInsertionsEndpoint);
  }

  return fetch(basePath + nucleotideInsertionsEndpoint + '?' + params.toString());
}

describe('The /nucleotideInsertions endpoint', () => {
  let someInsertion = 'ins_25701:CCC';

  it('should return nucleotide insertions for Switzerland', async () => {
    const result = await lapisClient.postNucleotideInsertions({
      insertionsRequest: { country: 'Switzerland' },
    });

    expect(result.data).to.have.length(4);

    const specificInsertion = result.data.find(insertionData => insertionData.insertion === someInsertion);
    expect(specificInsertion?.count).to.equal(17);
    expect(specificInsertion?.insertedSymbols).to.equal('CCC');
    expect(specificInsertion?.position).to.equal(25701);
    expect(specificInsertion?.sequenceName).to.be.null;
  });

  it('should return nucleotide insertions for multi segmented sequences', async () => {
    const result = await lapisClientMultiSegmented.postNucleotideInsertions({
      insertionsRequest: { country: 'Switzerland' },
    });

    expect(result.data).to.have.length(2);

    const insertionsFirstSegment = result.data.find(mutationData => mutationData.insertion === 'ins_L:1:AB');
    expect(insertionsFirstSegment?.count).to.equal(2);

    const insertionsSecondSegment = result.data.find(mutationData => mutationData.insertion === 'ins_M:2:BC');
    expect(insertionsSecondSegment?.count).to.equal(1);
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postNucleotideInsertions({
      insertionsRequest: {
        orderBy: [{ field: 'insertion', type: 'ascending' }],
      },
    });
    expect(ascendingOrderedResult.data[0]).to.have.property('insertion', 'ins_22204:CAGAA');
    expect(ascendingOrderedResult.data).to.have.length(4);

    const descendingOrderedResult = await lapisClient.postNucleotideInsertions({
      insertionsRequest: {
        orderBy: [{ field: 'insertion', type: 'descending' }],
      },
    });

    expect(descendingOrderedResult.data).to.have.length(4);
    expect(descendingOrderedResult.data[3]).to.have.property('insertion', 'ins_22204:CAGAA');
  });

  it('advancedQuery can filter out unwanted insertions', async () => {
    const urlParams = new URLSearchParams({
      variantQuery: "NOT ins_5959:TAT AND country='Switzerland'",
    });

    const result = await getNucleotideInsertions(urlParams);
    expect(result.status).equals(200);
    const resultJson = await result.json();
    expect(resultJson.data).to.have.length(3);
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postNucleotideInsertions({
      insertionsRequest: {
        orderBy: [{ field: 'insertion', type: 'ascending' }],
        limit: 4,
      },
    });

    expect(resultWithLimit.data).to.have.length(4);

    const resultWithLimitAndOffset = await lapisClient.postNucleotideInsertions({
      insertionsRequest: {
        orderBy: [{ field: 'insertion', type: 'ascending' }],
        limit: 4,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(3);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should correctly handle nucleotide insertion requests', async () => {
    const result = await lapisClient.postNucleotideInsertions({
      insertionsRequest: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
      },
    });

    expect(result.data).to.have.length(2);
    expect(result.data[0]).to.deep.equal({
      count: 1,
      insertedSymbols: 'CCC',
      insertion: 'ins_25701:CCC',
      position: 25701,
      sequenceName: null,
    });
    expect(result.data[1]).to.deep.equal({
      count: 1,
      insertedSymbols: 'TAT',
      insertion: 'ins_5959:TAT',
      position: 5959,
      sequenceName: null,
    });
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const result = await lapisClient.postNucleotideInsertions({
      insertionsRequest: {
        aminoAcidInsertions: ['ins_S:214:E?E'],
      },
    });

    expect(result.data).to.have.length(0);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      dataFormat: 'csv',
      orderBy: 'position',
    });

    const result = await fetch(basePath + '/sample/nucleotideInsertions?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
insertion,count,insertedSymbols,position,sequenceName
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
ins_5959:TAT,2,TAT,5959,
ins_22204:CAGAA,1,CAGAA,22204,
ins_22339:GCTGGT,1,GCTGGT,22339,
ins_25701:CCC,17,CCC,25701,
`.trim()
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      dataFormat: 'tsv',
      orderBy: 'position',
    });

    const result = await fetch(basePath + '/sample/nucleotideInsertions?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
insertion	count	insertedSymbols	position	sequenceName
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
ins_5959:TAT	2	TAT	5959	
ins_22204:CAGAA	1	CAGAA	22204	
ins_22339:GCTGGT	1	GCTGGT	22339	
ins_25701:CCC	17	CCC	25701	
    `.trim()
    );
  });
});
