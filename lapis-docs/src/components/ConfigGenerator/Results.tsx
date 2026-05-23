import { type FC, useContext, useMemo, useState } from 'react';
import { ConfigContext } from './configContext';
import { dump } from 'js-yaml';
import { pathoplexusConfigSchema } from './pathoplexusConfig.ts';
import { siloConfigSchema } from './siloConfig.ts';
import { withBaseUrl } from './urls.ts';

export const Results: FC = () => {
    const { config, topLevelConfig, configType } = useContext(ConfigContext);

    const fullConfig =
        configType === 'Pathoplexus'
            ? {
                  instanceName: config.instanceName,
                  opennessLevel: config.opennessLevel,
                  metadata: config.metadata,
                  website: {
                      tableColumns: config.tableColumns,
                  },
              }
            : {
                  schema: {
                      instanceName: config.instanceName,
                      opennessLevel: config.opennessLevel,
                      metadata: config.metadata,
                      primaryKey: config.primaryKey,
                      features: config.features,
                  },
                  ...topLevelConfig,
              };

    const zodParseResult =
        configType === 'Pathoplexus'
            ? pathoplexusConfigSchema.safeParse(fullConfig)
            : siloConfigSchema.safeParse(fullConfig);

    const configToEmit = zodParseResult.success ? zodParseResult.data : fullConfig;

    const yaml = useMemo(() => dump(configToEmit), [configToEmit]);
    const isValid = zodParseResult.success;

    function handleDownload() {
        const blob = new Blob([yaml], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);

        const a = document.createElement('a');
        a.href = url;
        a.download = `database_config.yaml`;
        a.click();

        URL.revokeObjectURL(url);
    }

    return (
        <aside className='flex flex-col gap-4 p-4 border border-base-300 rounded-sm bg-base-100 text-sm'>
            <div className='flex items-center justify-between'>
                <h2 className='text-base font-semibold'>Result</h2>
                <StatusBadge isValid={isValid} />
            </div>

            <YamlPreview yaml={yaml} />

            <div className='flex items-center gap-2'>
                <button
                    type='button'
                    className={`btn btn-sm ${isValid ? 'btn-primary' : 'btn-disabled'}`}
                    onClick={handleDownload}
                >
                    Download
                </button>
                <CopyButton yaml={yaml} />
            </div>

            {!isValid && <ValidationPanel issues={zodParseResult.error.issues} />}

            <ArtifactsPanel />
        </aside>
    );
};

const StatusBadge: FC<{ isValid: boolean }> = ({ isValid }) => (
    <span
        className={`text-xs font-mono px-2 py-0.5 border rounded-sm ${
            isValid ? 'border-success/40 text-success' : 'border-warning/40 text-warning'
        }`}
    >
        {isValid ? '✓ valid' : '✗ incomplete'}
    </span>
);

const YamlPreview: FC<{ yaml: string }> = ({ yaml }) => (
    <pre className='border border-base-300 bg-base-200/50 rounded-sm p-3 text-xs font-mono whitespace-pre overflow-x-auto max-h-96'>
        {yaml}
    </pre>
);

const CopyButton: FC<{ yaml: string }> = ({ yaml }) => {
    const [copied, setCopied] = useState(false);
    const handleCopy = async () => {
        try {
            await navigator.clipboard.writeText(yaml);
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        } catch {
            // no-op; older browsers
        }
    };
    return (
        <button type='button' className='btn btn-sm btn-outline' onClick={handleCopy}>
            {copied ? 'Copied' : 'Copy YAML'}
        </button>
    );
};

const ValidationPanel: FC<{
    issues: readonly { path: readonly PropertyKey[]; message: string }[];
}> = ({ issues }) => {
    if (issues.length === 0) return null;
    return (
        <div className='border border-warning/40 border-l-4 rounded-sm p-3 text-xs'>
            <div className='font-semibold mb-1'>Not ready for download</div>
            <ul className='space-y-1'>
                {issues.map((issue, i) => {
                    const path = issue.path.length === 0 ? '(root)' : issue.path.map(String).join('.');
                    return (
                        <li key={i}>
                            <span className='font-mono'>{path}</span>: {issue.message}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

const ArtifactsPanel: FC = () => {
    const { config } = useContext(ConfigContext);

    const lineageSystems = Array.from(
        new Set(
            config.metadata
                .map((m) => m.generateLineageIndex)
                .filter((value): value is string => typeof value === 'string' && value.length > 0),
        ),
    );

    const hasPhyloTreeField = config.metadata.some((m) => m.isPhyloTreeField === true);

    return (
        <div className='border border-base-300 border-l-4 border-l-base-content/30 rounded-sm p-3 text-xs'>
            <div className='font-semibold mb-2'>What else you'll need</div>
            <ul className='space-y-1.5'>
                <li>
                    A <code>reference_genomes.json</code> file (see{' '}
                    <a
                        href={withBaseUrl('/maintainer-docs/references/reference-genomes')}
                        className='underline hover:no-underline'
                    >
                        Reference Genomes
                    </a>
                    ).
                </li>
                <li>
                    A SILO preprocessing config and input data in NDJSON format (see{' '}
                    <a
                        href={withBaseUrl('/maintainer-docs/references/preprocessing')}
                        className='underline hover:no-underline'
                    >
                        Preprocessing
                    </a>
                    ).
                </li>
                {lineageSystems.length > 0 && (
                    <li>
                        Lineage-definition YAML file(s) for{' '}
                        {lineageSystems.map((name, idx) => (
                            <span key={name}>
                                <code>{name}</code>
                                {idx < lineageSystems.length - 1 ? ', ' : ''}
                            </span>
                        ))}
                        , referenced via <code>lineageDefinitionFilenames</code>.
                    </li>
                )}
                {hasPhyloTreeField && (
                    <li>
                        A phylogenetic-tree file (Newick or Auspice JSON v2) referenced via{' '}
                        <code>phyloTreeFilename</code>.
                    </li>
                )}
            </ul>
        </div>
    );
};
