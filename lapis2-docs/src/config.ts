import fs from 'fs';
import { parse } from 'yaml';

export type MetadataType = 'string' | 'date' | 'pango_lineage' | 'float' | 'int' | 'insertion' | 'aaInsertion';

export type Metadata = {
    name: string;
    type: MetadataType;
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
