import { expect } from 'chai';
import { queriesOverTimeClient } from './common';

describe('The /mutationsOverTime endpoint', () => {
  it('should return a correct queriesOverTime response', async () => {
    const result = await queriesOverTimeClient.postQueriesOverTime({
      queriesOverTimeRequest: {
        filters: {
          country: 'Switzerland',
        },
        queries: [
          { countQuery: 'C27972T | C26735T', coverageQuery: '!27972N & !26735N', displayLabel: 'my label' },
          { countQuery: 'C27972T & C26735T', coverageQuery: '!27972N & !26735N' },
        ],
        dateRanges: [
          { dateFrom: '2020-01-01', dateTo: '2020-12-31' },
          { dateFrom: '2021-01-01', dateTo: '2021-12-31' },
          { dateFrom: '2022-01-01', dateTo: '2022-12-31' },
        ],
        dateField: 'date',
      },
    });

    expect(result.data).to.deep.equal({
      queries: ['my label', 'C27972T & C26735T'],
      dateRanges: [
        { dateFrom: new Date('2020-01-01'), dateTo: new Date('2020-12-31') },
        { dateFrom: new Date('2021-01-01'), dateTo: new Date('2021-12-31') },
        { dateFrom: new Date('2022-01-01'), dateTo: new Date('2022-12-31') },
      ],
      data: [
        [
          { count: 6, coverage: 22 },
          { count: 52, coverage: 73 },
          { count: 0, coverage: 0 },
        ],
        [
          { count: 0, coverage: 22 },
          { count: 1, coverage: 73 },
          { count: 0, coverage: 0 },
        ],
      ],
      totalCountsByDateRange: [22, 77, 0],
      overallStatisticsByQuery: [
        { count: 58, coverage: 95, proportion: 58 / 95 },
        { count: 1, coverage: 95, proportion: 1 / 95 },
      ],
    });
  });

  it('returns a response with the correct dimensions etc.', async () => {
    const result = await queriesOverTimeClient.postNucleotideMutationsOverTime({
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

    expect(result.data.overallStatisticsByMutation).to.have.lengthOf(4);
    const c241tStats = result.data.overallStatisticsByMutation![3];
    expect(c241tStats).to.deep.equal({
      count: 92,
      coverage: 96,
      proportion: 92 / 96,
    });
  });

  it('returns an empty response if no mutations are given', async () => {
    const result = await queriesOverTimeClient.postNucleotideMutationsOverTime({
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
    expect(result.data.overallStatisticsByMutation).to.have.lengthOf(0);
  });

  it('returns an empty response if no date ranges are given', async () => {
    const result = await queriesOverTimeClient.postNucleotideMutationsOverTime({
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
    expect(result.data.overallStatisticsByMutation).to.have.lengthOf(0);
    expect(result.info.dataVersion).to.exist;
  });

  it('if downloadAsFile is true, the content disposition is set to attachment', async () => {
    const result = await queriesOverTimeClient.postNucleotideMutationsOverTimeRaw({
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
    const result = await queriesOverTimeClient.postNucleotideMutationsOverTimeRaw({
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
