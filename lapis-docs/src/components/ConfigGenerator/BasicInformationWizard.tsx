import { ConfigContext, LAPIS_OPENNESS_OPEN, type PartialConfig } from './configContext.tsx';
import { useContext, useEffect } from 'react';
import { HelpTooltip } from './HelpTooltip.tsx';
import { Field, SectionDivider, SectionHeading } from './FormLayout.tsx';

const features = [
    {
        featureName: 'sarsCoV2VariantQuery',
        label: 'SARS-CoV-2 variant query',
        description:
            'SARS-CoV-2-specific query language exposed via the variantQuery parameter. Only enable this if your instance serves SARS-CoV-2 data.',
    },
    {
        featureName: 'generalizedAdvancedQuery',
        label: 'Generalized advanced query',
        description:
            'Generic advanced query language exposed via the advancedQuery parameter. Recommended for non-SARS-CoV-2 instances.',
    },
];

export function BasicInformationWizard() {
    const { config, modifyConfigField } = useContext(ConfigContext);

    useEffect(() => {
        if (config.opennessLevel === undefined) {
            modifyConfigField('opennessLevel', LAPIS_OPENNESS_OPEN);
        }
    }, [config.opennessLevel, modifyConfigField]);

    return (
        <div className='space-y-4'>
            <SectionHeading>Basic information</SectionHeading>
            <InstanceName
                instanceName={config.instanceName}
                updateInstanceName={(instanceName: string) => modifyConfigField('instanceName', instanceName)}
            />
            <SectionDivider />
            <FeaturesModifier />
        </div>
    );
}

function InstanceName({
    instanceName,
    updateInstanceName,
}: {
    instanceName: string | undefined;
    updateInstanceName: (instanceName: string) => void;
}) {
    return (
        <Field
            label='Instance name'
            required
            help={
                <HelpTooltip
                    text='Display name for this LAPIS instance. Shown on the landing page; not used for identification.'
                    docsHref='/maintainer-docs/references/database-configuration#the-schema-object'
                />
            }
        >
            <input
                type='text'
                required
                placeholder='e.g. sars-cov-2-public'
                className='input input-bordered w-full max-w-md font-mono text-sm'
                onChange={(event) => updateInstanceName(event.target.value)}
                value={instanceName ?? ''}
            />
        </Field>
    );
}

function FeaturesModifier() {
    const { config, modifyFeatureFields } = useContext(ConfigContext);

    const toggleFeature = (featureName: any, isEnabled: boolean) => {
        modifyFeatureFields(featureName, isEnabled ? 'add' : 'delete');
    };

    const hasFeature = (config: PartialConfig, featureName: string) => {
        return config.features?.some((feature) => feature.name === featureName) ?? false;
    };

    return (
        <div className='space-y-3'>
            <div className='text-sm font-medium flex items-center'>
                LAPIS features
                <HelpTooltip
                    text='Optional query languages that the maintainer can enable.'
                    docsHref='/maintainer-docs/references/database-configuration#features'
                />
            </div>
            <div className='space-y-2'>
                {features.map(({ featureName, label, description }) => (
                    <label key={featureName} className='flex items-start gap-3 py-1 cursor-pointer'>
                        <input
                            type='checkbox'
                            className='toggle toggle-sm mt-1'
                            onChange={(event) => toggleFeature(featureName, event.target.checked)}
                            checked={hasFeature(config, featureName)}
                        />
                        <div className='flex flex-col'>
                            <span className='text-sm font-mono'>{featureName}</span>
                            <span className='text-xs text-base-content/70'>{description}</span>
                        </div>
                    </label>
                ))}
            </div>
        </div>
    );
}
