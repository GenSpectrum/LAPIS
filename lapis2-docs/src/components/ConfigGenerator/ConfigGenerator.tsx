import { useState } from 'react';
import { ConfigProvider, type PartialConfig } from './configContext.tsx';
import { ConfigWizard } from './ConfigWizard.tsx';
import { Results } from './Results.tsx';
import { UploadConfig } from './UploadConfig.tsx';

export const ConfigGenerator = () => {
    const [config, setConfig] = useState<PartialConfig | undefined>(undefined);

    if (config === undefined) {
        return (
            <div className='not-content text-center '>
                <button onClick={() => setConfig({ metadata: [] })} className='btn btn-primary'>
                    New
                </button>
                <UploadConfig setConfig={setConfig} />
            </div>
        );
    }

    return (
        <div className='not-content flex'>
            <ConfigProvider initialConfig={{ ...config, opennessLevel: 'OPEN' }}>
                <div className='w-11/12 flex'>
                    <ConfigWizard />
                    <Results />
                </div>
            </ConfigProvider>
        </div>
    );
};
