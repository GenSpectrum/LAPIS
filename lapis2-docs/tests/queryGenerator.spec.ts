import { queryGeneratorTest as test } from './queryGenerator.page';

test.describe('The query generator wizard', () => {
    test('should add the selected fields and filters to the query', async ({ queryGeneratorPage }) => {
        await queryGeneratorPage.selectAggregatedWithFields('primaryKey', 'date');
        await queryGeneratorPage.clickNextButton();

        await queryGeneratorPage.fillFilterField('country', 'Switzerland');
        await queryGeneratorPage.fillFilterField('pangoLineage', 'B.1.1.7');
        await queryGeneratorPage.clickNextButton();

        await queryGeneratorPage.clickNextButton();

        await queryGeneratorPage.expectQueryUrlContains('fields=primaryKey&fields=date');
        await queryGeneratorPage.expectQueryUrlContains('country=Switzerland&pangoLineage=B.1.1.7');

        await queryGeneratorPage.selectOutputFormat('TSV');
        await queryGeneratorPage.expectQueryUrlContains('dataFormat=tsv');

        await queryGeneratorPage.selectOutputFormat('CSV');
        await queryGeneratorPage.expectQueryUrlContains('dataFormat=csv');

        await queryGeneratorPage.viewPythonCode();
        await queryGeneratorPage.expectCodeContains(
            '@dataclass class DataEntry: count: int primaryKey: Optional[str] date: Optional[str]',
        );
    });

    test('should add order by, limit and offset to query', async ({ queryGeneratorPage }) => {
        await queryGeneratorPage.selectAggregatedWithFields('primaryKey', 'date');
        await queryGeneratorPage.clickNextButton();
        await queryGeneratorPage.clickNextButton();

        await queryGeneratorPage.addOrderByField();
        await queryGeneratorPage.addOrderByField('date');
        await queryGeneratorPage.addOrderByField('primaryKey');
        await queryGeneratorPage.expectOrderByFields('count', 'date', 'primaryKey');

        await queryGeneratorPage.removeFirstOrderByField();
        await queryGeneratorPage.expectOrderByFields('date', 'primaryKey');

        await queryGeneratorPage.clickNextButton();

        await queryGeneratorPage.expectQueryUrlContains('orderBy=date&orderBy=primaryKey');
    });
});
