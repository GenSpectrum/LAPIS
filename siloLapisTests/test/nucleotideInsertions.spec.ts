import { expect } from 'chai';
import { basePath, lapisClient } from './common';

describe('The /nucleotideInsertions endpoint', () => {
  let someInsertion = 'ins_25701:CCC';

  it('should return nucleotide insertions for Switzerland', async () => {
    const result = await lapisClient.postNucleotideInsertions1({
      nucleotideInsertionsRequest: { country: 'Switzerland' },
    });

    expect(result.data).to.have.length(4);

    const specificInsertion = result.data.find(insertionData => insertionData.insertion === someInsertion);
    expect(specificInsertion?.count).to.equal(17);
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postNucleotideInsertions1({
      nucleotideInsertionsRequest: {
        orderBy: [{ field: 'count', type: 'ascending' }],
      },
    });

    expect(ascendingOrderedResult.data[0]).to.have.property('insertion', 'ins_5959:TAT');

    const descendingOrderedResult = await lapisClient.postNucleotideInsertions1({
      nucleotideInsertionsRequest: {
        orderBy: [{ field: 'count', type: 'descending' }],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('insertion', 'ins_25701:CCC');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postNucleotideInsertions1({
      nucleotideInsertionsRequest: {
        orderBy: [{ field: 'count', type: 'ascending' }],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('insertion', 'ins_22339:GCTGGT');

    const resultWithLimitAndOffset = await lapisClient.postNucleotideInsertions1({
      nucleotideInsertionsRequest: {
        orderBy: [{ field: 'count', type: 'ascending' }],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should correctly handle nucleotide insertion requests in GET requests', async () => {
    const expectedFirstResultWithNucleotideInsertion = {
      count: 1,
      insertion: 'ins_25701:CCC',
    };

    const result = await lapisClient.postNucleotideInsertions1({
      nucleotideInsertionsRequest: {
        nucleotideInsertions: ['ins_25701:CC?'],
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data).to.contain.deep.equal(expectedFirstResultWithNucleotideInsertion);
  });

  it('should correctly handle amino acid insertion requests in GET requests', async () => {
    const expectedFirstResultWithAminoAcidInsertion = {
      count: 1,
      insertion: 'ins_25701:CC?',
    };

    const result = await lapisClient.postNucleotideInsertions1({
      nucleotideInsertionsRequest: {
        aminoAcidInsertions: ['ins_S:214:E?E'],
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data).to.contain.deep.equal(expectedFirstResultWithAminoAcidInsertion);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/nucleotideInsertions?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
insertion,count
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
ins_5959:TAT,1
ins_22339:GCTGGT,1
ins_22204:CAGAA,1
ins_25701:CCC,17
`.trim()
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      dataFormat: 'tsv',
    });

    const result = await fetch(basePath + '/nucleotideInsertions?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
insertion	count
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
ins_5959:TAT	1
ins_22339:GCTGGT	1
ins_22204:CAGAA	1
ins_25701:CCC	17
    `.trim()
    );
  });

  it('should return the lapis data version in the response', async () => {
    const result = await fetch(basePath + '/nucleotideInsertions');

    expect(result.status).equals(200);
    expect(result.headers.get('lapis-data-version')).to.match(/\d{10}/);
  });
});
