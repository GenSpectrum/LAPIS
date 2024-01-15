import { expect, Page, test } from '@playwright/test';
import { baseUrl } from './queryGenerator.page';

const gettingStartedPages = ['Introduction', 'Generate your request'];

const referencesPages = [
    'Introduction',
    'Fields',
    'Filters',
    'Open API / Swagger',
    'Reference Genomes',
    'Nucleotide And Amino Acid Symbols',
    'Database Config',
];

const conceptsPages = [
    'Data versions',
    'Mutation filters',
    'Ambiguous symbols',
    'Pango lineage query',
    'Request methods: GET and POST',
    'Response format',
    'Variant query',
];

const userTutorialPages = ['Plot the global distribution of all sequences in R'];

const architecturePages = [
    'Introduction and Goals',
    'Architecture and Constraints',
    'System Scope and Context',
    'Solution Strategy',
    'Building Block View',
    'Runtime View',
    'Glossary',
];

const maintainerDocsPages = [
    'Database Configuration',
    'Reference Genomes',
    'Starting SILO and LAPIS',
    'Preprocessing',
    'Start LAPIS and SILO',
];

const pages = [
    ...gettingStartedPages,
    ...referencesPages,
    ...conceptsPages,
    ...userTutorialPages,
    ...architecturePages,
    ...maintainerDocsPages,
];

test.describe('The documentation', () => {
    test('should show all expected pages via next buttons and all relative links should work', async ({ page }) => {
        await page.goto(baseUrl);

        await page.getByRole('link', { name: 'Introduction' }).click();
        await expect(page).toHaveTitle(/^Introduction/);

        await clickOnAllNextButtonsAndRelativeLinks(page);
    });

    test('should show all expected pages via link in navigation', async ({ page }) => {
        await page.goto(baseUrl);

        await page.getByRole('link', { name: 'Introduction' }).click();
        await expect(page).toHaveTitle(/^Introduction/);

        await clickOnAllLinksInNavigation(page);
    });
});

async function clickOnAllNextButtonsAndRelativeLinks(page: Page) {
    const pagesAfterIntroduction = pages.slice(1);
    for (const pageName of pagesAfterIntroduction) {
        await page.getByRole('link', { name: `Next ${pageName}` }).click();
        await expect(page).toHaveTitle(new RegExp(`^${pageName}`));

        await clickOnAllRelativeLinksInMainBody(page);
    }
}

async function clickOnAllRelativeLinksInMainBody(page: Page) {
    const currentPageUrl = page.url();

    const relativeLinks = await page
        .getByRole('main')
        .locator('a[href]:not([href^="http://"]):not([href^="https://"])')
        .all();
    for (const relativeLink of relativeLinks) {
        await relativeLink.click();

        const errorMessage = `Went to ${page.url()} from ${currentPageUrl}, but did not find target page.`;
        await expect(page.getByText('Page not found.'), errorMessage).not.toBeVisible();

        await page.goBack();
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
