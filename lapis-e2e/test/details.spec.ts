import { expect } from 'chai';
import { basePath, lapisClient } from './common';

function setsAreEqual<T>(a: Set<T>, b: Set<T>) {
  return a.size === b.size && [...a].every(x => b.has(x));
}

describe('The /details endpoint', () => {
  it('should return details with specified fields', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        pangoLineage: 'B.1.617.2',
        fields: ['pangoLineage', 'division'],
      },
    });

    expect(result.data).to.have.length(2);
    result.data.sort((a: { division: string }, b: { division: string }) =>
      a.division.localeCompare(b.division)
    );
    expect(result.data[1]).to.be.deep.equal({
      age: undefined,
      country: undefined,
      date: undefined,
      division: 'Zürich',
      pangoLineage: 'B.1.617.2',
      primaryKey: undefined,
      usherTree: undefined,
      qcValue: undefined,
      region: undefined,
      testBooleanColumn: undefined,
    });
  });

  it('should return details with all fields when no explicit fields were specified', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        pangoLineage: 'B.1.617.2',
      },
    });

    expect(result.data).to.have.length(2);
    result.data.sort((a: { division: string }, b: { division: string }) =>
      a.division.localeCompare(b.division)
    );
    expect(result.data[1]).to.be.deep.equal({
      age: 54,
      country: 'Switzerland',
      date: '2021-07-19',
      division: 'Zürich',
      primaryKey: 'key_3128796',
      usherTree: 'key_3128796',
      pangoLineage: 'B.1.617.2',
      qcValue: 0.96,
      region: 'Europe',
      testBooleanColumn: false,
    });
  });

  it('should order by specified fields', async () => {
    const ascendingOrderedResult = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'division', type: 'ascending' }],
        fields: ['division'],
      },
    });

    expect(ascendingOrderedResult.data.at(0).division).to.be.undefined;
    expect(ascendingOrderedResult.data.at(1).division).to.be.undefined;
    expect(ascendingOrderedResult.data.at(2)).to.have.property('division', 'Aargau');

    const descendingOrderedResult = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'division', type: 'descending' }],
        fields: ['division'],
      },
    });

    expect(descendingOrderedResult.data[0]).to.have.property('division', 'Zürich');
  });

  it('should apply limit and offset', async () => {
    const resultWithLimit = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'primaryKey', type: 'ascending' }],
        fields: ['primaryKey'],
        limit: 2,
      },
    });

    expect(resultWithLimit.data).to.have.length(2);
    expect(resultWithLimit.data[1]).to.have.property('primaryKey', 'key_1001920');

    const resultWithLimitAndOffset = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'primaryKey', type: 'ascending' }],
        fields: ['primaryKey'],
        limit: 2,
        offset: 1,
      },
    });

    expect(resultWithLimitAndOffset.data).to.have.length(2);
    expect(resultWithLimitAndOffset.data[0]).to.deep.equal(resultWithLimit.data[1]);
  });

  it('should handle PhyloDescendantOf queries', async () => {
    const urlParams = new URLSearchParams({
      'fields': 'usherTree',
      'usherTree.phyloDescendantOf': 'NODE_0000043',
      'orderBy': 'usherTree',
      'dataFormat': 'csv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(result.status).to.be.equal(200);

    const urlParamsAdvanced = new URLSearchParams({
      fields: 'usherTree',
      advancedquery: 'usherTree.PhyloDescendantOf=NODE_0000043',
      orderBy: 'usherTree',
      dataFormat: 'csv',
    });

    const resultAdvanced = await fetch(basePath + '/sample/details?' + urlParamsAdvanced.toString());

    expect(resultAdvanced.status).to.be.equal(200);

    const urlParamsVariantQuery = new URLSearchParams({
      fields: 'usherTree',
      variantQuery: 'usherTree.DescendantOf:NODE_0000043',
      orderBy: 'usherTree',
      dataFormat: 'csv',
    });

    const resultVariantQuery = await fetch(basePath + '/sample/details?' + urlParamsVariantQuery.toString());

    expect(resultVariantQuery.status).to.be.equal(200);

    const resultText = await result.text();
    const resultAdvancedText = await resultAdvanced.text();
    const resultVariantText = await resultVariantQuery.text();
    expect(resultText).to.be.equal(resultAdvancedText);
    expect(resultText).to.be.equal(resultVariantText);

    expect(resultAdvancedText).to.be.equal(
      String.raw`
usherTree
key_2181005
key_2270139
    `.trim() + '\n'
    );
  });

  it('should handle advancedQuery', async () => {
    const urlParams = new URLSearchParams({
      fields: 'primaryKey',
      aminoAcidInsertions: 'ins_S:143:T,ins_ORF1a:3602:F?P',
      division: 'Vaud',
      orderBy: 'primaryKey',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(result.status).to.be.equal(200);

    const urlParamsAdvanced = new URLSearchParams({
      fields: 'primaryKey',
      advancedQuery: 'division=Vaud AND ins_S:143:T AND ins_ORF1a:3602:F?P',
      orderBy: 'primaryKey',
      dataFormat: 'csv',
    });

    const resultAdvanced = await fetch(basePath + '/sample/details?' + urlParamsAdvanced.toString());

    expect(resultAdvanced.status).to.be.equal(200);

    const resultText = await result.text();
    const resultAdvancedText = await resultAdvanced.text();
    expect(resultText).to.be.equal(resultAdvancedText);
  });

  it('should return the data as CSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'division,pangoLineage,primaryKey',
      orderBy: 'primaryKey',
      limit: '3',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
division,pangoLineage,primaryKey
Vaud,B.1.177.44,key_1001493
Bern,B.1.177,key_1001920
Solothurn,B.1,key_1002052
    `.trim() + '\n'
    );
  });

  it('should return only CSV header when no data', async () => {
    const urlParams = new URLSearchParams({
      country: 'this country does not exist',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      'primaryKey,usherTree,date,region,country,pangoLineage,division,age,qc_value,test_boolean_column\n'
    );
  });

  it('should return the data as TSV', async () => {
    const urlParams = new URLSearchParams({
      fields: 'division,pangoLineage,primaryKey',
      orderBy: 'primaryKey',
      limit: '3',
      dataFormat: 'tsv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(await result.text()).to.be.equal(
      String.raw`
division	pangoLineage	primaryKey
Vaud	B.1.177.44	key_1001493
Bern	B.1.177	key_1001920
Solothurn	B.1	key_1002052
    `.trim() + '\n'
    );
  });

  it('should correctly handle nucleotide insertion requests', async () => {
    const expectedResultWithNucleotideInsertion = {
      age: 57,
      country: 'Switzerland',
      date: '2021-05-12',
      division: 'Zürich',
      primaryKey: 'key_3578231',
      usherTree: 'key_3578231',
      pangoLineage: 'P.1',
      qcValue: 0.93,
      region: 'Europe',
      testBooleanColumn: undefined,
    };

    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        nucleotideInsertions: ['ins_25701:CC?', 'ins_5959:?AT'],
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.deep.equal(expectedResultWithNucleotideInsertion);
  });

  it('should correctly handle amino acid insertion requests', async () => {
    const expectedResultWithAminoAcidInsertion = {
      age: 52,
      country: 'Switzerland',
      date: '2021-07-04',
      division: 'Vaud',
      primaryKey: 'key_3259931',
      usherTree: 'key_3259931',
      pangoLineage: 'AY.43',
      qcValue: 0.98,
      region: 'Europe',
      testBooleanColumn: true,
    };

    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        aminoAcidInsertions: ['ins_S:143:T', 'ins_ORF1a:3602:F?P'],
      },
    });

    expect(result.data).to.have.length(1);
    expect(result.data[0]).to.deep.equal(expectedResultWithAminoAcidInsertion);
  });

  it("should provide a way to get a plain list of primary keys for CoV-Spectrum's UShER integration", async () => {
    const urlParams = new URLSearchParams({
      dataFormat: 'CSV-WITHOUT-HEADERS',
      fields: 'primaryKey',
      limit: '3',
      orderBy: 'primaryKey',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(result.headers.get('content-type')).equals('text/plain');
    expect(await result.text()).to.be.equal(
      String.raw`
key_1001493
key_1001920
key_1002052
    `.trim() + '\n'
    );
  });

  it('should order by random', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'random' }],
        fields: ['primaryKey', 'division'],
      },
    });

    expect(result).to.have.nested.property('data[2].division', 'Aargau');
  });

  it('should order by random (new format)', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: { random: true },
        fields: ['primaryKey', 'division'],
      },
    });

    expect(result).to.have.nested.property('data[2].division', 'Aargau');
  });

  it('should order by random with seed', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: [{ field: 'random(123)' }],
        fields: ['primaryKey', 'division'],
      },
    });

    expect(result).to.have.nested.property('data[2].division', 'Aargau');
  });

  it('should order by random with seed (new format)', async () => {
    const result = await lapisClient.postDetails({
      detailsPostRequest: {
        orderBy: { random: 123 },
        fields: ['primaryKey', 'division'],
      },
    });

    expect(result).to.have.nested.property('data[2].division', 'Aargau');
  });

  it('variantQuery and advancedQuery should be the same for sequence and regex intersections and unions', async () => {
    const sequenceQueries = [
      '!400- & (S:222V | S:234A)',
      '[exactly-2-of: N:A220V, S:222V, S:345- | S:346-, [2-of: 222T, 333G, 444A, 555C]]',
      'MAYBE(S:222V)',
    ];
    const regexQueries = ['region\d', 'Basel-(Stadt|Land)', 'Basel.*', '^Z.*rich$'];
    for (const sequenceQuery of sequenceQueries) {
      for (const regexQuery of regexQueries) {
        const resultRegexJson = await lapisClient.postDetails({
          detailsPostRequest: {
            fields: ['primaryKey'],
            divisionRegex: regexQuery,
            orderBy: [{ field: 'primaryKey' }],
          },
        });
        const resultVariant = await lapisClient.postDetails({
          detailsPostRequest: {
            fields: ['primaryKey'],
            variantQuery: sequenceQuery,
            orderBy: [{ field: 'primaryKey' }],
          },
        });

        const setRegex = new Set(
          resultRegexJson.data.map((entry: { primaryKey: string }) => entry.primaryKey)
        );
        const setVariant = new Set(
          resultVariant.data.map((entry: { primaryKey: string }) => entry.primaryKey)
        );

        const advancedQueryIntersection = `division.regex='${regexQuery}' AND ${sequenceQuery}`;
        const advancedQueryUnion = `division.regex='${regexQuery}' OR ${sequenceQuery}`;

        const resultIntersection = await lapisClient.postDetails({
          detailsPostRequest: {
            fields: ['primaryKey'],
            advancedQuery: advancedQueryIntersection,
            orderBy: [{ field: 'primaryKey' }],
          },
        });
        const resultUnion = await lapisClient.postDetails({
          detailsPostRequest: {
            fields: ['primaryKey'],
            advancedQuery: advancedQueryUnion,
            orderBy: [{ field: 'primaryKey' }],
          },
        });

        const setIntersection = new Set(
          resultIntersection.data.map((entry: { primaryKey: string }) => entry.primaryKey)
        );
        const setUnion = new Set(resultUnion.data.map((entry: { primaryKey: string }) => entry.primaryKey));

        expect(
          setsAreEqual(setRegex.union(setVariant), setUnion),
          `Union mismatch, actual: ${Array.from(setRegex.union(setVariant))}, expected: ${Array.from(setUnion)}`
        ).to.be.true;

        expect(
          setsAreEqual(setRegex.intersection(setVariant), setIntersection),
          `Intersection mismatch, actual: ${Array.from(setRegex.intersection(setVariant))}, expected: ${Array.from(setIntersection)}`
        ).to.be.true;
      }
    }
  });

  it('should throw an error for invalid Maybe request', async () => {
    const metadataQueries = ['division=Basel', "division.regex='Basel'", 'date>=2021-01-01'];

    for (const metadataQuery of metadataQueries) {
      const advancedQuery = `MAYBE(${metadataQuery})`;

      const urlParamsAdvanced = new URLSearchParams({
        fields: 'primaryKey',
        advancedQuery: advancedQuery,
        orderBy: 'primaryKey',
        dataFormat: 'csv',
      });

      const result = await fetch(basePath + '/sample/details?' + urlParamsAdvanced.toString());

      expect(result.status).equals(400);
      expect(result.headers.get('Content-Type')).equals('application/json');
      const json = await result.json();
      expect(json.error.detail).to.include('Failed to parse advanced query');
    }
  });

  it('advancedQuery and metadata searches should be the same for dates', async () => {
    const urlParams = new URLSearchParams({
      fields: 'primaryKey',
      dateFrom: '2021-01-01',
      dateTo: '2021-12-31',
      orderBy: 'primaryKey',
      dataFormat: 'csv',
    });

    const result = await fetch(basePath + '/sample/details?' + urlParams.toString());

    expect(result.status).to.be.equal(200);

    const urlParamsAdvanced = new URLSearchParams({
      fields: 'primaryKey',
      advancedQuery: 'date>=2021-01-01 AND date<=2021-12-31',
      orderBy: 'primaryKey',
      dataFormat: 'csv',
    });

    const resultAdvanced = await fetch(basePath + '/sample/details?' + urlParamsAdvanced.toString());

    expect(resultAdvanced.status).to.be.equal(200);

    const resultText = await result.text();
    const resultAdvancedText = await resultAdvanced.text();
    expect(resultText).to.be.equal(resultAdvancedText);
  });
});
