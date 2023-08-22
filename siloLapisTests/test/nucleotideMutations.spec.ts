import { expect } from 'chai';
import { basePath, lapisClient } from './common';

describe('The /nucleotideMutations endpoint', () => {
  let mutationWithLessThan10PercentProportion = 'C19220T';
  let mutationWithMoreThan50PercentProportion = 'G28280C';

  it('should return mutation proportions for Switzerland', async () => {
    const result = await lapisClient.postNucleotideMutations1({
      sequenceFiltersWithMinProportion: { country: 'Switzerland' },
    });

    expect(result.data).to.have.length(362);

    const rareMutationProportion = result.data.find(
      mutationData => mutationData.mutation === mutationWithLessThan10PercentProportion
    );
    expect(rareMutationProportion?.count).to.equal(8);
    expect(rareMutationProportion?.proportion).to.be.approximately(0.0816, 0.0001);

    const commonMutationProportion = result.data.find(
      mutationProportion => mutationProportion.mutation === mutationWithMoreThan50PercentProportion
    );
    expect(commonMutationProportion?.count).to.equal(51);
    expect(commonMutationProportion?.proportion).to.be.approximately(0.5204, 0.0001);
  });

  it('should return mutation proportions for Switzerland with minProportion 0.5', async () => {
    const result = await lapisClient.postNucleotideMutations1({
      sequenceFiltersWithMinProportion: {
        country: 'Switzerland',
        minProportion: 0.5,
      },
    });

    expect(result.data).to.have.length(108);

    const mutationsAboveThreshold = result.data.map(mutationData => mutationData.mutation);
    expect(mutationsAboveThreshold).to.contain(mutationWithMoreThan50PercentProportion);
    expect(mutationsAboveThreshold).to.not.contain(mutationWithLessThan10PercentProportion);
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postNucleotideMutations1({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
      },
    });

    expect(ascendingOrderedResult.data[0]).to.have.property('mutation', 'A1-');

    const descendingOrderedResult = await lapisClient.postNucleotideMutations1({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'descending' }],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('mutation', 'T9-');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postNucleotideMutations1({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('mutation', 'A11201G');

    const resultWithLimitAndOffset = await lapisClient.postNucleotideMutations1({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      age: '50',
      orderBy: 'mutation',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/nucleotideMutations?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
mutation,count,proportion
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
C7029T,1,0.0625
C71-,1,0.058823529411764705
C7124T,2,0.11764705882352941
`.trim()
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      age: '50',
      orderBy: 'mutation',
      dataFormat: 'tsv',
    });

    const result = await fetch(basePath + '/nucleotideMutations?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
mutation	count	proportion
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
C7029T	1	0.0625
C71-	1	0.058823529411764705
C7124T	2	0.11764705882352941
    `.trim()
    );
  });

  it('should return the lapis data version in the response', async () => {
    const result = await fetch(basePath + '/nucleotideMutations');

    expect(result.status).equals(200);
    expect(result.headers.get('lapis-data-version')).to.match(/\d{10}/);
  });
});
