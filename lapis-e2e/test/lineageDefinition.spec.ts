import { expect } from 'chai';
import { basePath, lapisInfoClient } from './common';

describe('The lineageDefinition endpoint', function () {
  this.timeout(5000);

  it('should return the file as JSON', async () => {
    const lineageDefinition = await lapisInfoClient.getLineageDefinition({
      column: 'pangoLineage',
    });

    expect(lineageDefinition['A']).to.deep.equal({
      parents: undefined,
      aliases: undefined,
    });
    expect(lineageDefinition['A.1']).to.deep.equal({
      parents: ['A'],
      aliases: undefined,
    });
    expect(lineageDefinition['AT.1']).to.deep.equal({
      parents: ['B.1.1.370'],
      aliases: ['B.1.1.370.1'],
    });
  });

  it('should return the file as YAML', async () => {
    const result = await fetch(basePath + '/sample/lineageDefinition/pangoLineage', {
      headers: { Accept: 'application/yaml' },
    });

    expect(result.status).to.equal(200);

    const expectedFileStart = `---
A: {}
A.1:
  parents:
  - "A"
A.11:
  parents:
  - "A"`;

    const lineageDefinitionYaml = await result.text();
    expect(lineageDefinitionYaml).to.match(new RegExp(`^${expectedFileStart}`));
  });
});
