import { expect } from 'chai';
import { basePath, lapisClient } from './common';

describe('The /aminoAcidInsertions endpoint', () => {
  let someInsertion = 'ins_S:214:EPE';

  it('should return amino acid insertions for Switzerland', async () => {
    const result = await lapisClient.postAminoAcidInsertions1({
      insertionsRequest: { country: 'Switzerland' },
    });

    expect(result.data).to.have.length(6);

    const specificInsertion = result.data.find(insertionData => insertionData.insertion === someInsertion);
    expect(specificInsertion?.count).to.equal(5);
    expect(specificInsertion?.insertedSymbols).to.equal('EPE');
    expect(specificInsertion?.position).to.equal(214);
    expect(specificInsertion?.sequenceName).to.equal('S');
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postAminoAcidInsertions1({
      insertionsRequest: {
        orderBy: [{ field: 'count', type: 'ascending' }],
      },
    });

    expect(ascendingOrderedResult.data[0]).to.have.property('insertion', 'ins_ORF1a:3602:F');

    const descendingOrderedResult = await lapisClient.postAminoAcidInsertions1({
      insertionsRequest: {
        orderBy: [{ field: 'count', type: 'descending' }],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('insertion', 'ins_S:214:EPE');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postAminoAcidInsertions1({
      insertionsRequest: {
        orderBy: [{ field: 'count', type: 'ascending' }],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('insertion', 'ins_ORF1a:3602:FEP');

    const resultWithLimitAndOffset = await lapisClient.postAminoAcidInsertions1({
      insertionsRequest: {
        orderBy: [{ field: 'count', type: 'ascending' }],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should correctly handle nucleotide insertion requests', async () => {
    const result = await lapisClient.postAminoAcidInsertions1({
      insertionsRequest: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
      },
    });

    expect(result.data).to.have.length(0);
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const expectedFirstResultWithAminoAcidInsertion = {
      insertion: 'ins_ORF1a:3602:FEP',
      count: 1,
      insertedSymbols: 'FEP',
      position: 3602,
      sequenceName: 'ORF1a',
    };

    const result = await lapisClient.postAminoAcidInsertions1({
      insertionsRequest: {
        aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
      },
    });

    expect(result.data).to.have.length(2);
    expect(result.data[0]).to.deep.equal(expectedFirstResultWithAminoAcidInsertion);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      dataFormat: 'csv',
      orderBy: 'position',
    });

    const result = await fetch(basePath + '/sample/aminoAcidInsertions?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
insertion,count,insertedSymbols,position,sequenceName
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
ins_S:143:T,1,T,143,S
ins_S:210:IV,1,IV,210,S
ins_S:214:EPE,5,EPE,214,S
ins_S:247:SGE,1,SGE,247,S
ins_ORF1a:3602:F,1,F,3602,ORF1a
ins_ORF1a:3602:FEP,1,FEP,3602,ORF1a
`.trim()
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      dataFormat: 'tsv',
      orderBy: 'position',
    });

    const result = await fetch(basePath + '/sample/aminoAcidInsertions?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
insertion	count	insertedSymbols	position	sequenceName
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
ins_S:143:T	1	T	143	S
ins_S:210:IV	1	IV	210	S
ins_S:214:EPE	5	EPE	214	S
ins_S:247:SGE	1	SGE	247	S
ins_ORF1a:3602:F	1	F	3602	ORF1a
ins_ORF1a:3602:FEP	1	FEP	3602	ORF1a
    `.trim()
    );
  });
});
