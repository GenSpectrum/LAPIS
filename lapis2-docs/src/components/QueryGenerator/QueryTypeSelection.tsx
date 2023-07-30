import type { ReactNode } from 'react';
import type { Config } from '../../config';
import { CheckBoxesWrapper, ContainerWrapper, LabeledCheckBox, LabelWrapper } from './styled-components';

type Selection =
    | 'aggregatedAll'
    | 'aggregatedStratified'
    | 'mutations'
    | 'insertions'
    | 'details'
    | 'nucleotideSequences'
    | 'aminoAcidSequences';

const sequenceTypes = ['nucleotide', 'aminoAcid'] as const;
type SequenceType = (typeof sequenceTypes)[number];
type DetailsType = 'all' | 'selected';
const alignmentTypes = ['unaligned', 'aligned'] as const;
type AlignmentType = (typeof alignmentTypes)[number];

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

type Props = {
    config: Config;
    state: QueryTypeSelectionState;
    onStateChange: (state: QueryTypeSelectionState) => void;
};

export const QueryTypeSelection = (props: Props) => {
    const { state, onStateChange } = props;
    const changeSelection = (selection: Selection) => {
        onStateChange({ ...state, selection });
    };

    const options = [
        { header: 'Number of sequences', selection: 'aggregatedAll', content: undefined },
        {
            header: 'Number of sequences per ...',
            selection: 'aggregatedStratified',
            content: <AggregatedStratified {...props} />,
        },
        { header: 'Metadata', selection: 'details', content: <Details {...props} /> },
        { header: 'Substitutions and deletions', selection: 'mutations', content: <Mutations {...props} /> },
        { header: 'Insertions', selection: 'insertions', content: <Insertions {...props} /> },
        {
            header: 'Nucleotide sequences',
            selection: 'nucleotideSequences',
            content: <NucleotideSequences {...props} />,
        },
        { header: 'Amino acid sequences', selection: 'aminoAcidSequences', content: <AminoAcidSequences {...props} /> },
    ] as const;

    return (
        <div>
            <div className='mb-4'>Which type of data would you like to get?</div>
            <div className='flex flex-col gap-4'>
                {options.map((option) => (
                    <Option
                        key={option.selection}
                        header={option.header}
                        selected={state.selection === option.selection}
                        onSelection={() => changeSelection(option.selection)}
                    >
                        {option.content}
                    </Option>
                ))}
            </div>
        </div>
    );
};

type OptionProps = {
    header: string;
    children?: ReactNode;
    selected: boolean;
    onSelection: () => void;
};

const Option = ({ header, children, selected, onSelection }: OptionProps) => {
    return (
        <div className={`border border-solid rounded-xl ${selected ? 'border-blue-100 ' : 'border-blue-50 '}`}>
            <label
                className={`flex items-center gap-4 p-4 rounded-xl ${
                    selected ? 'bg-blue-100 font-bold ' : 'bg-blue-50 font-medium'
                }`}
            >
                <input
                    type='checkbox'
                    checked={selected}
                    onChange={() => onSelection()}
                    className='checkbox border border-solid'
                />
                <div className='inline-block'>{header}</div>
            </label>
            {children && <div className={`pb-4 px-4 mt-4`}>{children}</div>}
        </div>
    );
};

const AggregatedStratified = ({ config, state, onStateChange }: Props) => {
    const changeAggregatedStratifiedField = (field: string) => {
        const newFields = new Set(state.aggregatedStratified.fields);
        if (newFields.has(field)) {
            newFields.delete(field);
        } else {
            newFields.add(field);
        }
        onStateChange({
            ...state,
            aggregatedStratified: {
                ...state.aggregatedStratified,
                fields: newFields,
            },
        });
    };

    return (
        <ContainerWrapper>
            <div>
                <LabelWrapper>By which field(s) would you like to stratify?</LabelWrapper>
                <CheckBoxesWrapper>
                    {config.schema.metadata.map((m) => (
                        <LabeledCheckBox
                            label={m.name}
                            key={m.name}
                            type='checkbox'
                            className='w-40'
                            disabled={state.selection !== 'aggregatedStratified'}
                            checked={state.aggregatedStratified.fields.has(m.name)}
                            onChange={() => changeAggregatedStratifiedField(m.name)}
                        />
                    ))}
                </CheckBoxesWrapper>
            </div>
        </ContainerWrapper>
    );
};

const Details = ({ config, state, onStateChange }: Props) => {
    const changeDetailsType = (type: DetailsType) => {
        onStateChange({
            ...state,
            details: { ...state.details, type },
        });
    };

    const changeDetailsField = (field: string) => {
        const newFields = new Set(state.details.fields);
        if (newFields.has(field)) {
            newFields.delete(field);
        } else {
            newFields.add(field);
        }
        onStateChange({
            ...state,
            details: {
                ...state.details,
                fields: newFields,
            },
        });
    };

    return (
        <ContainerWrapper>
            <div>
                <LabelWrapper>Which metadata would you like to get?</LabelWrapper>
                <CheckBoxesWrapper>
                    <LabeledCheckBox
                        label='Give me all available metadata'
                        type='checkbox'
                        disabled={state.selection !== 'details'}
                        checked={state.details.type === 'all'}
                        onChange={() => changeDetailsType('all')}
                        className='w-80'
                    />
                    <LabeledCheckBox
                        label='Let me specify the fields'
                        type='checkbox'
                        disabled={state.selection !== 'details'}
                        checked={state.details.type === 'selected'}
                        onChange={() => changeDetailsType('selected')}
                        className='w-80'
                    />
                </CheckBoxesWrapper>
            </div>
            <div>
                <LabelWrapper>Which fields?</LabelWrapper>
                <CheckBoxesWrapper>
                    {config.schema.metadata.map((m) => (
                        <LabeledCheckBox
                            label={m.name}
                            key={m.name}
                            type='checkbox'
                            className='w-40'
                            disabled={state.selection !== 'details' || state.details.type !== 'selected'}
                            checked={state.details.fields.has(m.name)}
                            onChange={() => changeDetailsField(m.name)}
                        />
                    ))}
                </CheckBoxesWrapper>
            </div>
        </ContainerWrapper>
    );
};

const Mutations = ({ state, onStateChange }: Props) => {
    const changeType = (type: SequenceType) => {
        onStateChange({
            ...state,
            mutations: { ...state.mutations, type },
        });
    };

    const changeMinProportion = (minProportion: string) => {
        onStateChange({
            ...state,
            mutations: { ...state.mutations, minProportion },
        });
    };

    return (
        <ContainerWrapper>
            <div>
                <LabelWrapper>Would you like to get the nucleotide or AA mutations?</LabelWrapper>
                <CheckBoxesWrapper>
                    {sequenceTypes.map((t) => (
                        <LabeledCheckBox
                            label={t}
                            key={t}
                            type='checkbox'
                            className='w-80'
                            disabled={state.selection !== 'mutations'}
                            checked={state.mutations.type === t}
                            onChange={() => changeType(t)}
                        />
                    ))}
                </CheckBoxesWrapper>
            </div>

            <div>
                <LabelWrapper>
                    What is the minimal proportion of samples in which the mutations should occur?
                </LabelWrapper>
                <input
                    type='text'
                    className='input input-bordered w-full max-w-xs'
                    disabled={state.selection !== 'mutations'}
                    value={state.mutations.minProportion}
                    onChange={(e) => changeMinProportion(e.target.value)}
                />
            </div>
        </ContainerWrapper>
    );
};

const Insertions = ({ state, onStateChange }: Props) => {
    const changeType = (type: SequenceType) => {
        onStateChange({
            ...state,
            insertions: { ...state.insertions, type },
        });
    };

    return (
        <ContainerWrapper>
            <div>
                <LabelWrapper>Would you like to get the nucleotide or AA insertions?</LabelWrapper>
                <CheckBoxesWrapper>
                    {sequenceTypes.map((t) => (
                        <LabeledCheckBox
                            label={t}
                            key={t}
                            type='checkbox'
                            className='w-80'
                            disabled={state.selection !== 'insertions'}
                            checked={state.insertions.type === t}
                            onChange={() => changeType(t)}
                        />
                    ))}
                </CheckBoxesWrapper>
            </div>
        </ContainerWrapper>
    );
};

const NucleotideSequences = ({ state, onStateChange }: Props) => {
    const changeType = (type: AlignmentType) => {
        onStateChange({
            ...state,
            nucleotideSequences: { ...state.nucleotideSequences, type },
        });
    };

    return (
        <ContainerWrapper>
            <div>
                <LabelWrapper>Would you like to get the unaligned (original) or aligned sequences?</LabelWrapper>
                <CheckBoxesWrapper>
                    {alignmentTypes.map((t) => (
                        <LabeledCheckBox
                            label={t}
                            key={t}
                            type='checkbox'
                            className='w-80'
                            disabled={state.selection !== 'nucleotideSequences'}
                            checked={state.nucleotideSequences.type === t}
                            onChange={() => changeType(t)}
                        />
                    ))}
                </CheckBoxesWrapper>
            </div>
        </ContainerWrapper>
    );
};

const AminoAcidSequences = ({ state, onStateChange }: Props) => {
    const changeGene = (gene: string) => {
        onStateChange({
            ...state,
            aminoAcidSequences: { ...state.aminoAcidSequences, gene },
        });
    };

    return (
        <ContainerWrapper>
            <div>
                <LabelWrapper>Which gene/reading frame are you interested in?</LabelWrapper>
                <input
                    type='text'
                    className='input input-bordered w-full max-w-xs'
                    disabled={state.selection !== 'aminoAcidSequences'}
                    value={state.aminoAcidSequences.gene}
                    onChange={(e) => changeGene(e.target.value)}
                />
            </div>
        </ContainerWrapper>
    );
};
