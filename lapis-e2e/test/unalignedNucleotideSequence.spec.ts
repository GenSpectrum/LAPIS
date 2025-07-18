import { expect } from 'chai';
import {
  basePath,
  basePathMultiSegmented,
  lapisMultiSegmentedSequenceController,
  lapisSingleSegmentedSequenceController,
} from './common';

describe('The /unalignedNucleotideSequence endpoint', () => {
  describe('single segmented', () => {
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

    it('should throw an error for fasta header template when returning JSON', async () => {
      const urlParams = new URLSearchParams({
        dataFormat: 'JSON',
        fastaHeaderTemplate: '{primaryKey}{date}{something invalid}',
      });

      const response = await fetch(`${basePath}/sample/unalignedNucleotideSequences?${urlParams}`);

      const body = await response.json();
      expect(response.status, body).to.equal(400);
      expect(body.error.detail).to.contain('fastaHeaderTemplate is only applicable for FASTA format');
    });

    it('should fill the fasta header template', async () => {
      const urlParams = new URLSearchParams({
        fastaHeaderTemplate: 'key={primaryKey}|{date}|{counTry}|{.segment}',
        primaryKey: 'key_1408408',
      });

      const response = await fetch(`${basePath}/sample/unalignedNucleotideSequences?${urlParams}`);

      const text = await response.text();
      expect(response.status, text).to.equal(200);
      expect(text.split('\n')[0]).to.equal('>key=key_1408408|2021-03-18|Switzerland|main');
    });
  });

  describe('multi segmented', () => {
    it('should return multi segmented unaligned sequences', async () => {
      const result = await lapisMultiSegmentedSequenceController.postUnalignedNucleotideSequence({
        segment: 'L',
        nucleotideSequenceRequest: { primaryKey: 'key_0', dataFormat: 'JSON' },
      });

      expect(result).to.have.length(1);
      expect(result[0].primaryKey).to.equal('key_0');
      expect(result[0].l).to.equal('ACNTCT');
    });

    it('should return all requested segments', async () => {
      const result = await lapisMultiSegmentedSequenceController.postAllUnalignedNucleotideSequences({
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
        l: 'ACNTCT',
        m: 'NCGGG',
        s: undefined,
      });
      expect(result[1]).to.deep.equal({
        primaryKey: 'key_1',
        l: 'NACTCT',
        m: undefined,
        s: undefined,
      });
      expect(result[2]).to.deep.equal({
        primaryKey: 'key_2',
        l: undefined,
        m: undefined,
        s: undefined,
      });
    });

    it('should throw an error for fasta header template when returning JSON', async () => {
      const urlParams = new URLSearchParams({
        dataFormat: 'JSON',
        fastaHeaderTemplate: '{primaryKey}{date}{something invalid}',
      });

      const response = await fetch(
        `${basePathMultiSegmented}/sample/unalignedNucleotideSequences/L?${urlParams}`
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
        `${basePathMultiSegmented}/sample/unalignedNucleotideSequences?${urlParams}`
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
        `${basePathMultiSegmented}/sample/unalignedNucleotideSequences/M?${urlParams}`
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
        `${basePathMultiSegmented}/sample/unalignedNucleotideSequences?${urlParams}`
      );

      const text = await response.text();
      expect(response.status, text).to.equal(200);
      expect(text.split('\n')[0]).to.equal('>key=key_0|2021-03-18|Switzerland|L');
    });
  });
});
