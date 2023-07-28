import type { ReactNode } from 'react';
import type { Config } from '../../config';

type Selection = 'aggregatedAll' | 'aggregatedStratified' | 'details' | 'nucleotideSequences' | 'aminoAcidSequences';

type DetailsType = 'all' | 'selected';

export type QueryTypeSelectionState = {
    selection: Selection;
    aggregatedAll: {};
    aggregatedStratified: {
        fields: Set<string>;
    };
    details: {
        type: DetailsType;
        fields: Set<string>;
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
        { header: 'Number of sequences', selection: 'aggregatedAll', content: <></> },
        {
            header: 'Number of sequences per ...',
            selection: 'aggregatedStratified',
            content: <AggregatedStratified {...props} />,
        },
        { header: 'Metadata', selection: 'details', content: <Details {...props} /> },
    ] as const;

    return (
        <div>
            <div>Which type of data would you like to get?</div>
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
        <div className={`border border-solid p-4`}>
            <div className='flex gap-4'>
                <input
                    type='checkbox'
                    checked={selected}
                    onChange={() => onSelection()}
                    className='checkbox border border-solid'
                />
                <div className='font-bold inline-block'>{header}</div>
            </div>
            <div>{children}</div>
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
        <>
            <div>Examples:</div>
            <ul>
                <li>Give me the number of sequences per lineage</li>
                <li>Give me the number of sequences per country and date</li>
            </ul>
            <div>
                <div>By which field(s) would you like to stratify?</div>
                <div className='flex flex-wrap ml-8 gap-8'>
                    {config.schema.metadata.map((m) => (
                        <div key={m.name}>
                            <input
                                type='checkbox'
                                className='checkbox checkbox-sm border border-solid'
                                disabled={state.selection !== 'aggregatedStratified'}
                                checked={state.aggregatedStratified.fields.has(m.name)}
                                onChange={() => changeAggregatedStratifiedField(m.name)}
                            />
                            {m.name}
                        </div>
                    ))}
                </div>
            </div>
        </>
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
        <>
            <div>
                <div>
                    <input
                        type='checkbox'
                        disabled={state.selection !== 'details'}
                        checked={state.details.type === 'all'}
                        onChange={() => changeDetailsType('all')}
                        className='checkbox checkbox-sm ml-8 mt-4 border border-solid'
                    />{' '}
                    Give me all available metadata
                </div>
                <div>
                    <input
                        type='checkbox'
                        disabled={state.selection !== 'details'}
                        checked={state.details.type === 'selected'}
                        onChange={() => changeDetailsType('selected')}
                        className='checkbox checkbox-sm ml-8 border border-solid'
                    />{' '}
                    Let me specify the fields
                </div>
                <div className='flex flex-wrap ml-8 gap-8'>
                    {config.schema.metadata.map((m) => (
                        <div key={m.name}>
                            <input
                                type='checkbox'
                                className='checkbox checkbox-sm border border-solid'
                                disabled={state.selection !== 'details' || state.details.type !== 'selected'}
                                checked={state.details.fields.has(m.name)}
                                onChange={() => changeDetailsField(m.name)}
                            />
                            {m.name}
                        </div>
                    ))}
                </div>
            </div>
        </>
    );
};
