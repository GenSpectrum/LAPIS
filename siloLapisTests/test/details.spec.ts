import { expect } from 'chai';
import { lapisClient } from './common';
import fs from 'fs';
import { SequenceFilters } from './lapisClient';

describe('The /details endpoint', () => {
  it('should return details with specified fields', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        pangoLineage: 'B.1.617.2',
        fields: ['pango_lineage', 'division'],
      },
    });

    expect(result).to.have.length(2);
    expect(result[0]).to.be.deep.equal({
      age: undefined,
      country: undefined,
      date: undefined,
      division: 'Aargau',
      gisaidEpiIsl: undefined,
      pangoLineage: 'B.1.617.2',
      qcValue: undefined,
      region: undefined,
    });
  });

  it('should return details with all fields when no explicit fields were specified', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        pangoLineage: 'B.1.617.2',
      },
    });

    expect(result).to.have.length(2);
    expect(result[0]).to.be.deep.equal({
      age: 50,
      country: 'Switzerland',
      date: '2021-07-19',
      division: 'Aargau',
      gisaidEpiIsl: 'EPI_ISL_3128811',
      pangoLineage: 'B.1.617.2',
      qcValue: 0.9,
      region: 'Europe',
    });
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'division', type: 'ascending' }],
        fields: ['division'],
      },
    });

    expect(ascendingOrderedResult[0]).to.have.property('division', 'Aargau');

    const descendingOrderedResult = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'division', type: 'descending' }],
        fields: ['division'],
      },
    });

    expect(descendingOrderedResult[0]).to.have.property('division', 'Zürich');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'gisaid_epi_isl', type: 'ascending' }],
        fields: ['gisaid_epi_isl'],
        limit: 2,
      },
    });

    expect(resultWithLimit).to.have.length(2);
    expect(resultWithLimit[1]).to.have.property('gisaidEpiIsl', 'EPI_ISL_1001920');

    const resultWithLimitAndOffset = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'gisaid_epi_isl', type: 'ascending' }],
        fields: ['gisaid_epi_isl'],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset).to.have.length(2);
    expect(resultWithLimitAndOffset[0]).to.deep.equal(resultWithLimit[1]);
  });
});
