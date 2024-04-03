import { expect, Page, test } from '@playwright/test';
import { baseUrl } from './queryGenerator.page';

type DocumentationPage = {
    title: string;
    relativeUrl: string;
};

const prependToRelativeUrl = (pages: DocumentationPage[], prefix: string) =>
    pages.map((page) => {
        return {
            title: page.title,
            relativeUrl: `${prefix}${page.relativeUrl}`,
        };
    });

const gettingStartedPages = prependToRelativeUrl(
    [
        { title: 'Introduction', relativeUrl: '/introduction' },
        {
            title: 'Generate your request',
            relativeUrl: '/generate-your-request',
        },
    ],
    '/getting-started',
);

const referencesPages = prependToRelativeUrl(
    [
        { title: 'Introduction', relativeUrl: '/introduction' },
        { title: 'Fields', relativeUrl: '/fields' },
        { title: 'Filters', relativeUrl: '/filters' },
        { title: 'Additional Request Properties', relativeUrl: '/additional-request-properties' },
        { title: 'Open API / Swagger', relativeUrl: '/open-api-definition' },
        { title: 'Database Config', relativeUrl: '/database-config' },
        { title: 'Reference Genomes', relativeUrl: '/reference-genomes' },
        { title: 'Nucleotide And Amino Acid Symbols', relativeUrl: '/nucleotide-and-amino-acid-symbols' },
    ],
    '/references',
);

const conceptsPages = prependToRelativeUrl(
    [
        { title: 'Data versions', relativeUrl: '/data-versions' },
        { title: 'Mutation filters', relativeUrl: '/mutation-filters' },
        { title: 'Ambiguous symbols', relativeUrl: '/ambiguous-symbols' },
        { title: 'Pango lineage query', relativeUrl: '/pango-lineage-query' },
        { title: 'Request methods: GET and POST', relativeUrl: '/request-methods' },
        { title: 'Response format', relativeUrl: '/response-format' },
        { title: 'Variant query', relativeUrl: '/variant-query' },
        { title: 'Request Id', relativeUrl: '/request-id' },
    ],
    '/concepts',
);

const userTutorialPages = prependToRelativeUrl(
    [
        {
            title: 'Plot the global distribution of all sequences in R',
            relativeUrl: '/plot-global-distribution-of-sequences-in-r',
        },
    ],
    '/tutorials',
);

const architecturePages = prependToRelativeUrl(
    [
        { title: 'Introduction and Goals', relativeUrl: '/01-introduction' },
        { title: 'Architecture and Constraints', relativeUrl: '/02-architecture-and-constraints' },
        { title: 'System Scope and Context', relativeUrl: '/03-system-scope-and-context' },
        { title: 'Solution Strategy', relativeUrl: '/04-solution-strategy' },
        { title: 'Building Block View', relativeUrl: '/05-building-block-view' },
        { title: 'Runtime View', relativeUrl: '/06-runtime-view' },
        { title: 'Glossary', relativeUrl: '/99-glossary' },
    ],
    '/architecture-and-dev-docs',
);

const maintainerDocsPages = prependToRelativeUrl(
    [
        ...prependToRelativeUrl(
            [
                { title: 'Database Configuration', relativeUrl: '/database-configuration' },
                { title: 'Reference Genomes', relativeUrl: '/reference-genomes' },
                { title: 'Starting SILO and LAPIS', relativeUrl: '/starting-silo-and-lapis' },
                { title: 'Preprocessing', relativeUrl: '/preprocessing' },
            ],
            '/references',
        ),
        ...prependToRelativeUrl([{ title: 'Caching', relativeUrl: '/caching' }], '/concepts'),
        ...prependToRelativeUrl(
            [
                { title: 'Start LAPIS and SILO', relativeUrl: '/start-lapis-and-silo' },
                { title: 'Generate your config', relativeUrl: '/generate-your-config' },
            ],
            '/tutorials',
        ),
    ],
    '/maintainer-docs',
);

const pages = [
    ...gettingStartedPages,
    ...referencesPages,
    ...conceptsPages,
    ...userTutorialPages,
    ...architecturePages,
    ...maintainerDocsPages,
];

test.describe('The documentation', () => {
    pages.forEach((documentationPage, indexOfCurrentPage) => {
        test(`should have working relative links and next button on page ${documentationPage.relativeUrl}`, async ({
            page,
        }) => {
            await page.goto(baseUrl.slice(0, -1) + documentationPage.relativeUrl);

            await clickOnAllRelativeLinksInMainBody(page);

            await clickOnNextButton(page, indexOfCurrentPage);
        });
    });

    test('should show all expected pages via link in navigation', async ({ page }) => {
        await page.goto(baseUrl);

        await page.getByRole('link', { name: 'Introduction' }).click();
        await expect(page).toHaveTitle(/^Introduction/);

        await clickOnAllLinksInNavigation(page);
    });
});

function findNeighbouringPages(indexOfCurrentPage: number) {
    if (indexOfCurrentPage === -1) {
        return {};
    }

    const previousPage = indexOfCurrentPage > 0 ? pages[indexOfCurrentPage - 1] : undefined;
    const nextPage = indexOfCurrentPage < pages.length - 1 ? pages[indexOfCurrentPage + 1] : undefined;

    return {
        previousPage,
        nextPage,
    };
}

async function clickOnNextButton(page: Page, indexOfCurrentPage: number) {
    const { nextPage } = findNeighbouringPages(indexOfCurrentPage);

    if (nextPage !== undefined) {
        await page.getByRole('link', { name: `Next ${nextPage.title}` }).click();
        await expect(page).toHaveTitle(new RegExp(`^${nextPage.title}`));
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
        const alreadyClickedTimes = clickedLinks[pageName.relativeUrl] || 0;
        await page.getByRole('link', { name: pageName.title, exact: true }).nth(alreadyClickedTimes).click();
        clickedLinks[pageName.relativeUrl] = alreadyClickedTimes + 1;
        await expect(page).toHaveTitle(new RegExp(`^${pageName.title}`));
    }
}
