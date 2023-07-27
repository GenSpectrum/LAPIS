import type { ReactNode } from 'react';

export type QueryType =
    | { type: 'aggregated'; fields: string | undefined }
    | { type: 'details'; fields: string | undefined }
    | { type: 'aa-mutations'; minProportion: number }
    | { type: 'nuc-mutations'; minProportion: number }
    | { type: 'fasta' }
    | { type: 'fasta-aligned' };

type Props = {
    selection: QueryType;
    onSelection: (type: QueryType) => void;
};

export const QueryTypeSelection = ({ selection, onSelection }: Props) => {
    return (
        <div>
            <div>Which type of data would you like to get?</div>
            <div className='flex flex-col gap-4'>
                <Option
                    header='Number of sequences'
                    selected={selection.type === 'aggregated' && selection.fields === undefined}
                    onSelection={() =>
                        onSelection({
                            type: 'aggregated',
                            fields: undefined,
                        })
                    }
                />
                <Option
                    header='Number of sequences per ...'
                    selected={selection.type === 'aggregated' && selection.fields !== undefined}
                    onSelection={() =>
                        onSelection({
                            type: 'aggregated',
                            fields: '',
                        })
                    }
                >
                    <div>Examples:</div>
                    <ul>
                        <li>Give me the number of sequences per lineage</li>
                        <li>Give me the number of sequences per country and date</li>
                    </ul>
                    {selection.type === 'aggregated' && selection.fields !== undefined && (
                        <div>
                            <div>By which field(s) would you like to stratify?</div>
                            <input
                                type='text'
                                placeholder='List of fields, comma-separated (TODO auto-completion needed)'
                                className='input input-bordered w-full'
                                value={selection.fields}
                                onChange={(e) =>
                                    onSelection({
                                        type: 'aggregated',
                                        fields: e.target.value,
                                    })
                                }
                            />
                        </div>
                    )}
                </Option>
                <Option
                    header='Metadata'
                    selected={selection.type === 'details'}
                    onSelection={() =>
                        onSelection({
                            type: 'details',
                            fields: undefined,
                        })
                    }
                >
                    {selection.type === 'details' && (
                        <div>
                            <div>
                                <input
                                    type='checkbox'
                                    checked={selection.type === 'details' && selection.fields === undefined}
                                    onChange={() =>
                                        onSelection({
                                            type: 'details',
                                            fields: undefined,
                                        })
                                    }
                                    className='checkbox checkbox-sm ml-8 mt-4 border border-solid'
                                />{' '}
                                Give me all available metadata
                            </div>
                            <div>
                                <input
                                    type='checkbox'
                                    checked={selection.type === 'details' && selection.fields !== undefined}
                                    onChange={() =>
                                        onSelection({
                                            type: 'details',
                                            fields: '',
                                        })
                                    }
                                    className='checkbox checkbox-sm ml-8 border border-solid'
                                />{' '}
                                Let me specify the fields
                            </div>
                            {selection.type === 'details' && selection.fields !== undefined && (
                                <input
                                    type='text'
                                    placeholder='List of fields, comma-separated (TODO auto-completion needed)'
                                    className='input input-bordered w-full'
                                    value={selection.fields}
                                    onChange={(e) =>
                                        onSelection({
                                            type: 'details',
                                            fields: e.target.value,
                                        })
                                    }
                                />
                            )}
                        </div>
                    )}
                </Option>
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
