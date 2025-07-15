import { expect } from 'chai';
import { basePath, lapisClient } from './common';

describe('The /alignedAminoAcidSequence endpoint', () => {
  describe('when getting a single sequence', () => {
    it('should return amino acid sequences for Switzerland', async () => {
      const result = await lapisClient.postAlignedAminoAcidSequence({
        gene: 'S',
        aminoAcidSequenceRequest: { country: 'Switzerland', dataFormat: 'JSON' },
      });

      expect(result).to.have.length(100);

      result.sort((a: { primaryKey: string }, b: { primaryKey: string }) =>
        a.primaryKey.localeCompare(b.primaryKey)
      );
      expect(result[0].primaryKey).to.equal('key_1001493');
      expect(result[0].s).to.have.length(1274);
    });

    it('should order ascending by specified fields', async () => {
      const result = await lapisClient.postAlignedAminoAcidSequence({
        gene: 'S',
        aminoAcidSequenceRequest: {
          orderBy: [{ field: 'primaryKey', type: 'ascending' }],
          dataFormat: 'JSON',
        },
      });

      expect(result).to.have.length(100);
      expect(result[0].primaryKey).to.equal('key_1001493');
      expect(result[0].s).to.have.length(1274);
    });

    it('should order descending by specified fields', async () => {
      const result = await lapisClient.postAlignedAminoAcidSequence({
        gene: 'S',
        aminoAcidSequenceRequest: {
          orderBy: [{ field: 'primaryKey', type: 'descending' }],
          dataFormat: 'JSON',
        },
      });

      expect(result).to.have.length(100);
      expect(result[0].primaryKey).to.equal('key_931279');
      expect(result[0].s).to.have.length(1274);
    });

    it('should apply limit and offset', async () => {
      const resultWithLimit = await lapisClient.postAlignedAminoAcidSequence({
        gene: 'S',
        aminoAcidSequenceRequest: {
          orderBy: [{ field: 'primaryKey', type: 'ascending' }],
          limit: 2,
          dataFormat: 'JSON',
        },
      });

      expect(resultWithLimit).to.have.length(2);
      expect(resultWithLimit[0].primaryKey).to.equal('key_1001493');
      expect(resultWithLimit[0].s).to.have.length(1274);

      const resultWithLimitAndOffset = await lapisClient.postAlignedAminoAcidSequence({
        gene: 'S',
        aminoAcidSequenceRequest: {
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
      const result = await lapisClient.postAlignedAminoAcidSequence({
        gene: 'S',
        aminoAcidSequenceRequest: {
          nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
          dataFormat: 'JSON',
        },
      });

      expect(result).to.have.length(1);
      expect(result[0].primaryKey).to.equal('key_3578231');
      expect(result[0].s).to.have.length(1274);
    });

    it('should correctly handle amino acid insertion requests', async () => {
      const result = await lapisClient.postAlignedAminoAcidSequence({
        gene: 'S',
        aminoAcidSequenceRequest: {
          aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
          dataFormat: 'JSON',
        },
      });

      expect(result).to.have.length(1);
      expect(result[0].primaryKey).to.equal('key_3259931');
    });

    it('should throw an error for fasta header template when returning JSON', async () => {
      const urlParams = new URLSearchParams({
        dataFormat: 'JSON',
        fastaHeaderTemplate: '{primaryKey}{date}{something invalid}',
      });

      const response = await fetch(`${basePath}/sample/alignedAminoAcidSequences/S?${urlParams}`);

      const body = await response.json();
      expect(response.status, body).to.equal(400);
      expect(body.error.detail).to.contain('fastaHeaderTemplate is only applicable for FASTA format');
    });

    it('should fill the fasta header template', async () => {
      const urlParams = new URLSearchParams({
        fastaHeaderTemplate: 'key={primaryKey}|{date}|{country}|{.gene}',
        primaryKey: 'key_1408408',
      });

      const response = await fetch(`${basePath}/sample/alignedAminoAcidSequences/S?${urlParams}`);

      expect(response.status).to.equal(200);
      const text = await response.text();
      expect(text.split('\n')[0]).to.equal('>key=key_1408408|2021-03-18|Switzerland|S');
    });
  });

  describe('when getting all sequences', () => {
    it('should return amino acid sequences for Switzerland', async () => {
      const result = await lapisClient.postAllAlignedAminoAcidSequences({
        allAminoAcidSequenceRequest: { country: 'Switzerland', dataFormat: 'JSON' },
      });

      expect(result).to.have.length(100);

      result.sort((a: { primaryKey: string }, b: { primaryKey: string }) =>
        a.primaryKey.localeCompare(b.primaryKey)
      );
      expect(result[0].primaryKey).to.equal('key_1001493');
      expect(result[0].e).to.have.length(76);
      expect(result[0].m).to.have.length(223);
      expect(result[0].n).to.have.length(420);
      expect(result[0].oRF1a).to.have.length(4401);
      expect(result[0].oRF1b).to.have.length(2696);
      expect(result[0].oRF3a).to.have.length(276);
      expect(result[0].oRF6).to.have.length(62);
      expect(result[0].oRF7a).to.have.length(122);
      expect(result[0].oRF7b).to.have.length(44);
      expect(result[0].oRF8).to.have.length(122);
      expect(result[0].oRF9b).to.have.length(98);
      expect(result[0].s).to.have.length(1274);
    });

    it('should return only the requested amino acid sequences for Switzerland', async () => {
      const result = await lapisClient.postAllAlignedAminoAcidSequences({
        allAminoAcidSequenceRequest: { country: 'Switzerland', dataFormat: 'JSON', genes: ['E', 'ORF1a'] },
      });

      expect(result).to.have.length(100);

      result.sort((a: { primaryKey: string }, b: { primaryKey: string }) =>
        a.primaryKey.localeCompare(b.primaryKey)
      );
      expect(result[0].primaryKey).to.equal('key_1001493');
      expect(result[0].e).to.have.length(76);
      expect(result[0].m).to.be.undefined;
      expect(result[0].n).to.be.undefined;
      expect(result[0].oRF1a).to.have.length(4401);
      expect(result[0].oRF1b).to.be.undefined;
      expect(result[0].oRF3a).to.be.undefined;
      expect(result[0].oRF6).to.be.undefined;
      expect(result[0].oRF7a).to.be.undefined;
      expect(result[0].oRF7b).to.be.undefined;
      expect(result[0].oRF8).to.be.undefined;
      expect(result[0].oRF9b).to.be.undefined;
      expect(result[0].s).to.be.undefined;
    });

    it('should return bad request for unknown gene', async () => {
      const urlParams = new URLSearchParams({
        genes: 'unknownGene',
      });

      const response = await fetch(`${basePath}/sample/alignedAminoAcidSequences?${urlParams}`);

      expect(response.status).to.equal(400);

      const errorResponse = await response.json();
      expect(errorResponse.error.detail).to.match(
        /Error from SILO: The table does not contain the SequenceColumn 'unknownGene'/
      );
    });

    it('should throw an error for fasta header template when returning JSON', async () => {
      const urlParams = new URLSearchParams({
        dataFormat: 'JSON',
        fastaHeaderTemplate: '{primaryKey}{date}{something invalid}',
      });

      const response = await fetch(`${basePath}/sample/alignedAminoAcidSequences?${urlParams}`);

      const body = await response.json();
      expect(response.status, body).to.equal(400);
      expect(body.error.detail).to.contain('fastaHeaderTemplate is only applicable for FASTA format');
    });

    it('should fill the fasta header template', async () => {
      const urlParams = new URLSearchParams({
        fastaHeaderTemplate: 'key={primaryKey}|{date}|{counTry}|{.gene}',
        primaryKey: 'key_1408408',
      });

      const response = await fetch(`${basePath}/sample/alignedAminoAcidSequences?${urlParams}`);

      const text = await response.text();
      expect(response.status, text).to.equal(200);
      expect(text.split('\n')[0]).to.equal('>key=key_1408408|2021-03-18|Switzerland|E');
    });
  });
});
