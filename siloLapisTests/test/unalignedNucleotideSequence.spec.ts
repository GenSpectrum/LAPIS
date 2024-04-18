import { expect } from 'chai';
import { basePath, lapisSingleSegmentedSequenceController, sequenceData } from './common';

describe('The /unalignedNucleotideSequence endpoint', () => {
  it('should return unaligned nucleotide sequences for Switzerland', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: { country: 'Switzerland' },
    });

    const { primaryKeys, sequences } = sequenceData(result);

    expect(primaryKeys, 'primaryKeys').to.have.length(100);
    expect(sequences, 'sequences').to.have.length(100);
    expect(primaryKeys[0]).to.equal('>key_3259931');
    expect(sequences[0]).to.have.length(29903);
  });

  it('should order ascending by specified fields', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: { orderBy: [{ field: 'primaryKey', type: 'ascending' }] },
    });

    const { primaryKeys, sequences } = sequenceData(result);

    expect(primaryKeys).to.have.length(100);
    expect(sequences).to.have.length(100);
    expect(primaryKeys[0]).to.equal('>key_1001493');
    expect(sequences[0]).to.have.length(29903);
  });

  it('should order descending by specified fields', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: { orderBy: [{ field: 'primaryKey', type: 'descending' }] },
    });

    const { primaryKeys, sequences } = sequenceData(result);

    expect(primaryKeys).to.have.length(100);
    expect(sequences).to.have.length(100);
    expect(primaryKeys[0]).to.equal('>key_931279');
    expect(sequences[0]).to.have.length(29903);
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: {
        orderBy: [{ field: 'primaryKey', type: 'ascending' }],
        limit: 2,
      },
    });

    const { primaryKeys: primaryKeysWithLimit, sequences: sequencesWithLimit } =
      sequenceData(resultWithLimit);

    expect(primaryKeysWithLimit).to.have.length(2);
    expect(sequencesWithLimit).to.have.length(2);
    expect(primaryKeysWithLimit[0]).to.equal('>key_1001493');
    expect(sequencesWithLimit[0]).to.have.length(29903);

    const resultWithLimitAndOffset =
      await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
        nucleotideSequenceRequest: {
          orderBy: [{ field: 'primaryKey', type: 'ascending' }],
          limit: 2,
          offset: 1,
        },
      });

    const { primaryKeys: primaryKeysWithLimitAndOffset, sequences: sequencesWithLimitAndOffset } =
      sequenceData(resultWithLimitAndOffset);

    expect(primaryKeysWithLimitAndOffset).to.have.length(2);
    expect(sequencesWithLimitAndOffset).to.have.length(2);
    expect(primaryKeysWithLimitAndOffset[0]).to.equal(primaryKeysWithLimit[1]);
    expect(sequencesWithLimitAndOffset[0]).to.equal(sequencesWithLimit[1]);
  });

  it('should correctly handle nucleotide insertion requests', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
      },
    });

    const { primaryKeys, sequences } = sequenceData(result);

    expect(primaryKeys).to.have.length(1);
    expect(sequences).to.have.length(1);
    expect(primaryKeys[0]).to.equal('>key_3578231');
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: {
        aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
      },
    });

    const { primaryKeys, sequences } = sequenceData(result);

    expect(primaryKeys).to.have.length(1);
    expect(sequences).to.have.length(1);
    expect(primaryKeys[0]).to.equal('>key_3259931');
  });

  it('should return the short sequence', async () => {
    const result = await lapisSingleSegmentedSequenceController.postUnalignedNucleotideSequence({
      nucleotideSequenceRequest: { primaryKey: 'key_1749899' },
    });

    const { primaryKeys, sequences } = sequenceData(result);

    expect(primaryKeys).to.have.length(1);
    expect(sequences).to.have.length(1);
    expect(primaryKeys[0]).to.equal('>key_1749899');
    expect(sequences[0]).to.equal('some_very_short_string');
  });
});
