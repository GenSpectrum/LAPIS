import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import react from '@astrojs/react';

import fs from 'fs';

import tailwindcss from '@tailwindcss/vite';

function getVersion() {
    try {
        return `(version ${fs.readFileSync('version.txt').toString().trim().substring(0, 7)})`;
    } catch (e) {
        return '';
    }
}

// https://astro.build/config
export default defineConfig({
    integrations: [
        starlight({
            title: `LAPIS ${getVersion()}`,
            social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/GenSpectrum/LAPIS' }],
            customCss: ['./src/styles/custom.css'],
            editLink: {
                baseUrl: 'https://github.com/GenSpectrum/LAPIS/tree/main/lapis-docs/',
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
                            link: '/references/introduction',
                        },
                        {
                            label: 'Fields',
                            link: '/references/fields',
                        },
                        {
                            label: 'Filters',
                            link: '/references/filters',
                        },
                        {
                            label: 'Additional Request Properties',
                            link: '/references/additional-request-properties',
                        },
                        {
                            label: 'Open API / Swagger',
                            link: '/references/open-api-definition',
                        },
                        {
                            label: 'Database Config',
                            link: '/references/database-config',
                        },
                        {
                            label: 'Reference Genomes',
                            link: '/references/reference-genomes',
                        },
                        {
                            label: 'Nucleotide And Amino Acid Symbols',
                            link: '/references/nucleotide-and-amino-acid-symbols',
                        },
                    ],
                },
                {
                    label: 'Concepts',
                    items: [
                        {
                            label: 'Data versions',
                            link: '/concepts/data-versions',
                        },
                        {
                            label: 'Mutation filters',
                            link: '/concepts/mutation-filters',
                        },
                        {
                            label: 'Insertion filters',
                            link: '/concepts/insertion-filters',
                        },
                        {
                            label: 'Ambiguous symbols',
                            link: '/concepts/ambiguous-symbols',
                        },
                        {
                            label: 'Pango lineage query',
                            link: '/concepts/pango-lineage-query',
                        },
                        {
                            label: 'Request methods: GET and POST',
                            link: '/concepts/request-methods',
                        },
                        {
                            label: 'Response format',
                            link: '/concepts/response-format',
                        },
                        {
                            label: 'Variant query',
                            link: '/concepts/variant-query',
                        },
                        {
                            label: 'Advanced query',
                            link: '/concepts/advanced-query',
                        },
                        {
                            label: 'String search',
                            link: '/concepts/string-search',
                        },
                        {
                            label: 'Request Id',
                            link: '/concepts/request-id',
                        },
                        {
                            label: 'Authentication',
                            link: '/concepts/authentication',
                        },
                    ],
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
                                {
                                    label: 'Authentication',
                                    link: '/maintainer-docs/references/authentication',
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
    ],

    // Process images with sharp: https://docs.astro.build/en/guides/assets/#using-sharp
    image: {
        service: {
            entrypoint: 'astro/assets/services/sharp',
        },
    },

    base: process.env.BASE_URL,
    site: process.env.ASTRO_SITE,

    server: {
        allowedHosts: true,
    },

    vite: {
        plugins: [tailwindcss()],
    },
});
