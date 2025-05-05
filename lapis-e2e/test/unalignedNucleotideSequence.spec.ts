import { expect } from 'chai';
import { lapisSingleSegmentedSequenceController } from './common';

describe('The /unalignedNucleotideSequence endpoint', () => {
  it('should return unaligned nucleotide sequences for Switzerland', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: { country: 'Switzerland', dataFormat: 'JSON' },
    });

    expect(result).to.have.length(100);
    result.sort((a: { primaryKey: string }, b: { primaryKey: string }) =>
      a.primaryKey.localeCompare(b.primaryKey)
    );
    expect(result[0].primaryKey).to.equal('key_1001493');
    expect(result[0].main).to.have.length(29903);
  });

  it('should order ascending by specified fields', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: {
        orderBy: [{ field: 'primaryKey', type: 'ascending' }],
        dataFormat: 'JSON',
      },
    });

    expect(result).to.have.length(100);
    expect(result[0].primaryKey).to.equal('key_1001493');
    expect(result[0].main).to.have.length(29903);
  });

  it('should order descending by specified fields', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: {
        orderBy: [{ field: 'primaryKey', type: 'descending' }],
        dataFormat: 'JSON',
      },
    });

    expect(result).to.have.length(100);
    expect(result[0].primaryKey).to.equal('key_931279');
    expect(result[0].main).to.have.length(29903);
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: {
        orderBy: [{ field: 'primaryKey', type: 'ascending' }],
        limit: 2,
        dataFormat: 'JSON',
      },
    });
    expect(resultWithLimit).to.have.length(2);
    expect(resultWithLimit[0].primaryKey).to.equal('key_1001493');
    expect(resultWithLimit[0].main).to.have.length(29903);

    const resultWithLimitAndOffset =
      await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
        nucleotideSequenceRequest: {
          orderBy: [{ field: 'primaryKey', type: 'ascending' }],
          limit: 2,
          offset: 1,
          dataFormat: 'JSON',
        },
      });
    expect(resultWithLimitAndOffset).to.have.length(2);
    expect(resultWithLimitAndOffset[0]).to.deep.equal(resultWithLimit[1]);
  });

  it('should correctly handle nucleotide insertion requests', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
        dataFormat: 'JSON',
      },
    });
    expect(result).to.have.length(1);
    expect(result[0].primaryKey).to.equal('key_3578231');
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: {
        aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
        dataFormat: 'JSON',
      },
    });
    expect(result).to.have.length(1);
    expect(result[0].primaryKey).to.equal('key_3259931');
  });

  it('should return the short sequence', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: { primaryKey: 'key_1749899', dataFormat: 'JSON' },
    });

    expect(result).to.have.length(1);
    expect(result[0].primaryKey).to.equal('key_1749899');
    expect(result[0].main).to.equal('some_very_short_string');
  });
});
