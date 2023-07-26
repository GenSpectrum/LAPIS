import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

import react from '@astrojs/react';

// https://astro.build/config
export default defineConfig({
    integrations: [
        starlight({
            title: 'My Docs',
            social: {
                github: 'https://github.com/withastro/starlight',
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
                    label: 'Concepts',
                    items: [
                        {
                            label: 'Data versions',
                            link: '/concepts/data-versions/',
                        },
                        {
                            label: 'Mutation filters',
                            link: '/concepts/mutation-filters/',
                        },
                        {
                            label: 'Pango lineage query',
                            link: '/concepts/pango-lineage-query/',
                        },
                        {
                            label: 'Response format',
                            link: '/concepts/response-format/',
                        },
                        {
                            label: 'Variant query',
                            link: '/concepts/variant-query/',
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
});
