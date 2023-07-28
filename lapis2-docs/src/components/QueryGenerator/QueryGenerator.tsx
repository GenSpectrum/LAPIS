import { useState } from 'react';
import { QueryTypeSelection, QueryTypeSelectionState } from './QueryTypeSelection';
import { Filters, FiltersSelection } from './FiltersSelection';
import { MetadataOutputFormat, OutputFormatSelection } from './OutputFormatSelection';
import { Result } from './Result';
import type { Config } from '../../config';

type Props = {
    config: Config;
};

export const QueryGenerator = ({ config }: Props) => {
    const [step, setStep] = useState(0);
    const [queryType, setQueryType] = useState<QueryTypeSelectionState>({
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
    });
    const [filters, setFilters] = useState<Filters>(new Map());
    const [outputFormat, setOutputFormat] = useState<MetadataOutputFormat>('tsv');

    return (
        <div className='not-content'>
            <div>
                <ul className='steps w-full'>
                    <li className={`step step-primary`}>Query type</li>
                    <li className={`step ${step >= 1 ? 'step-primary' : ''}`}>Filters</li>
                    <li className={`step ${step >= 2 ? 'step-primary' : ''}`}>Output format</li>
                    <li className={`step ${step >= 3 ? 'step-primary' : ''}`}>Result</li>
                </ul>
            </div>

            <div className='mt-8'>
                {step === 0 && <QueryTypeSelection config={config} state={queryType} onStateChange={setQueryType} />}
                {step === 1 && <FiltersSelection config={config} filters={filters} onFiltersChange={setFilters} />}
                {step === 2 && (
                    <OutputFormatSelection
                        queryType={queryType}
                        format={outputFormat}
                        onFormatChange={setOutputFormat}
                    />
                )}
                {step === 3 && <Result queryType={queryType} filters={filters} outputFormat={outputFormat} />}
            </div>

            <div className='w-full flex justify-between mt-8'>
                <button className='btn' onClick={() => setStep((prev) => prev - 1)} disabled={step === 0}>
                    Previous
                </button>
                <button className='btn' onClick={() => setStep((prev) => prev + 1)} disabled={step === 3}>
                    Next
                </button>
            </div>
        </div>
    );
};
