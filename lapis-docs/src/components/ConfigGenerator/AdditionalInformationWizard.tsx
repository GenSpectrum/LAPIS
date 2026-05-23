import { type ChangeEvent, type FC, useContext, useEffect, useMemo } from 'react';
import { ConfigContext, type MetadataType, type TopLevelConfig } from './configContext.tsx';
import { HelpTooltip } from './HelpTooltip.tsx';
import { Field, SectionDivider, SectionHeading } from './FormLayout.tsx';

export function AdditionalInformationWizard() {
    return (
        <div className='space-y-4'>
            <SectionHeading>Additional settings</SectionHeading>
            <MetadataDropDown
                filterByType={'string'}
                name={'primaryKey'}
                label='Primary key'
                required
                help={
                    <HelpTooltip
                        text='The metadata field that uniquely identifies each sequence (e.g. an accession). Must be a string field.'
                        docsHref='/maintainer-docs/references/database-configuration#the-schema-object'
                    />
                }
            />

            <SectionDivider />

            <SectionHeading>Advanced (optional)</SectionHeading>
            <StringTopLevelField
                name='defaultNucleotideSequence'
                label='Default nucleotide sequence'
                helpText='Name of the nucleotide segment to be searched by default when no segment name is supplied in a query. Should match a segment from reference_genomes.json. Set this if there is more than one segment.'
            />
            <StringTopLevelField
                name='defaultAminoAcidSequence'
                label='Default amino acid sequence'
                helpText='Name of the gene to be searched by default when no gene name is supplied in a query. Should match a gene from reference_genomes.json.'
            />
            <NumberTopLevelField
                name='siloClientThreadCount'
                label='SILO client thread count'
                helpText='How many threads (connections) LAPIS uses to talk to SILO. Defaults to 64.'
            />
        </div>
    );
}

type MetadataDropDownProps = {
    name: 'primaryKey';
    filterByType: MetadataType;
    label: string;
    required?: boolean;
    help?: React.ReactNode;
};

const MetadataDropDown: FC<MetadataDropDownProps> = ({ name, filterByType, label, required, help }) => {
    const { config, modifyConfigField, removeConfigField } = useContext(ConfigContext);

    useEffect(() => {
        const currentValue = config[name];
        const matchesExisting = currentValue !== undefined && config.metadata.some((m) => m.name === currentValue);

        if (!matchesExisting) {
            const fallback = config.metadata.find((m) => m.type === filterByType);
            if (fallback !== undefined) {
                modifyConfigField(name, fallback.name);
            } else {
                removeConfigField(name);
            }
        }
    }, []);

    const options = useMemo(
        () => config.metadata.filter((metadata) => metadata.type === filterByType),
        [config.metadata, filterByType],
    );

    const handleChange = (event: ChangeEvent<HTMLSelectElement>) => {
        modifyConfigField(name, event.target.value);
    };

    return (
        <Field label={label} help={help} required={required}>
            <select
                className='select select-bordered select-sm w-full max-w-md font-mono'
                onChange={handleChange}
                value={config[name] ?? ''}
                disabled={options.length === 0}
                required={required}
            >
                {options.length === 0 && <option value=''>(no string fields defined)</option>}
                {options.map((option) => (
                    <option key={option.name} value={option.name}>
                        {option.name}
                    </option>
                ))}
            </select>
        </Field>
    );
};

type StringTopLevelFieldProps = {
    name: 'defaultNucleotideSequence' | 'defaultAminoAcidSequence';
    label: string;
    helpText: string;
};

const StringTopLevelField: FC<StringTopLevelFieldProps> = ({ name, label, helpText }) => {
    const { topLevelConfig, modifyTopLevelField } = useContext(ConfigContext);
    const value = topLevelConfig[name] ?? '';

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        const next = event.target.value.trim();
        modifyTopLevelField(name, next === '' ? undefined : next);
    };

    return (
        <Field label={label} help={<HelpTooltip text={helpText} />}>
            <input
                type='text'
                className='input input-bordered input-sm w-full max-w-md font-mono'
                value={value}
                onChange={handleChange}
                placeholder='leave empty to omit'
            />
        </Field>
    );
};

type NumberTopLevelFieldProps = {
    name: keyof TopLevelConfig;
    label: string;
    helpText: string;
};

const NumberTopLevelField: FC<NumberTopLevelFieldProps> = ({ name, label, helpText }) => {
    const { topLevelConfig, modifyTopLevelField } = useContext(ConfigContext);
    const value = topLevelConfig[name];

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        const raw = event.target.value.trim();
        if (raw === '') {
            modifyTopLevelField(name, undefined);
            return;
        }
        const parsed = Number.parseInt(raw, 10);
        if (Number.isNaN(parsed) || parsed <= 0) {
            modifyTopLevelField(name, undefined);
            return;
        }
        modifyTopLevelField(name, parsed);
    };

    return (
        <Field label={label} help={<HelpTooltip text={helpText} />}>
            <input
                type='number'
                min={1}
                step={1}
                className='input input-bordered input-sm w-full max-w-md font-mono'
                value={value ?? ''}
                onChange={handleChange}
                placeholder='leave empty to use the default'
            />
        </Field>
    );
};
