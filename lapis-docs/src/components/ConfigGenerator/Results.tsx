import { type FC, type ReactNode, useContext, useMemo } from 'react';
import { ConfigContext } from './configContext';
import { dump } from 'js-yaml';
import { pathoplexusConfigSchema } from './pathoplexusConfig.ts';
import { siloConfigSchema } from './siloConfig.ts';

export const Results: FC = () => {
    const { config, configType } = useContext(ConfigContext);

    let zodParseResult;
    let configToExport: object;
    if (configType === 'Pathoplexus') {
        configToExport = {
            instanceName: config.instanceName,
            opennessLevel: config.opennessLevel,
            metadata: config.metadata,
            website: {
                tableColumns: config.tableColumns,
            },
            silo: {
                dateToSortBy: config.dateToSortBy,
                partitionBy: config.partitionBy,
            },
        };
        zodParseResult = pathoplexusConfigSchema.safeParse(configToExport);
    } else {
        configToExport = config;
        zodParseResult = siloConfigSchema.safeParse(configToExport);
    }

    if (zodParseResult.success) {
        configToExport = zodParseResult.data;
    }

    const yaml = useMemo(() => dump({ schema: configToExport }), [configToExport]);

    function handleDownload() {
        const blob = new Blob([yaml], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);

        const a = document.createElement('a');
        a.href = url;
        a.download = `config.yaml`;
        a.click();

        URL.revokeObjectURL(url);
    }

    const tooltipMessage = 'Please check that you have provided a primary key and an instance name.';

    return (
        <div className='flex flex-col p-5 m-2 space-y-5 bg-base-100 w-1/3'>
            <h2 className='text-lg font-bold'>Results</h2>
            <div className='mockup-code overflow-x-auto max-w-full'>
                <pre>
                    <code>{yaml}</code>
                </pre>
            </div>

            <ConditionalTooltip tooltip={tooltipMessage} isActive={!zodParseResult.success}>
                <button
                    className={`btn btn-primary ${!zodParseResult.success ? 'btn-disabled' : ''}`}
                    onClick={handleDownload}
                >
                    Download
                </button>
            </ConditionalTooltip>
        </div>
    );
};

const ConditionalTooltip: FC<{ isActive: boolean; tooltip: string; children: ReactNode }> = ({
    isActive,
    tooltip,
    children,
}) => {
    if (!isActive) {
        return <>{children}</>;
    }

    return (
        <div className='tooltip' data-tip={tooltip}>
            {children}
        </div>
    );
};
