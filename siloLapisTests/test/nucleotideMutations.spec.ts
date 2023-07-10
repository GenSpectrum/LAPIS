import { expect } from 'chai';
import { lapisClient } from './common';
import fs from 'fs';
import { SequenceFilters } from './lapisClient';

describe('The /nucleotideMutationProportions endpoint', () => {
  let mutationWithLessThan10PercentProportion = 'C19220T';
  let mutationWithMoreThan50PercentProportion = 'G28280C';

  it('should return mutation proportions for Switzerland', async () => {
    const result = await lapisClient.postNucleotideMutations({
      sequenceFiltersWithMinProportion: { country: 'Switzerland' },
    });

    expect(result).to.have.length(362);

    const rareMutationProportion = result.find(
      mutationData => mutationData.position === mutationWithLessThan10PercentProportion
    );
    expect(rareMutationProportion?.count).to.equal(8);
    expect(rareMutationProportion?.proportion).to.be.approximately(0.0816, 0.0001);

    const commonMutationProportion = result.find(
      mutationProportion => mutationProportion.position === mutationWithMoreThan50PercentProportion
    );
    expect(commonMutationProportion?.count).to.equal(51);
    expect(commonMutationProportion?.proportion).to.be.approximately(0.5204, 0.0001);
  });

  it('should return mutation proportions for Switzerland with minProportion 0.5', async () => {
    const result = await lapisClient.postNucleotideMutations({
      sequenceFiltersWithMinProportion: {
        country: 'Switzerland',
        minProportion: 0.5,
      },
    });

    expect(result).to.have.length(108);

    const mutationsAboveThreshold = result.map(mutationData => mutationData.position);
    expect(mutationsAboveThreshold).to.contain(mutationWithMoreThan50PercentProportion);
    expect(mutationsAboveThreshold).to.not.contain(mutationWithLessThan10PercentProportion);
  });
});
