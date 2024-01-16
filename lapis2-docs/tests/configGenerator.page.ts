import { expect, Locator, Page, test } from '@playwright/test';
import { baseUrl } from './queryGenerator.page';

export const configGeneratorTest = test.extend<{ configGeneratorPage: ConfigGeneratorPage }>({
    configGeneratorPage: async ({ page }, use) => {
        const queryGeneratorPage = new ConfigGeneratorPage(page);
        await queryGeneratorPage.goto();
        await use(queryGeneratorPage);
    },
});

type Metadata = {
    name?: string;
    type?: string;
    generateIndex?: boolean;
};

export class ConfigGeneratorPage {
    private readonly nextButton: Locator;

    constructor(public readonly page: Page) {
        this.nextButton = page.getByRole('button', { name: 'Next', exact: true });
    }

    public async goto() {
        await this.page.goto(baseUrl);
        await this.page.getByRole('link', { name: 'Introduction' }).click();
        await this.page.getByRole('link', { name: 'Generate your config', exact: true }).click();
    }

    public async clickNextButton() {
        await this.nextButton.click();
    }

    public async clickNewButton() {
        await this.page.getByRole('button', { name: 'New', exact: true }).click();
    }

    public async fillInstanceName(name: string) {
        await this.page.getByRole('textbox', { name: 'Instance Name' }).fill(name);
    }

    public async addMetadataField() {
        await this.page.getByRole('button', { name: 'Add', exact: true }).click();
    }

    public async editMetadata(oldName: string, newMetadata: Metadata) {
        await this.openMetadataEdit(oldName);

        if (newMetadata.name !== undefined) {
            await this.editMetadataNameInModal(oldName, newMetadata.name);
        }
        if (newMetadata.type !== undefined) {
            await this.editMetadataTypeInModal(oldName, newMetadata.type);
        }
        if (newMetadata.generateIndex !== undefined) {
            await this.editMetadataGenerateIndexInModal(oldName, newMetadata.generateIndex);
        }

        await this.page
            .locator(`#modal_${oldName.replace(' ', '')}`)
            .getByRole('button', { name: 'OK' })
            .click();
    }

    public async openMetadataEdit(metadataName: string) {
        await (await this.getMetadataRow(metadataName)).getByRole('button').first().click();
    }

    public async getMetadataRow(metadataName: string) {
        return this.page.getByRole('row', { name: `${metadataName}` });
    }

    public async editMetadataNameInModal(oldName: string, name: string) {
        await this.page.getByRole('row', { name: oldName }).getByRole('textbox').click();
        await this.page.getByRole('row', { name: oldName }).getByRole('textbox').fill(name);
    }

    public async editMetadataTypeInModal(metadataName: string, type: string) {
        await this.page.getByRole('row', { name: metadataName }).getByRole('combobox').selectOption(type);
    }

    public async editMetadataGenerateIndexInModal(metadataName: string, generateIndex: boolean) {
        await this.page.getByRole('row', { name: metadataName }).getByRole('checkbox').check({
            timeout: 1000,
        });
    }

    public async editAdditionalInformation(oldValue: string, newValue: string) {
        await this.page.getByLabel(oldValue).selectOption(newValue);
    }

    public async clickBackButton() {
        await this.page.getByRole('button', { name: 'Back', exact: true }).click();
    }

    public async expectDownloadOfConfig() {
        const downloadPromise = this.page.waitForEvent('download');
        await this.page.getByRole('button', { name: 'Download', exact: true }).click();
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toBe('config.yaml');
    }

    public async expectConfigContains(expected: string) {
        await expect(this.page.getByRole('code')).toContainText(expected);
    }
}
