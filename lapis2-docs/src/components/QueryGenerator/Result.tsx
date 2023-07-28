import type { QueryTypeSelectionState } from './QueryTypeSelection';
import type { Filters } from './FiltersSelection';
import type { MetadataOutputFormat } from './OutputFormatSelection';
import { useState } from 'react';

type Props = {
    queryType: QueryTypeSelectionState;
    filters: Filters;
    outputFormat: MetadataOutputFormat;
};

const tabs = ['Query URL', 'R code', 'Python code'] as const;

export const Result = (props: Props) => {
    const [activeTab, setActiveTab] = useState<(typeof tabs)[number]>(tabs[0]);

    return (
        <div>
            <div className='tabs'>
                {tabs.map((tab) => (
                    <a
                        key={tab}
                        className={`tab tab-lifted ${tab === activeTab ? 'tab-active' : ''}`}
                        onClick={() => setActiveTab(tab)}
                    >
                        {tab}
                    </a>
                ))}
            </div>
            <div className='-mt-px border border-solid border-gray-200 p-8'>
                {activeTab === tabs[0] && <QueryUrlTab {...props} />}
                {activeTab === tabs[1] && <RTab {...props} />}
                {activeTab === tabs[2] && <PythonTab {...props} />}
            </div>
        </div>
    );
};

function constructGetQueryUrl({ queryType, filters, outputFormat }: Props) {
    let endpoint;
    const params = new URLSearchParams();
    switch (queryType.selection) {
        case 'aggregatedAll':
            endpoint = 'aggregated';
            break;
        case 'aggregatedStratified':
            endpoint = 'aggregated';
            const aggregatedFields = queryType.aggregatedStratified.fields;
            if (aggregatedFields.size > 0) {
                params.append('fields', [...aggregatedFields].join(','));
            }
            break;
        case 'details':
            endpoint = 'details';
            const detailsFields = queryType.details.fields;
            if (detailsFields.size > 0) {
                params.append('fields', [...detailsFields].join(','));
            }
            break;
    }
    for (let [name, value] of filters) {
        params.set(name, value);
    }
    if (endpoint !== 'fasta' && endpoint !== 'fasta-aligned' && outputFormat !== 'json') {
        params.set('dataFormat', outputFormat);
    }
    return `/${endpoint}?${params}`;
}

const QueryUrlTab = (props: Props) => {
    // TODO Prepend the URL to the instance
    const queryUrl = constructGetQueryUrl(props);

    return (
        <div>
            <input
                type='text'
                className='input input-bordered w-full'
                value={queryUrl}
                readOnly
                onFocus={(e) => e.target.select()}
            />
            <a href={queryUrl} target='_blank'>
                <button className='btn btn-sm mt-4'>Open in browser</button>
            </a>
        </div>
    );
};

const RTab = (props: Props) => {
    return <div>TODO R code</div>;
};

const PythonTab = (props: Props) => {
    return <div>TODO Python code</div>;
};
