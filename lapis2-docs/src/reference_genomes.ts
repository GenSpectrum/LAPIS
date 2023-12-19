import fs from 'fs';
import { z } from 'zod';

const referenceSequenceSchema = z.object({
    name: z.string(),
    sequence: z.string(),
});

const referenceGenomesSchema = z.object({
    nucleotideSequences: z.array(referenceSequenceSchema),
    genes: z.array(referenceSequenceSchema),
});

export type ReferenceGenomes = z.infer<typeof referenceGenomesSchema>;

let _referenceGenomes: ReferenceGenomes | null = null;

export function getReferenceGenomes(): ReferenceGenomes {
    if (_referenceGenomes === null) {
        const configFilePath = process.env.REFERENCE_GENOMES_FILE;
        if (configFilePath === undefined) {
            throw new Error('Please set the environment variable REFERENCE_GENOMES_FILE.');
        }
        _referenceGenomes = referenceGenomesSchema.parse(JSON.parse(fs.readFileSync(configFilePath, 'utf8')));
    }
    return _referenceGenomes;
}

export function isMultiSegmented(referenceGenomes: ReferenceGenomes): boolean {
    return referenceGenomes.nucleotideSequences.length > 1;
}
