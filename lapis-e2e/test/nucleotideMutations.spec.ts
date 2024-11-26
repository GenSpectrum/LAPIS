import { expect } from 'chai';
import { basePath, lapisClient, lapisClientMultiSegmented } from './common';

describe('The /nucleotideMutations endpoint', () => {
  let mutationWithLessThan10PercentProportion = 'C19220T';
  let mutationWithMoreThan50PercentProportion = 'G28280C';

  it('should return mutation proportions for Switzerland', async () => {
    const result = await lapisClient.postNucleotideMutations({
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
    expect(commonMutationProportion?.sequenceName).to.be.undefined;
    expect(commonMutationProportion?.mutationFrom).to.be.equal('G');
    expect(commonMutationProportion?.mutationTo).to.be.equal('C');
    expect(commonMutationProportion?.position).to.be.equal(28280);
  });

  it('should return mutations proportions for multi segmented', async () => {
    const result = await lapisClientMultiSegmented.postNucleotideMutations({
      sequenceFiltersWithMinProportion: { country: 'Switzerland' },
    });

    expect(result.data).to.have.length(2);

    const mutationProportionOnFirstSegment = result.data.find(
      mutationData => mutationData.mutation === 'L:T1A'
    );
    expect(mutationProportionOnFirstSegment?.count).to.equal(2);

    const mutationProportionOnSecondSegment = result.data.find(
      mutationData => mutationData.mutation === 'M:T1C'
    );
    expect(mutationProportionOnSecondSegment?.count).to.equal(1);
  });

  it('should return mutation proportions for Switzerland with minProportion 0.5', async () => {
    const result = await lapisClient.postNucleotideMutations({
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
    const ascendingOrderedResult = await lapisClient.postNucleotideMutations({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
      },
    });

    expect(ascendingOrderedResult.data[0]).to.have.property('mutation', 'A1-');

    const descendingOrderedResult = await lapisClient.postNucleotideMutations({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'descending' }],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('mutation', 'T9-');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postNucleotideMutations({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('mutation', 'A11201G');

    const resultWithLimitAndOffset = await lapisClient.postNucleotideMutations({
      sequenceFiltersWithMinProportion: {
        orderBy: [{ field: 'mutation', type: 'ascending' }],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should correctly handle nucleotide insertion requests', async () => {
    const expectedFirstResultWithNucleotideInsertion = {
      count: 1,
      mutation: 'C241T',
      proportion: 1.0,
      mutationFrom: 'C',
      mutationTo: 'T',
      position: 241,
      sequenceName: undefined,
    };

    const result = await lapisClient.postNucleotideMutations({
      sequenceFiltersWithMinProportion: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
      },
    });

    expect(result.data).to.have.length(115);
    expect(result.data[0]).to.deep.equal(expectedFirstResultWithNucleotideInsertion);
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const expectedFirstResultWithAminoAcidInsertion = {
      count: 1,
      mutation: 'G210T',
      proportion: 1.0,
      mutationFrom: 'G',
      mutationTo: 'T',
      position: 210,
      sequenceName: undefined,
    };

    const result = await lapisClient.postNucleotideMutations({
      sequenceFiltersWithMinProportion: {
        aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
      },
    });

    expect(result.data).to.have.length(108);
    expect(result.data[0]).to.deep.equal(expectedFirstResultWithAminoAcidInsertion);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      country: 'Switzerland',
      age: '50',
      orderBy: 'mutation',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/sample/nucleotideMutations?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
mutation,count,proportion,sequenceName,mutationFrom,mutationTo,position
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
C7029T,1,0.0625,,C,T,7029
C71-,1,0.058823529411764705,,C,-,71
C7124T,2,0.11764705882352941,,C,T,7124
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

    const result = await fetch(basePath + '/sample/nucleotideMutations?' + urlParams.toString());
    const resultText = await result.text();

    expect(resultText).to.contain(
      String.raw`
mutation	count	proportion	sequenceName	mutationFrom	mutationTo	position
    `.trim()
    );

    expect(resultText).to.contain(
      String.raw`
C7029T	1	0.0625		C	T	7029
C71-	1	0.058823529411764705		C	-	71
C7124T	2	0.11764705882352941		C	T	7124
    `.trim()
    );
  });
});
