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
});
