import fs from 'fs';
import { parse } from 'yaml';

export type MetadataType =
    | 'string'
    | 'date'
    | 'pango_lineage'
    | 'float'
    | 'int'
    | 'insertion'
    | 'aaInsertion'
    | 'boolean';

export type Metadata = {
    name: string;
    type: MetadataType;
    generateLineageIndex?: boolean;
};

export type Feature = {
    name: string;
};

export type Config = {
    schema: {
        instanceName: string;
        metadata: Metadata[];
        features: Feature[];
        primaryKey: string;
    };
};

let _config: Config | null = null;
let _featuresNames: Set<string> | null = null;

export function getConfig(): Config {
    if (_config === null) {
        const configFilePath = process.env.CONFIG_FILE;
        if (configFilePath === undefined) {
            throw new Error('Please set the environment variable CONFIG_FILE.');
        }
        _config = parse(fs.readFileSync(configFilePath, 'utf8')) as Config;
        _featuresNames = new Set(_config.schema.features.map((f) => f.name));
    }
    return _config;
}

export function hasFeature(feature: string): boolean {
    if (!_featuresNames) {
        getConfig();
    }
    return _featuresNames!.has(feature);
}

export function hasPangoLineage(config: Config): boolean {
    return config.schema.metadata.some(
        (m) =>
            m.generateLineageIndex === true &&
            (m.name.toLowerCase().includes('pangolineage') || m.name.toLowerCase().includes('pango_lineage')),
    );
}
