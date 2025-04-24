import { expect } from 'chai';
import {
  basePath,
  expectIsZstdEncoded,
  lapisMultiSegmentedSequenceController,
  lapisSingleSegmentedSequenceController,
} from './common';

describe('The /alignedNucleotideSequence endpoint', () => {
  it('should return aligned nucleotide sequences for Switzerland', async () => {
    const result = await lapisSingleSegmentedSequenceController.postAlignedNucleotideSequence({
      nucleotideSequenceRequest: { country: 'Switzerland', dataFormat: 'JSON' },
    });

    expect(result).to.have.length(100);
    result.sort((a: { primaryKey: string }, b: { primaryKey: string }) =>
      a.primaryKey.localeCompare(b.primaryKey)
    );
    expect(result[0].primaryKey).to.equal('key_1001493');
    expect(result[0].main).to.have.length(29903);
  });

  it('should return aligned nucleotide sequences for multi segmented sequences', async () => {
    const result = await lapisMultiSegmentedSequenceController.postAlignedNucleotideSequence({
      nucleotideSequenceRequest: { country: 'Switzerland', dataFormat: 'JSON' },
      segment: 'M',
    });

    expect(result).to.have.length(6);
    result.sort((a: { primaryKey: string }, b: { primaryKey: string }) =>
      a.primaryKey.localeCompare(b.primaryKey)
    );
    expect(result[0].primaryKey).to.equal('key_0');
    expect(result[0].m).to.equal('CGGG');
});

  it('should order ascending by specified fields', async () => {
    const result = await lapisSingleSegmentedSequenceController.postAlignedNucleotideSequence({
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
    const result = await lapisSingleSegmentedSequenceController.postAlignedNucleotideSequence({
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
    const resultWithLimit = await lapisSingleSegmentedSequenceController.postAlignedNucleotideSequence({
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
      await lapisSingleSegmentedSequenceController.postAlignedNucleotideSequence({
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
    const result = await lapisSingleSegmentedSequenceController.postAlignedNucleotideSequence({
      nucleotideSequenceRequest: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
        dataFormat: 'JSON',
      },
    });

    expect(result).to.have.length(1);
    expect(result[0].primaryKey).to.equal('key_3578231');
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const result = await lapisSingleSegmentedSequenceController.postAlignedNucleotideSequence({
      nucleotideSequenceRequest: {
        aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
        dataFormat: 'JSON',
      },
    });

    expect(result).to.have.length(1);
    expect(result[0].primaryKey).to.equal('key_3259931');
  });

  it('should return an empty zstd compressed file', async () => {
    const urlParams = new URLSearchParams({
      compression: 'zstd',
      primaryKey: 'something so that no data will be returned',
    });

    const response = await fetch(basePath + '/sample/alignedNucleotideSequences?' + urlParams.toString());

    expectIsZstdEncoded(await response.arrayBuffer());
  });
});
