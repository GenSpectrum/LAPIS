import { useState } from 'react';
import { QueryTypeSelection } from './QueryTypeSelection';
import { type Filters, FiltersSelection } from './FiltersSelection';
import { Result } from './Result';
import { type Config } from '../../config';
import { type OrderByLimitOffset, OrderLimitOffsetSelection } from './OrderLimitOffsetSelection.tsx';
import { getInitialQueryState, type QueryTypeSelectionState } from './QueryTypeSelectionState.ts';

type Props = {
    config: Config;
    lapisUrl: string;
};

export const QueryGenerator = ({ config, lapisUrl }: Props) => {
    const [step, setStep] = useState(0);
    const [queryType, setQueryType] = useState<QueryTypeSelectionState>(getInitialQueryState());
    const [filters, setFilters] = useState<Filters>(new Map());
    const [orderByLimitOffset, setOrderByLimitOffset] = useState<OrderByLimitOffset>({
        orderBy: [],
        limit: undefined,
        offset: undefined,
    });

    return (
        <div className='not-content'>
            <div>
                <ul className='steps w-full'>
                    <li className={`step step-primary`}>Query type</li>
                    <li className={`step ${step >= 1 ? 'step-primary' : ''}`}>Filters</li>
                    <li className={`step ${step >= 2 ? 'step-primary' : ''}`}>Order by, limit and offset</li>
                    <li className={`step ${step >= 3 ? 'step-primary' : ''}`}>Result</li>
                </ul>
            </div>

            <div className='mt-8'>
                {step === 0 && <QueryTypeSelection config={config} state={queryType} onStateChange={setQueryType} />}
                {step === 1 && <FiltersSelection config={config} filters={filters} onFiltersChange={setFilters} />}
                {step === 2 && (
                    <OrderLimitOffsetSelection
                        config={config}
                        queryType={queryType}
                        orderByLimitOffset={orderByLimitOffset}
                        onOrderByLimitOffsetChange={setOrderByLimitOffset}
                    />
                )}
                {step === 3 && (
                    <Result
                        queryType={queryType}
                        filters={filters}
                        config={config}
                        lapisUrl={lapisUrl}
                        orderByLimitOffset={orderByLimitOffset}
                    />
                )}
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
