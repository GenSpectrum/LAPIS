import { useState } from 'react';
import { QueryType, QueryTypeSelection } from './QueryTypeSelection';
import { FiltersSelection } from './FiltersSelection';
import { OutputFormatSelection } from './OutputFormatSelection';
import { Result } from './Result';

export const QueryGenerator = () => {
    const [step, setStep] = useState(0);
    const [queryType, setQueryType] = useState<QueryType>('aggregated');

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

            <div className='mt-4'>
                {step === 0 && <QueryTypeSelection selection={queryType} onSelection={setQueryType} />}
                {step === 1 && <FiltersSelection />}
                {step === 2 && <OutputFormatSelection />}
                {step === 3 && <Result />}
            </div>

            <div className='w-full flex justify-between mt-4'>
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
