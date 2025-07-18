import { expect } from 'chai';
import {
  basePath,
  basePathMultiSegmented,
  expectIsZstdEncoded,
  lapisMultiSegmentedSequenceController,
  lapisSingleSegmentedSequenceController,
} from './common';

describe('The /alignedNucleotideSequence endpoint', () => {
  describe('single segmented', () => {
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

    it('should throw an error for fasta header template when returning JSON', async () => {
      const urlParams = new URLSearchParams({
        dataFormat: 'JSON',
        fastaHeaderTemplate: '{primaryKey}{date}{something invalid}',
      });

      const response = await fetch(`${basePath}/sample/alignedNucleotideSequences?${urlParams}`);

      const body = await response.json();
      expect(response.status, body).to.equal(400);
      expect(body.error.detail).to.contain('fastaHeaderTemplate is only applicable for FASTA format');
    });

    it('should fill the fasta header template', async () => {
      const urlParams = new URLSearchParams({
        fastaHeaderTemplate: 'key={primaryKey}|{date}|{counTry}|{.segment}',
        primaryKey: 'key_1408408',
      });

      const response = await fetch(`${basePath}/sample/alignedNucleotideSequences?${urlParams}`);

      const text = await response.text();
      expect(response.status, text).to.equal(200);
      expect(text.split('\n')[0]).to.equal('>key=key_1408408|2021-03-18|Switzerland|main');
    });
  });

  describe('multi segmented', () => {
    it('should return aligned nucleotide sequences', async () => {
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

    it('should return all requested segments', async () => {
      const result = await lapisMultiSegmentedSequenceController.postAllAlignedNucleotideSequences({
        allNucleotideSequenceRequest: {
          country: 'Switzerland',
          dataFormat: 'JSON',
          orderBy: [{ field: 'primaryKey', type: 'ascending' }],
          segments: ['M', 'L'],
        },
      });

      expect(result).to.have.length(6);
      expect(result[0]).to.deep.equal({
        primaryKey: 'key_0',
        l: 'ACTCT',
        m: 'CGGG',
        s: undefined,
      });
    });

    it('should throw an error for fasta header template when returning JSON', async () => {
      const urlParams = new URLSearchParams({
        dataFormat: 'JSON',
        fastaHeaderTemplate: '{primaryKey}{date}{something invalid}',
      });

      const response = await fetch(
        `${basePathMultiSegmented}/sample/alignedNucleotideSequences/M?${urlParams}`
      );

      const body = await response.json();
      expect(response.status, body).to.equal(400);
      expect(body.error.detail).to.contain('fastaHeaderTemplate is only applicable for FASTA format');
    });

    it('should throw an error for fasta header template when returning all sequences JSON', async () => {
      const urlParams = new URLSearchParams({
        dataFormat: 'JSON',
        fastaHeaderTemplate: '{primaryKey}{date}{something invalid}',
      });

      const response = await fetch(
        `${basePathMultiSegmented}/sample/alignedNucleotideSequences?${urlParams}`
      );

      const body = await response.json();
      expect(response.status, body).to.equal(400);
      expect(body.error.detail).to.contain('fastaHeaderTemplate is only applicable for FASTA format');
    });

    it('should fill the fasta header template', async () => {
      const urlParams = new URLSearchParams({
        fastaHeaderTemplate: 'key={primaryKey}|{date}|{counTry}|{.segment}',
        primaryKey: 'key_0',
      });

      const response = await fetch(
        `${basePathMultiSegmented}/sample/alignedNucleotideSequences/M?${urlParams}`
      );

      const text = await response.text();
      expect(response.status, text).to.equal(200);
      expect(text.split('\n')[0]).to.equal('>key=key_0|2021-03-18|Switzerland|M');
    });

    it('should fill the fasta header template when getting all sequences', async () => {
      const urlParams = new URLSearchParams({
        fastaHeaderTemplate: 'key={primaryKey}|{date}|{counTry}|{.segment}',
        primaryKey: 'key_0',
      });

      const response = await fetch(
        `${basePathMultiSegmented}/sample/alignedNucleotideSequences?${urlParams}`
      );

      const text = await response.text();
      expect(response.status, text).to.equal(200);
      expect(text.split('\n')[0]).to.equal('>key=key_0|2021-03-18|Switzerland|L');
    });
  });
});
