import type { Config, MetadataType } from '../../config.ts';
import type { ResultField, ResultFieldType } from '../../utils/code-generators/types.ts';

export type Selection = keyof Omit<QueryTypeSelectionState, 'selection'>;

export const sequenceTypes = ['nucleotide', 'aminoAcid'] as const;
export type SequenceType = (typeof sequenceTypes)[number];
export type DetailsType = 'all' | 'selected';
export const alignmentTypes = ['unaligned', 'aligned'] as const;
export type AlignmentType = (typeof alignmentTypes)[number];

export type QueryTypeSelectionState = {
    selection: Selection;
    aggregatedAll: {};
    aggregatedStratified: {
        fields: Set<string>;
    };
    mutations: {
        type: SequenceType;
        minProportion: string;
    };
    insertions: {
        type: SequenceType;
    };
    details: {
        type: DetailsType;
        fields: Set<string>;
    };
    nucleotideSequences: {
        type: AlignmentType;
    };
    aminoAcidSequences: {
        gene: string;
    };
};

export function getInitialQueryState(): QueryTypeSelectionState {
    return {
        selection: 'aggregatedAll',
        aggregatedAll: {},
        aggregatedStratified: {
            fields: new Set<string>(),
        },
        mutations: {
            type: 'nucleotide',
            minProportion: '0.05',
        },
        insertions: {
            type: 'nucleotide',
        },
        details: {
            type: 'all',
            fields: new Set<string>(),
        },
        nucleotideSequences: {
            type: 'unaligned',
        },
        aminoAcidSequences: {
            gene: '',
        },
    };
}

export function getResultFields(selectionState: QueryTypeSelectionState, config: Config): ResultField[] {
    const fieldsThatAreAlwaysPresent = getFieldsThatAreAlwaysPresent(selectionState.selection);

    const selectionStateElement = selectionState[selectionState.selection] as { fields?: Set<string> };
    let fields = selectionStateElement.fields ? Array.from(selectionStateElement.fields) : [];
    const resultFields = fields.map((fieldName) => ({
        name: fieldName,
        type: getType(fieldName, config),
        nullable: true,
    }));

    return [...fieldsThatAreAlwaysPresent, ...resultFields];
}

function getFieldsThatAreAlwaysPresent(selection: Selection): ResultField[] {
    switch (selection) {
        case 'aggregatedAll':
            return [{ name: 'count', type: 'integer', nullable: false }];
        case 'aggregatedStratified':
            return [{ name: 'count', type: 'integer', nullable: false }];
        case 'mutations':
            return [
                { name: 'mutation', type: 'string', nullable: false },
                { name: 'proportion', type: 'string', nullable: false },
                { name: 'count', type: 'integer', nullable: false },
            ];
        case 'insertions':
            return [
                { name: 'insertion', type: 'string', nullable: false },
                { name: 'count', type: 'integer', nullable: false },
            ];
        case 'details':
            return [];
        case 'nucleotideSequences':
            return [];
        case 'aminoAcidSequences':
            return [];
    }
}

function getType(fieldName: string, config: Config) {
    let matchingMetadataField = config.schema.metadata.find((metadataField) => metadataField.name === fieldName)!;
    return mapMetadataTypeToResultFieldType(matchingMetadataField.type);
}

function mapMetadataTypeToResultFieldType(type: MetadataType): ResultFieldType {
    switch (type) {
        case 'pango_lineage':
        case 'date':
        case 'string':
            return 'string';
    }
}
