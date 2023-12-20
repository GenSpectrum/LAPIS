import { expect, Page, test } from '@playwright/test';
import { baseUrl } from './queryGenerator.page';

const pages = [
    'Introduction',
    'Generate your request',
    'Endpoints',
    'Fields',
    'Filters',
    'Open API / Swagger',
    'Data versions',
    'Mutation filters',
    'Pango lineage query',
    'Request methods: GET and POST',
    'Response format',
    'Variant query',
    'Introduction and Goals',
    'Architecture and Constraints',
    'System Scope and Context',
    'Glossary',
];

test.describe('The documentation', () => {
    test('should show all expected pages', async ({ page }) => {
        await page.goto(baseUrl);

        await page.getByRole('link', { name: 'Introduction' }).click();
        await expect(page).toHaveTitle(/^Introduction/);

        await clickOnAllNextButtons(page);
        await clickOnAllLinksInNavigation(page);
    });
});

async function clickOnAllNextButtons(page: Page) {
    const pagesAfterIntroduction = pages.slice(1);
    for (const pageName of pagesAfterIntroduction) {
        await page.getByRole('link', { name: `Next ${pageName}` }).click();
        await expect(page).toHaveTitle(new RegExp(`^${pageName}`));
    }
}

async function clickOnAllLinksInNavigation(page: Page) {
    for (const pageName of pages) {
        await page.getByRole('link', { name: pageName, exact: true }).click();
        await expect(page).toHaveTitle(new RegExp(`^${pageName}`));
    }
}
