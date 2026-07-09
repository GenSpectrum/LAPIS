import { expect } from 'chai';
import {
  aminoAcidCoOccurrenceClient,
  basePath,
  expectIsGzipEncoded,
  multiSegmentedCoOccurrenceClient,
  singleSegmentedCoOccurrenceClient,
} from './common';
import { Gene } from './lapisClient';
import { Segment } from './lapisClientMultiSegmented';

// Country Switzerland has 100 sequences in total (see aggregatedQueries/aggregrationFields.json).
// A combination's count can never exceed that, regardless of how many sequences have ambiguous
// reads at the requested position(s) (those just end up in their own combination, e.g. with 'N').
const maxPossibleSwitzerlandCount = 100;

describe('The /nucleotideCoOccurrence endpoint (single segmented)', () => {
  it('should return the co-occurrence of symbols at a single position', async () => {
    const result = await singleSegmentedCoOccurrenceClient.postSingleSegmentNucleotideCoOccurrence({
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [28280],
      },
    });

    const totalCount = result.data.reduce((sum, entry) => sum + entry.count, 0);
    expect(totalCount).to.be.at.most(maxPossibleSwitzerlandCount);

    const mutatedCombination = result.data.find(entry => entry['main:28280'] === 'C');
    expect(mutatedCombination?.count).to.equal(51);
  });

  it('should return the co-occurrence of symbols at a range of positions', async () => {
    const result = await singleSegmentedCoOccurrenceClient.postSingleSegmentNucleotideCoOccurrence({
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [{ from: 28279, to: 28281 }],
      },
    });

    expect(result.data.length).to.be.greaterThan(0);

    result.data.forEach(entry => {
      expect(entry).to.have.property('main:28279');
      expect(entry).to.have.property('main:28280');
      expect(entry).to.have.property('main:28281');
    });
  });

  it('should return the co-occurrence of symbols at multiple discrete positions', async () => {
    const result = await singleSegmentedCoOccurrenceClient.postSingleSegmentNucleotideCoOccurrence({
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [19220, 28280],
      },
    });

    expect(result.data.length).to.be.greaterThan(0);

    result.data.forEach(entry => {
      expect(entry).to.have.property('main:19220');
      expect(entry).to.have.property('main:28280');
    });
  });

  it('should support GET requests', async () => {
    const urlParams = new URLSearchParams({ country: 'Switzerland', positions: '28280' });
    const response = await fetch(basePath + '/component/nucleotideCoOccurrence?' + urlParams.toString());

    expect(response.status).to.equal(200);
    const resultJson = await response.json();

    const mutatedCombination = resultJson.data.find(
      (entry: { 'main:28280': string }) => entry['main:28280'] === 'C'
    );
    expect(mutatedCombination?.count).to.equal(51);
  });

  it('should order by count descending and respect limit', async () => {
    const result = await singleSegmentedCoOccurrenceClient.postSingleSegmentNucleotideCoOccurrence({
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [28280],
        limit: 1,
        orderBy: [{ field: 'count', type: 'descending' }],
      },
    });

    expect(result.data).to.have.lengthOf(1);
    expect(result.data[0]['main:28280']).to.equal('C');
    expect(result.data[0].count).to.equal(51);
  });

  it('should order by the relabeled position field', async () => {
    const result = await singleSegmentedCoOccurrenceClient.postSingleSegmentNucleotideCoOccurrence({
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [28280],
        orderBy: [{ field: 'main:28280', type: 'ascending' }],
      },
    });

    const values = result.data.map(entry => entry['main:28280']);
    expect(values).to.deep.equal([...values].sort());
  });

  it('should return a 400 error when positions is missing', async () => {
    const result = await fetch(basePath + '/component/nucleotideCoOccurrence', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ country: 'Switzerland' }),
    });

    expect(result.status).to.equal(400);
  });

  it('should return a 404 for a segment path when the genome is single segmented', async () => {
    const result = await fetch(basePath + '/component/nucleotideCoOccurrence/main?positions=1');

    expect(result.status).to.equal(404);
  });

  it('if downloadAsFile is true, the content disposition is set to attachment', async () => {
    const result = await singleSegmentedCoOccurrenceClient.postSingleSegmentNucleotideCoOccurrenceRaw({
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [28280],
        downloadAsFile: true,
      },
    });

    expect(result.raw.headers.has('Content-Disposition')).is.true;
    expect(result.raw.headers.get('Content-Disposition')).contains('attachment');
    expect(result.raw.headers.get('Content-Disposition')).contains('nucleotideCoOccurrence');
  });

  it('should return gzip compressed data when requested', async () => {
    const response = await fetch(basePath + '/component/nucleotideCoOccurrence', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ country: 'Switzerland', positions: [28280], compression: 'gzip' }),
    });

    expect(response.status).to.equal(200);
    expect(response.headers.get('content-type')).to.equal('application/gzip');
    expectIsGzipEncoded(await response.arrayBuffer());
  });
});

describe('The /nucleotideCoOccurrence endpoint (multi segmented)', () => {
  it('should return the co-occurrence of symbols for a segment', async () => {
    const result = await multiSegmentedCoOccurrenceClient.postNucleotideCoOccurrence({
      segment: Segment.L,
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [1],
      },
    });

    const mutatedCombination = result.data.find(entry => entry['L:1'] === 'A');
    expect(mutatedCombination?.count).to.equal(2);
  });

  it('should return the co-occurrence of symbols for a different segment', async () => {
    const result = await multiSegmentedCoOccurrenceClient.postNucleotideCoOccurrence({
      segment: Segment.M,
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [1],
      },
    });

    const mutatedCombination = result.data.find(entry => entry['M:1'] === 'C');
    expect(mutatedCombination?.count).to.equal(1);
  });
});

describe('The /aminoAcidCoOccurrence endpoint', () => {
  it('should return the co-occurrence of symbols at a single position', async () => {
    const result = await aminoAcidCoOccurrenceClient.postAminoAcidCoOccurrence({
      gene: Gene.S,
      coOccurrenceRequest: {
        country: 'Switzerland',
        positions: [478],
      },
    });

    const totalCount = result.data.reduce((sum, entry) => sum + entry.count, 0);
    expect(totalCount).to.be.at.most(maxPossibleSwitzerlandCount);

    const mutatedCombination = result.data.find(entry => entry['S:478'] === 'K');
    expect(mutatedCombination?.count).to.equal(69);
  });

  it('should return a 400 error when the position range is invalid', async () => {
    const result = await fetch(basePath + '/component/aminoAcidCoOccurrence/S', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ country: 'Switzerland', positions: [{ from: 10, to: 1 }] }),
    });

    expect(result.status).to.equal(400);
  });
});
