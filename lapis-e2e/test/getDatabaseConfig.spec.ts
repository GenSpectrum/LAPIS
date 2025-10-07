import { lapisInfoClient } from './common';
import yaml from 'js-yaml';
import fs from 'fs';
import { expect } from 'chai';
import { DatabaseConfig } from './lapisClient';

describe('The /databaseConfig endpoint', () => {
  it('should download the config with all defaults set', async () => {
    const result = await lapisInfoClient.getDatabaseConfigAsJson();

    const databaseConfigFromFile = yaml.load(
      fs.readFileSync('testData/singleSegmented/testDatabaseConfig.yaml', 'utf8')
    ) as DatabaseConfig;

    const expected: DatabaseConfig = {
      ...databaseConfigFromFile,
      defaultAminoAcidSequence: undefined,
      schema: {
        ...databaseConfigFromFile.schema,
        metadata: databaseConfigFromFile.schema.metadata.map(field => ({
          ...field,
          valuesAreUnique: field.valuesAreUnique ?? false,
          generateIndex: field.generateIndex ?? false,
          generateLineageIndex: field.generateLineageIndex ?? undefined,
          isPhyloTreeField: field.isPhyloTreeField ?? false,
        })),
      },
    };

    expect(result).to.deep.equal(expected);
  });
});
