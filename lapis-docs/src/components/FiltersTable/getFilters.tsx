import type { Config, MetadataType } from '../../config.ts';

const filtersWithFromAndTo = ['date', 'int', 'float'];

type MetadataFilterDescription = {
    name: string;
    type: MetadataType;
    description?: string;
};

export function getFilters(config: Config) {
    return config.schema.metadata.flatMap((metadata): MetadataFilterDescription[] => {
        if (filtersWithFromAndTo.includes(metadata.type)) {
            return [
                { name: metadata.name, type: metadata.type },
                {
                    name: `${metadata.name}From`,
                    type: metadata.type,
                    description: `filtering the "${metadata.name}" column (including the boundary)`,
                },
                {
                    name: `${metadata.name}To`,
                    type: metadata.type,
                    description: `filtering the "${metadata.name}" column (including the boundary)`,
                },
            ];
        }

        if (metadata.type === 'insertion' || metadata.type === 'aaInsertion') {
            return [];
        }

        return [{ name: metadata.name, type: metadata.type }];
    });
}
