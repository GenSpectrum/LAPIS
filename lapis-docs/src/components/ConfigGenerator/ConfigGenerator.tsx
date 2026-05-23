import { useState } from 'react';
import { ConfigProvider, type PartialConfig, type TopLevelConfig } from './configContext.tsx';
import { ConfigWizard } from './ConfigWizard.tsx';
import { Results } from './Results.tsx';
import { UploadConfig } from './UploadConfig.tsx';

export const ConfigGenerator = () => {
    const [config, setConfig] = useState<PartialConfig | undefined>(undefined);
    const [topLevelConfig, setTopLevelConfig] = useState<TopLevelConfig>({});

    if (config === undefined) {
        return (
            <div className='not-content'>
                <p className='text-sm text-base-content/70 mb-4'>
                    Generates a <code>database_config.yaml</code> for LAPIS + SILO.
                </p>
                <div className='grid grid-cols-1 sm:grid-cols-2 gap-4'>
                    <button onClick={() => setConfig({ metadata: [] })} className='btn btn-primary h-24 text-base'>
                        Start from scratch
                    </button>
                    <UploadConfig setConfig={setConfig} setTopLevelConfig={setTopLevelConfig} />
                </div>
            </div>
        );
    }

    return (
        <div className='not-content'>
            <ConfigProvider initialConfig={{ ...config, opennessLevel: 'OPEN' }} initialTopLevelConfig={topLevelConfig}>
                <div className='flex flex-col lg:flex-row gap-4'>
                    <div className='flex-1 min-w-0'>
                        <ConfigWizard />
                    </div>
                    <div className='lg:w-96 lg:flex-shrink-0'>
                        <Results />
                    </div>
                </div>
            </ConfigProvider>
        </div>
    );
};
