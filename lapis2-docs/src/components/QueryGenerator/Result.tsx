import type { QueryTypeSelectionState } from './QueryTypeSelection';
import type { Filters } from './FiltersSelection';
import type { TabularOutputFormat } from './OutputFormatSelection';
import { CodeBlock } from '../CodeBlock';
import { Tab, TabsBox } from '../TabsBox/react/TabsBox';
import { generateNonFastaQuery } from '../../utils/code-generators/python/generator';
import type { Config, MetadataType } from '../../config';
import { OutputFormatSelection } from './OutputFormatSelection';
import { useState } from 'react';
import type { ResultField, ResultFieldType } from '../../utils/code-generators/types';
import { ContainerWrapper, LabelWrapper } from './styled-components';

type Props = {
    queryType: QueryTypeSelectionState;
    filters: Filters;
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
    const { endpoint, body } = constructPostQuery(props);
    const params = new URLSearchParams();
    for (let [name, value] of Object.entries(body)) {
        if (Array.isArray(value)) {
            params.set(name, value.join(','));
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
    return `${endpoint}?${params}`;
}

const QueryUrlTab = (props: Props) => {
    const [tabularOutputFormat, setTabularOutputFormat] = useState<TabularOutputFormat>('json');

    // TODO Prepend the URL to the instance
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

function constructPostQuery({ queryType, filters, config, lapisUrl }: Props): {
    lapisUrl: string;
    endpoint: string;
    body: object;
    resultFields: ResultField[];
} {
    let endpoint = '/';
    const body: any = {};
    const resultFields: ResultField[] = [];

    switch (queryType.selection) {
        case 'aggregatedAll':
            endpoint += 'aggregated';
            resultFields.push({ name: 'count', type: 'integer', nullable: false });
            break;
        case 'aggregatedStratified':
            endpoint += 'aggregated';
            resultFields.push({ name: 'count', type: 'integer', nullable: false });
            const aggregatedFields = queryType.aggregatedStratified.fields;
            if (aggregatedFields.size > 0) {
                body.fields = [...aggregatedFields];
                resultFields.push(...fieldNamesToResultFields(aggregatedFields, config));
            }
            break;
        case 'mutations':
            endpoint += queryType.mutations.type === 'nucleotide' ? 'nuc-mutations' : 'aa-mutations';
            body.minProportion = queryType.mutations.minProportion;
            resultFields.push(
                { name: 'mutation', type: 'string', nullable: false },
                { name: 'proportion', type: 'float', nullable: false },
                { name: 'count', type: 'integer', nullable: false },
            );
            break;
        case 'insertions':
            endpoint += queryType.insertions.type === 'nucleotide' ? 'nuc-insertions' : 'aa-insertions';
            resultFields.push(
                { name: 'insertion', type: 'string', nullable: false },
                { name: 'count', type: 'integer', nullable: false },
            );
            break;
        case 'details':
            endpoint += 'details';
            const detailsFields = queryType.details.fields;
            if (detailsFields.size > 0) {
                body.fields = [...detailsFields];
                resultFields.push(...fieldNamesToResultFields(detailsFields, config));
            }
            break;
        case 'nucleotideSequences':
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
    return { lapisUrl, endpoint, body, resultFields };
}

function mapMetadataTypeToResultFieldType(type: MetadataType): ResultFieldType {
    switch (type) {
        case 'pango_lineage':
        case 'date':
        case 'string':
            return 'string';
    }
}

function fieldNamesToResultFields(fields: Set<string>, config: Config): ResultField[] {
    const metadataMap = new Map(config.schema.metadata.map((m) => [m.name, m.type]));
    return [...fields].map((field) => ({
        name: field,
        type: mapMetadataTypeToResultFieldType(metadataMap.get(field)!),
        nullable: true,
    }));
}
