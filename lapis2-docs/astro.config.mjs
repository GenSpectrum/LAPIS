import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import react from '@astrojs/react';
import { hasFeature } from './src/config.ts';

import tailwind from '@astrojs/tailwind';

// https://astro.build/config
export default defineConfig({
    integrations: [
        starlight({
            title: 'LAPIS',
            social: {
                github: 'https://github.com/GenSpectrum/LAPIS',
            },
            customCss: ['./src/styles/custom.css'],
            editLink: {
                baseUrl: 'https://github.com/GenSpectrum/LAPIS/tree/main/lapis2-docs/',
            },
            sidebar: [
                {
                    label: 'Getting started',
                    items: [
                        {
                            label: 'Introduction',
                            link: '/getting-started/introduction',
                        },
                        {
                            label: 'Generate your request',
                            link: '/getting-started/generate-your-request',
                        },
                    ],
                },
                {
                    label: 'References',
                    items: [
                        {
                            label: 'Introduction',
                            link: '/references/introduction/',
                        },
                        {
                            label: 'Fields',
                            link: '/references/fields/',
                        },
                        {
                            label: 'Filters',
                            link: '/references/filters/',
                        },
                        {
                            label: 'Additional Request Properties',
                            link: '/references/additional-request-properties/',
                        },
                        {
                            label: 'Open API / Swagger',
                            link: '/references/open-api-definition/',
                        },
                        {
                            label: 'Database Config',
                            link: '/references/database-config/',
                        },
                        {
                            label: 'Reference Genomes',
                            link: '/references/reference-genomes/',
                        },
                        {
                            label: 'Nucleotide And Amino Acid Symbols',
                            link: '/references/nucleotide-and-amino-acid-symbols/',
                        },
                    ],
                },
                {
                    label: 'Concepts',
                    items: filterAvailableConcepts([
                        {
                            label: 'Data versions',
                            link: '/concepts/data-versions/',
                        },
                        {
                            label: 'Mutation filters',
                            link: '/concepts/mutation-filters/',
                        },
                        {
                            label: 'Ambiguous symbols',
                            link: '/concepts/ambiguous-symbols/',
                        },
                        {
                            label: 'Pango lineage query',
                            link: '/concepts/pango-lineage-query/',
                            onlyIfFeature: 'sarsCoV2VariantQuery',
                        },
                        {
                            label: 'Request methods: GET and POST',
                            link: '/concepts/request-methods/',
                        },
                        {
                            label: 'Response format',
                            link: '/concepts/response-format/',
                        },
                        {
                            label: 'Variant query',
                            link: '/concepts/variant-query/',
                            onlyIfFeature: 'sarsCoV2VariantQuery',
                        },
                        {
                            label: 'Request Id',
                            link: '/concepts/request-id/',
                        },
                    ]),
                },
                {
                    label: 'Tutorials',
                    items: [
                        {
                            label: 'Plot the global distribution of all sequences in R',
                            link: '/tutorials/plot-global-distribution-of-sequences-in-r',
                        },
                    ],
                },
                {
                    label: 'Architecture and Dev Docs',
                    items: [
                        {
                            label: 'Introduction and Goals',
                            link: '/architecture-and-dev-docs/01-introduction',
                        },
                        {
                            label: 'Architecture and Constraints',
                            link: '/architecture-and-dev-docs/02-architecture-and-constraints',
                        },
                        {
                            label: 'System Scope and Context',
                            link: '/architecture-and-dev-docs/03-system-scope-and-context',
                        },
                        {
                            label: 'Solution Strategy',
                            link: '/architecture-and-dev-docs/04-solution-strategy',
                        },
                        {
                            label: 'Building Block View',
                            link: '/architecture-and-dev-docs/05-building-block-view',
                        },
                        {
                            label: 'Runtime View',
                            link: '/architecture-and-dev-docs/06-runtime-view',
                        },
                        {
                            label: 'Glossary',
                            link: '/architecture-and-dev-docs/99-glossary',
                        },
                    ],
                },
                {
                    label: 'Maintainer Documentation',
                    items: [
                        {
                            label: 'References',
                            items: [
                                {
                                    label: 'Database Configuration',
                                    link: '/maintainer-docs/references/database-configuration',
                                },
                                {
                                    label: 'Reference Genomes',
                                    link: '/maintainer-docs/references/reference-genomes',
                                },
                                {
                                    label: 'Starting SILO and LAPIS',
                                    link: '/maintainer-docs/references/starting-silo-and-lapis',
                                },
                                {
                                    label: 'Preprocessing',
                                    link: '/maintainer-docs/references/preprocessing',
                                },
                            ],
                        },
                        {
                            label: 'Concepts',
                            items: [
                                {
                                    label: 'Caching',
                                    link: '/maintainer-docs/concepts/caching',
                                },
                            ],
                        },
                        {
                            label: 'Tutorials',
                            items: [
                                {
                                    label: 'Start LAPIS and SILO',
                                    link: '/maintainer-docs/tutorials/start-lapis-and-silo',
                                },
                                {
                                    label: 'Generate your config',
                                    link: '/maintainer-docs/tutorials/generate-your-config',
                                },
                            ],
                        },
                    ],
                },
            ],
        }),
        react(),
        tailwind(),
    ],
    // Process images with sharp: https://docs.astro.build/en/guides/assets/#using-sharp
    image: {
        service: {
            entrypoint: 'astro/assets/services/sharp',
        },
    },
    base: process.env.BASE_URL,
    site: process.env.ASTRO_SITE,
});

/**
 * TODO Not sure if this is actually a good solution. The filtering now happens at compile time but ideally, it happens
 *   at runtime (so that the user/maintainer does not need to re-compile for their own config).
 */
function filterAvailableConcepts(pages) {
    return pages
        .filter((p) => !p.onlyIfFeature || hasFeature(p.onlyIfFeature))
        .map(({ label, link }) => ({
            label,
            link,
        }));
}
