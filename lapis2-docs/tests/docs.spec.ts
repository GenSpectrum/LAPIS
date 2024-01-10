import { expect, Page, test } from '@playwright/test';
import { baseUrl } from './queryGenerator.page';

const pages = [
    'Introduction',
    'Generate your request',
    'Introduction',
    'Fields',
    'Filters',
    'Open API / Swagger',
    'Reference Genome',
    'Database Config',
    'Configuration',
    'Authentication',
    'Data versions',
    'Mutation filters',
    'Pango lineage query',
    'Request methods: GET and POST',
    'Response format',
    'Variant query',
    'Plot the global distribution of all sequences in R',
    'Start LAPIS and SILO',
    'Introduction and Goals',
    'Architecture and Constraints',
    'System Scope and Context',
    'Solution Strategy',
    'Building Block View',
    'Runtime View',
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
    const clickedLinks: Record<string, number> = {};

    for (const pageName of pages) {
        const alreadyClickedTimes = clickedLinks[pageName] || 0;
        await page.getByRole('link', { name: pageName, exact: true }).nth(alreadyClickedTimes).click();
        clickedLinks[pageName] = alreadyClickedTimes + 1;
        await expect(page).toHaveTitle(new RegExp(`^${pageName}`));
    }
}
