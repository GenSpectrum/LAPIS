import type { Config, MetadataType } from '../../config.ts';
import { isMultiSegmented, type ReferenceGenomes } from '../../reference_genomes.ts';

export const filtersWithFromAndTo = ['date', 'int', 'float'];

export type MetadataFilterDescription = {
    name: string;
    type: MetadataType;
    description?: string;
    link?: { href: string; text: string };
};

export function getFilters(config: Config) {
    return config.schema.metadata.flatMap((metadata): MetadataFilterDescription[] => {
        if (filtersWithFromAndTo.includes(metadata.type)) {
            return [
                {
                    name: metadata.name,
                    type: metadata.type,
                    description: `Filters the "${metadata.name}" column" with exact match`,
                },
                {
                    name: `${metadata.name}From`,
                    type: metadata.type,
                    description: `Filters the "${metadata.name}" column (including the boundary)`,
                },
                {
                    name: `${metadata.name}To`,
                    type: metadata.type,
                    description: `Filters the "${metadata.name}" column (including the boundary)`,
                },
            ];
        }

        if (metadata.type === 'string') {
            const stringDescription = {
                name: metadata.name,
                type: metadata.type,
                description: `Filters the "${metadata.name}" column" with exact match`,
            };

            if (metadata.lapisAllowsRegexSearch) {
                const allowRegexSearchDescription = {
                    name: `${metadata.name}.regex`,
                    type: metadata.type,
                    description: `Filters the "${metadata.name}" column using a regular expression,`,
                    link: {
                        href: '/concepts/string-search',
                        text: 'string search',
                    },
                };

                return [stringDescription, allowRegexSearchDescription];
            }

            return [stringDescription];
        }

        if (metadata.type === 'pango_lineage') {
            return [
                {
                    name: metadata.name,
                    type: metadata.type,
                    link: { href: '/concepts/pango-lineage-query', text: 'pango lineage query' },
                    description: `Filters the "${metadata.name}" column for a pango lineage,`,
                },
            ];
        }

        if (metadata.type === 'boolean') {
            return [
                {
                    name: metadata.name,
                    type: metadata.type,
                    description: `Filters the "${metadata.name}" column for a boolean value`,
                },
            ];
        }

        return [{ name: metadata.name, type: metadata.type }];
    });
}

export const getCommonFilterDescription = (referenceGenomes: ReferenceGenomes) => {
    const multiSegmented = isMultiSegmented(referenceGenomes);

    return [
        {
            name: 'nucleotideMutations',
            type: 'list of strings',
            description: 'Filters for nucleotide mutations,',
            link: {
                href: '/concepts/mutation-filters',
                text: 'mutation filters',
            },
        },
        {
            name: 'aminoAcidMutations',
            type: 'list of strings',
            description: 'Filters for amino acid mutations,',
            link: {
                href: '/concepts/mutation-filters',
                text: 'mutation filters',
            },
        },
        {
            name: 'nucleotideInsertions',
            type: 'list of strings',
            description:
                `In the format ins_${multiSegmented && '<sequenceName>:'}{'<position>:<insertion>'}. ` +
                `Example: ins_${multiSegmented ? `${referenceGenomes.nucleotideSequences[0].name}:` : ''}100:AGG`,
        },
        {
            name: 'aminoAcidInsertions',
            type: 'list of strings',
            description:
                `In the format ins_<sequenceName>:<position><insertion>. ` +
                `Example: ins_${referenceGenomes.genes[0].name}:100:DEF`,
        },
    ];
};
