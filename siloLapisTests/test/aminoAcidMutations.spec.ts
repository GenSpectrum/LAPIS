import { expect } from 'chai';
import { basePath, lapisClient } from './common';

describe('The /aminoAcidMutations endpoint', () => {
  let mutationWithLessThan10PercentProportion = 'N:D3L';
  let mutationWithMoreThan50PercentProportion = 'S:T478K';

  it('should return mutation proportions for Switzerland', async () => {
    const result = await lapisClient.postAminoAcidMutations({
      sequenceFiltersWithMinProportion: { country: 'Switzerland' },
    });

    expect(result.data).to.have.length(107);

    const rareMutationProportion = result.data.find(
      mutationData => mutationData.mutation === mutationWithLessThan10PercentProportion
    );
    expect(rareMutationProportion?.count).to.equal(7);
    expect(rareMutationProportion?.proportion).to.be.approximately(0.07142857142857142, 0.0001);

    const commonMutationProportion = result.data.find(
      mutationProportion => mutationProportion.mutation === mutationWithMoreThan50PercentProportion
    );
    expect(commonMutationProportion?.count).to.equal(69);
    expect(commonMutationProportion?.proportion).to.be.approximately(0.7340425531914894, 0.0001);
  });

  it('should return mutation proportions for Switzerland with minProportion 0.5', async () => {
    const result = await lapisClient.postAminoAcidMutations({
      sequenceFiltersWithMinProportion: {
        country: 'Switzerland',
        minProportion: 0.5,
      },
    });

    expect(result.data).to.have.length(4);

    const mutationsAboveThreshold = result.data.map(mutationData => mutationData.mutation);
    expect(mutationsAboveThreshold).to.contain(mutationWithMoreThan50PercentProportion);
    expect(mutationsAboveThreshold).to.not.contain(mutationWithLessThan10PercentProportion);
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postAminoAcidMutations({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
      },
    });

    expect(ascendingOrderedResult.data[0]).to.have.property('mutation', 'ORF1a:A1306S');

    const descendingOrderedResult = await lapisClient.postAminoAcidMutations({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'descending' }],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('mutation', 'ORF8:Y73C');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postAminoAcidMutations({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('mutation', 'ORF1a:A1708D');

    const resultWithLimitAndOffset = await lapisClient.postAminoAcidMutations({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should return the lapis data version in the response', async () => {
    const result = await fetch(basePath + '/aminoAcidMutations');

    expect(result.status).equals(200);
    expect(result.headers.get('lapis-data-version')).to.match(/\d{10}/);
  });
});
