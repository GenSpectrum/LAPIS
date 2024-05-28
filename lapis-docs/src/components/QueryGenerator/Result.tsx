import type { Filters } from './FiltersSelection';
import type { TabularOutputFormat } from './OutputFormatSelection';
import { OutputFormatSelection } from './OutputFormatSelection';
import { CodeBlock } from '../CodeBlock';
import { Tab, TabsBox } from '../TabsBox/react/TabsBox';
import { generateNonFastaQuery } from '../../utils/code-generators/python/generator';
import type { Config } from '../../config';
import React, { useState } from 'react';
import type { ResultField } from '../../utils/code-generators/types';
import { CheckBoxesWrapper, ContainerWrapper, LabeledCheckBox, LabelWrapper } from './styled-components';
import { getResultFields, MULTI_SEGMENTED, type QueryTypeSelectionState } from './QueryTypeSelectionState.ts';
import type { OrderByLimitOffset } from './OrderLimitOffsetSelection.tsx';

const compressionOptions = [
    { value: undefined, label: 'No compression' },
    { value: 'gzip', label: 'gzip' },
    { value: 'zstd', label: 'zstd' },
];

type CompressionValues = (typeof compressionOptions)[number]['value'];

type AdditionalProperties = {
    downloadAsFile: boolean;
    tabularOutputFormat: TabularOutputFormat;
    compression: CompressionValues;
};

type Props = {
    queryType: QueryTypeSelectionState;
    filters: Filters;
    orderByLimitOffset: OrderByLimitOffset;
    config: Config;
    lapisUrl: string;
};

type TabProps = Props & AdditionalProperties;

export const Result = (props: Props) => {
    const [additionalProperties, setAdditionalProperties] = useState<AdditionalProperties>({
        downloadAsFile: false,
        tabularOutputFormat: 'json',
        compression: undefined,
    });

    const tabProps = { ...props, ...additionalProperties };

    const tabs = [
        { name: 'Query URL', content: <QueryUrlTab {...tabProps} /> },
        { name: 'R code', content: <RTab {...tabProps} /> },
        { name: 'Python code', content: <PythonTab {...tabProps} /> },
    ];

    return (
        <>
            <LabeledCheckBox
                label='Download as file'
                type='checkbox'
                checked={additionalProperties.downloadAsFile}
                onChange={() => setAdditionalProperties((prev) => ({ ...prev, downloadAsFile: !prev.downloadAsFile }))}
            />
            <OutputFormatSelection
                queryType={props.queryType}
                format={additionalProperties.tabularOutputFormat}
                onFormatChange={(value) => setAdditionalProperties((prev) => ({ ...prev, tabularOutputFormat: value }))}
            />
            <div className='mt-4'>
                <LabelWrapper>Do you want to fetch compressed data?</LabelWrapper>
                <CheckBoxesWrapper>
                    {compressionOptions.map(({ value, label }) => (
                        <LabeledCheckBox
                            label={label}
                            type='checkbox'
                            className='w-40'
                            checked={value === additionalProperties.compression}
                            onChange={() => setAdditionalProperties((prev) => ({ ...prev, compression: value }))}
                        />
                    ))}
                </CheckBoxesWrapper>
            </div>
            <TabsBox>
                {tabs.map((tab) => (
                    <Tab label={tab.name}>{tab.content}</Tab>
                ))}
            </TabsBox>
        </>
    );
};

function constructGetQueryUrl(props: TabProps) {
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
        props.tabularOutputFormat !== 'json'
    ) {
        params.set('dataFormat', props.tabularOutputFormat);
    }
    if (props.downloadAsFile) {
        params.set('downloadAsFile', 'true');
    }
    if (props.compression !== undefined) {
        params.set('compression', props.compression);
    }
    return `${lapisUrl}${endpoint}?${params}`;
}

const QueryUrlTab = (props: TabProps) => {
    const queryUrl = constructGetQueryUrl(props);

    return (
        <ContainerWrapper>
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

const RTab = (props: TabProps) => {
    return <CodeBlock>TODO R code</CodeBlock>;
};

const PythonTab = (props: TabProps) => {
    if (props.queryType.selection === 'nucleotideSequences' || props.queryType.selection === 'aminoAcidSequences') {
        return <CodeBlock>TODO Code for fetching sequences</CodeBlock>;
    }
    const propsWithJson: TabProps = {
        ...props,
    };
    const { lapisUrl, endpoint, body, resultFields } = constructPostQuery(propsWithJson);
    const code = generateNonFastaQuery(lapisUrl, endpoint, body, resultFields);
    return <CodeBlock>{code}</CodeBlock>;
};

function constructPostQuery({
    queryType,
    filters,
    config,
    lapisUrl,
    orderByLimitOffset,
    downloadAsFile,
    tabularOutputFormat,
    compression,
}: TabProps): {
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
            endpoint += queryType.mutations.type === 'nucleotide' ? 'nucleotideMutations' : 'aminoAcidMutations';
            body.minProportion = queryType.mutations.minProportion;
            break;
        case 'insertions':
            endpoint += queryType.insertions.type === 'nucleotide' ? 'nucleotideInsertions' : 'aminoAcidInsertions';
            break;
        case 'details':
            endpoint += 'details';
            const detailsFields = queryType.details.fields;
            if (detailsFields.size > 0) {
                body.fields = [...detailsFields];
            }
            break;
        case 'nucleotideSequences':
            endpoint +=
                queryType.nucleotideSequences.type === 'unaligned'
                    ? 'nucleotideSequences'
                    : 'alignedNucleotideSequences';
            if (queryType.nucleotideSequences.segment.type === MULTI_SEGMENTED) {
                endpoint += `/${queryType.nucleotideSequences.segment.segmentName}`;
            }
            break;
        case 'aminoAcidSequences':
            endpoint += `alignedAminoAcidSequences/${queryType.aminoAcidSequences.gene}`;
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
    if (downloadAsFile) {
        body.downloadAsFile = true;
    }
    if (tabularOutputFormat !== 'json') {
        body.dataFormat = tabularOutputFormat;
    }
    if (compression !== undefined) {
        body.compression = compression;
    }
    return { lapisUrl, endpoint, body, resultFields };
}
