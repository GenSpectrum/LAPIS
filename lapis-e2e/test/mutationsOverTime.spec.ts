import { expect } from 'chai';
import { mutOverTimeClient } from './common';

describe('The /mutationsOverTime endpoint', () => {
  it('returns a response with the correct dimensions etc.', async () => {
    const result = await mutOverTimeClient.postNucleotideMutationsOverTime({
      mutationsOverTimeRequest: {
        filters: {
          country: 'Switzerland',
        },
        dateField: 'date',
        includeMutations: ['T51-', 'C13011T', 'C14120T', 'C241T'],
        dateRanges: [
          {
            dateFrom: '2020-06-01',
            dateTo: '2020-12-31',
          },
          {
            dateFrom: '2021-01-01',
            dateTo: '2021-05-31',
          },
          {
            dateFrom: '2021-06-01',
            dateTo: '2021-12-31',
          },
        ],
      },
    });
    expect(result.data.data).to.have.lengthOf(4);
    expect(result.data.data[0]).to.have.lengthOf(3);
    expect(result.data.dateRanges).to.have.lengthOf(3);
    expect(result.data.mutations).to.have.lengthOf(4);

    const c241t = result.data.data[3];
    expect(c241t).to.deep.equal([
      { count: 20, coverage: 20 },
      { count: 58, coverage: 62 },
      { count: 14, coverage: 14 },
    ]);
  });

  it('returns an empty response if no mutations are given', async () => {
    const result = await mutOverTimeClient.postNucleotideMutationsOverTime({
      mutationsOverTimeRequest: {
        filters: {
          country: 'Switzerland',
        },
        dateField: 'date',
        includeMutations: [],
        dateRanges: [
          {
            dateFrom: '2020-06-01',
            dateTo: '2020-12-31',
          },
          {
            dateFrom: '2021-01-01',
            dateTo: '2021-05-31',
          },
          {
            dateFrom: '2021-06-01',
            dateTo: '2021-12-31',
          },
        ],
      },
    });
    expect(result.data.data).to.have.lengthOf(0);
    expect(result.data.dateRanges).to.have.lengthOf(3);
    expect(result.data.mutations).to.have.lengthOf(0);
  });

  it('returns an empty response if no date ranges are given', async () => {
    const result = await mutOverTimeClient.postNucleotideMutationsOverTime({
      mutationsOverTimeRequest: {
        filters: {
          country: 'Switzerland',
        },
        dateField: 'date',
        includeMutations: ['T51-', 'C13011T', 'C14120T', 'C241T'],
        dateRanges: [],
      },
    });
    expect(result.data.data).to.have.lengthOf(0);
    expect(result.data.dateRanges).to.have.lengthOf(0);
    expect(result.data.mutations).to.have.lengthOf(4);
  });

  it('if downloadAsFile is true, the content disposition is set to attachment', async () => {
    const result = await mutOverTimeClient.postNucleotideMutationsOverTimeRaw({
      mutationsOverTimeRequest: {
        filters: {
          country: 'Switzerland',
        },
        includeMutations: [],
        dateRanges: [],
        dateField: 'date',
        downloadAsFile: true,
      },
    });
    expect(result.raw.headers.has('Content-Disposition')).is.true;
    expect(result.raw.headers.get('Content-Disposition')).contains('attachment');
  });

  it('if downloadFileBasename is set, it is present in the headers', async () => {
    const result = await mutOverTimeClient.postNucleotideMutationsOverTimeRaw({
      mutationsOverTimeRequest: {
        filters: {
          country: 'Switzerland',
        },
        includeMutations: [],
        dateRanges: [],
        dateField: 'date',
        downloadAsFile: true,
        downloadFileBasename: 'foobar',
      },
    });
    expect(result.raw.headers.has('Content-Disposition')).is.true;
    expect(result.raw.headers.get('Content-Disposition')).contains('attachment');
    expect(result.raw.headers.get('Content-Disposition')).contains('foobar');
  });
});
