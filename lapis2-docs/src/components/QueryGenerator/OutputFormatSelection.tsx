import type { QueryTypeSelectionState } from './QueryTypeSelection';

const availableFormats = ['tsv', 'csv', 'json'] as const;
export type MetadataOutputFormat = (typeof availableFormats)[number];

type Props = {
    queryType: QueryTypeSelectionState;
    format: MetadataOutputFormat;
    onFormatChange: (format: MetadataOutputFormat) => void;
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
                    <div className='flex flex-col gap-4'>
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
