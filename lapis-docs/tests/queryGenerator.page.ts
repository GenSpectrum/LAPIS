import { expect, Locator, Page, test } from '@playwright/test';

export const baseUrl = 'http://localhost:4321/docs/';

export const queryGeneratorTest = test.extend<{ queryGeneratorPage: QueryGeneratorPage }>({
    queryGeneratorPage: async ({ page }, use) => {
        const queryGeneratorPage = new QueryGeneratorPage(page);
        await queryGeneratorPage.goto();
        await use(queryGeneratorPage);
    },
});

export class QueryGeneratorPage {
    private readonly nextButton: Locator;
    private readonly addOrderByFieldButton: Locator;
    private readonly orderByContainer: Locator;

    constructor(public readonly page: Page) {
        this.nextButton = page.getByRole('button', { name: 'Next', exact: true });
        this.addOrderByFieldButton = page.getByRole('button', { name: '+', exact: true });
        this.orderByContainer = page.locator('div').filter({ hasText: /^Order by:/ });
    }

    public async goto() {
        await this.page.goto(baseUrl);
        await this.page.getByRole('link', { name: 'Introduction' }).click();
        await this.page.getByRole('link', { name: 'Generate your request', exact: true }).click();
    }

    public async selectAggregatedWithFields(...fields: string[]) {
        await this.page.getByLabel('Number of sequences per ...', { exact: true }).check();
        await this.waitUntilFirstCheckboxIsUnchecked();
        for (const field of fields) {
            await this.page.getByLabel(field).first().check();
        }
    }

    async waitUntilFirstCheckboxIsUnchecked() {
        await expect(this.page.getByLabel('Number of sequences', { exact: true })).not.toBeChecked();
    }

    public async clickNextButton() {
        await this.nextButton.click();
    }

    public async fillFilterField(name: string, value: string) {
        await this.page.getByText(`${name}:`, { exact: true }).getByRole('textbox').fill(value);
    }

    public async addOrderByField(name?: string) {
        if (name) {
            await this.page.getByRole('main').getByRole('combobox').selectOption(name);
        }
        await this.addOrderByFieldButton.click();
    }

    public async removeFirstOrderByField() {
        await this.orderByContainer.getByRole('button', { name: '-', exact: true }).first().click();
    }

    public async expectOrderByFields(...fields: string[]) {
        for (const field of fields) {
            const index = fields.indexOf(field);
            await expect(this.orderByContainer.getByRole('textbox').nth(index)).toHaveValue(field);
        }
    }

    public async selectOutputFormat(format: 'JSON' | 'TSV' | 'CSV') {
        await this.page.getByLabel(format).check();
    }

    public async selectCompressionFormat(format: 'gzip' | 'zstd') {
        await this.page.getByLabel(format).check();
    }

    public async checkDownloadAsFile() {
        await this.page.getByLabel('Download as file').check();
    }

    public async expectQueryUrlContains(expected: string) {
        await expect(this.page.getByRole('textbox', {})).toHaveValue(
            new RegExp(`^http://localhost:8090/sample.*${expected}`),
        );
    }

    public async viewPythonCode() {
        await this.page.getByText('Python code').click();
    }

    public async expectCodeContains(expected: string) {
        await expect(this.page.getByRole('code')).toContainText(expected);
    }
}
