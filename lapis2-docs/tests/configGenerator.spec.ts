import { configGeneratorTest as test } from './configGenerator.page';

test.describe('The config generator wizard', () => {
    test('should run once', async ({ configGeneratorPage }) => {
        await configGeneratorPage.clickNewButton();

        await configGeneratorPage.fillInstanceName('test');

        await configGeneratorPage.clickNextButton();

        await configGeneratorPage.addMetadataField();
        await configGeneratorPage.editMetadata('New Metadata', {
            name: 'TestString',
            type: 'String',
            generateIndex: true,
        });

        await configGeneratorPage.addMetadataField();
        await configGeneratorPage.editMetadata('New Metadata', {
            name: 'TestDate',
            type: 'date',
        });

        await configGeneratorPage.addMetadataField();
        await configGeneratorPage.editMetadata('New Metadata', {
            name: 'TestPangoLineage',
            type: 'pango_lineage',
        });

        await configGeneratorPage.clickNextButton();
        await configGeneratorPage.clickBackButton();

        await configGeneratorPage.addMetadataField();
        await configGeneratorPage.editMetadata('New Metadata', {
            name: 'SecondTestString',
            type: 'String',
            generateIndex: true,
        });

        await configGeneratorPage.addMetadataField();
        await configGeneratorPage.editMetadata('New Metadata', {
            name: 'SecondTestDate',
            type: 'date',
        });

        await configGeneratorPage.addMetadataField();
        await configGeneratorPage.editMetadata('New Metadata', {
            name: 'SecondTestPangoLineage',
            type: 'pango_lineage',
        });

        await configGeneratorPage.clickNextButton();

        await configGeneratorPage.editAdditionalInformation('TestString', 'SecondTestString');
        await configGeneratorPage.editAdditionalInformation('TestDate', 'SecondTestDate');
        await configGeneratorPage.editAdditionalInformation('TestPangoLineage', 'SecondTestPangoLineage');

        await configGeneratorPage.expectDownloadOfConfig();

        const expectedConfig = `
        schema:
          instanceName: test
          opennessLevel: OPEN
          metadata:
            - name: TestString
              type: string
              generateIndex: true
            - name: TestDate
              type: date
              generateIndex: false
            - name: TestPangoLineage
              type: pango_lineage
              generateIndex: true
            - name: SecondTestString
              type: string
              generateIndex: true
            - name: SecondTestDate
              type: date
              generateIndex: false
            - name: SecondTestPangoLineage
              type: pango_lineage
              generateIndex: true
          primaryKey: SecondTestString
          dateToSortBy: SecondTestDate
          partitionBy: SecondTestPangoLineage
        `;

        await configGeneratorPage.expectConfigContains(expectedConfig);
    });
});
