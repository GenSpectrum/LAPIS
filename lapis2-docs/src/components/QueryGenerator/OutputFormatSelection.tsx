import type { QueryTypeSelectionState } from './QueryTypeSelection';

const availableFormats = ['json', 'tsv', 'csv'] as const;
export type TabularOutputFormat = (typeof availableFormats)[number];

type Props = {
    queryType: QueryTypeSelectionState;
    format: TabularOutputFormat;
    onFormatChange: (format: TabularOutputFormat) => void;
};

export const OutputFormatSelection = ({ queryType, format, onFormatChange }: Props) => {
    return (
        <div>
            {queryType.selection === 'nucleotideSequences' || queryType.selection === 'aminoAcidSequences' ? (
                <>
                    For sequences, only <b>FASTA</b> is available as output format.
                </>
            ) : (
                <>
                    <div>Which format do you prefer?</div>
                    <div className='flex gap-4'>
                        {availableFormats.map((f) => (
                            <div>
                                <input
                                    type='checkbox'
                                    className='checkbox checkbox-sm border border-solid'
                                    checked={f === format}
                                    onChange={() => onFormatChange(f)}
                                />{' '}
                                {f}
                            </div>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
};
