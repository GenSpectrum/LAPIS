import { ConfigContext, LAPIS_OPENNESS_OPEN, type PartialConfig } from './configContext.tsx';
import { useContext, useEffect } from 'react';

const features = [
    {
        featureName: 'sarsCoV2VariantQuery',
        label: 'SARS CoV2 Variant Query',
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
        <div className='flex flex-col mb-4'>
            <h1 className='text-xl font-bold mb-4'>Basic Information</h1>
            <InstanceName
                instanceName={config.instanceName}
                updateInstanceName={(instanceName: string) => modifyConfigField('instanceName', instanceName)}
            />
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
        <div className='form-control w-full max-w-xs mb-4'>
            <label className='label'>
                <span className='label-text'>Instance Name</span>
            </label>
            <input
                type='text'
                placeholder='Instance Name'
                className='input input-bordered w-full max-w-xs'
                onChange={(event) => updateInstanceName(event.target.value)}
                value={instanceName ?? ''}
            />
        </div>
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
        <div className='form-control'>
            <span className='label-text mb-2'>LAPIS Features</span>
            {features.map(({ featureName, label }) => (
                <label key={featureName} className='flex items-center space-x-2 py-2'>
                    <span className='label-text'>{label}</span>
                    <input
                        type='checkbox'
                        className='toggle toggle-accent'
                        onChange={(event) => toggleFeature(featureName, event.target.checked)}
                        checked={hasFeature(config, featureName)}
                    />
                </label>
            ))}
        </div>
    );
}
