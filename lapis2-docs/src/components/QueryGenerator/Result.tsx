import type { Filters } from './FiltersSelection';
import type { TabularOutputFormat } from './OutputFormatSelection';
import { OutputFormatSelection } from './OutputFormatSelection';
import { CodeBlock } from '../CodeBlock';
import { Tab, TabsBox } from '../TabsBox/react/TabsBox';
import { generateNonFastaQuery } from '../../utils/code-generators/python/generator';
import type { Config } from '../../config';
import { useState } from 'react';
import type { ResultField } from '../../utils/code-generators/types';
import { ContainerWrapper, LabelWrapper } from './styled-components';
import { getResultFields, type QueryTypeSelectionState } from './QueryTypeSelectionState.ts';
import type { OrderByLimitOffset } from './OrderLimitOffsetSelection.tsx';

type Props = {
    queryType: QueryTypeSelectionState;
    filters: Filters;
    orderByLimitOffset: OrderByLimitOffset;
    config: Config;
    lapisUrl: string;
};

export const Result = (props: Props) => {
    const tabs = [
        { name: 'Query URL', content: <QueryUrlTab {...props} /> },
        { name: 'R code', content: <RTab {...props} /> },
        { name: 'Python code', content: <PythonTab {...props} /> },
    ];

    return (
        <TabsBox>
            {tabs.map((tab) => (
                <Tab label={tab.name}>{tab.content}</Tab>
            ))}
        </TabsBox>
    );
};

function constructGetQueryUrl(props: Props, tabularOutputFormat: TabularOutputFormat) {
    const { lapisUrl, endpoint, body } = constructPostQuery(props);
    const params = new URLSearchParams();
    for (let [name, value] of Object.entries(body)) {
        if (Array.isArray(value)) {
            for (let valueElement of value) {
                params.append(name, valueElement);
            }
        } else {
            params.set(name, value);
        }
    }
    const queryType = props.queryType;
    if (
        queryType.selection !== 'nucleotideSequences' &&
        queryType.selection !== 'aminoAcidSequences' &&
        tabularOutputFormat !== 'json'
    ) {
        params.set('dataFormat', tabularOutputFormat);
    }
    return `${lapisUrl}${endpoint}?${params}`;
}

const QueryUrlTab = (props: Props) => {
    const [tabularOutputFormat, setTabularOutputFormat] = useState<TabularOutputFormat>('json');

    const queryUrl = constructGetQueryUrl(props, tabularOutputFormat);

    return (
        <ContainerWrapper>
            <OutputFormatSelection
                queryType={props.queryType}
                format={tabularOutputFormat}
                onFormatChange={setTabularOutputFormat}
            />
            <div>
                <LabelWrapper>Query URL:</LabelWrapper>
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
        </ContainerWrapper>
    );
};

const RTab = (props: Props) => {
    return <CodeBlock>TODO R code</CodeBlock>;
};

const PythonTab = (props: Props) => {
    if (props.queryType.selection === 'nucleotideSequences' || props.queryType.selection === 'aminoAcidSequences') {
        return <CodeBlock>TODO Code for fetching sequences</CodeBlock>;
    }
    const propsWithJson: Props = {
        ...props,
    };
    const { lapisUrl, endpoint, body, resultFields } = constructPostQuery(propsWithJson);
    const code = generateNonFastaQuery(lapisUrl, endpoint, body, resultFields);
    return <CodeBlock>{code}</CodeBlock>;
};

function constructPostQuery({ queryType, filters, config, lapisUrl, orderByLimitOffset }: Props): {
    lapisUrl: string;
    endpoint: string;
    body: object;
    resultFields: ResultField[];
} {
    let endpoint = '/sample/';
    const body: any = {};
    const resultFields = getResultFields(queryType, config);

    switch (queryType.selection) {
        case 'aggregatedAll':
            endpoint += 'aggregated';
            break;
        case 'aggregatedStratified':
            endpoint += 'aggregated';
            const aggregatedFields = queryType.aggregatedStratified.fields;
            if (aggregatedFields.size > 0) {
                body.fields = [...aggregatedFields];
            }
            break;
        case 'mutations':
            endpoint += queryType.mutations.type === 'nucleotide' ? 'nuc-mutations' : 'aa-mutations';
            body.minProportion = queryType.mutations.minProportion;
            break;
        case 'insertions':
            endpoint += queryType.insertions.type === 'nucleotide' ? 'nuc-insertions' : 'aa-insertions';
            break;
        case 'details':
            endpoint += 'details';
            const detailsFields = queryType.details.fields;
            if (detailsFields.size > 0) {
                body.fields = [...detailsFields];
            }
            break;
        case 'nucleotideSequences':
            // TODO(#521): multi segment case
            endpoint += queryType.nucleotideSequences.type === 'unaligned' ? 'nuc-sequences' : 'nuc-sequences-aligned';
            break;
        case 'aminoAcidSequences':
            endpoint += `aa-sequences-aligned/${queryType.aminoAcidSequences.gene}`;
            break;
    }
    for (let [name, value] of filters) {
        if (value.length > 0) {
            body[name] = value;
        }
    }
    if (orderByLimitOffset.orderBy.length > 0) {
        body.orderBy = orderByLimitOffset.orderBy;
    }
    if (orderByLimitOffset.limit !== undefined) {
        body.limit = orderByLimitOffset.limit;
    }
    if (orderByLimitOffset.offset !== undefined) {
        body.offset = orderByLimitOffset.offset;
    }
    return { lapisUrl, endpoint, body, resultFields };
}
