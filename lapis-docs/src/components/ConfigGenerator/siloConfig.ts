import { z } from 'astro/zod';
import { featureSchema, metadataTypeSchema, opennessLevelSchema, topLevelConfigSchema } from './configContext.tsx';

export const siloSchemaSchema = z.object({
    instanceName: z.string(),
    opennessLevel: opennessLevelSchema,
    metadata: z.array(
        z.object({
            name: z.string(),
            type: metadataTypeSchema,
            generateIndex: z.boolean().optional(),
            generateLineageIndex: z.string().optional(),
            isPhyloTreeField: z.boolean().optional(),
            autocomplete: z.boolean().optional(),
            required: z.boolean().optional(),
            notSearchable: z.boolean().optional(),
        }),
    ),
    primaryKey: z.string(),
    features: z.array(featureSchema).optional(),
});

export const siloConfigSchema = z
    .object({
        schema: siloSchemaSchema,
    })
    .merge(topLevelConfigSchema);
