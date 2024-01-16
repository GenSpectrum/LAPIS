import { z } from 'zod';
import { featureSchema, metadataTypeSchema, opennessLevelSchema } from './configContext.tsx';

export const siloConfigSchema = z.object({
    instanceName: z.string(),
    opennessLevel: opennessLevelSchema,
    metadata: z.array(
        z.object({
            name: z.string(),
            type: metadataTypeSchema,
            generateIndex: z.boolean().optional(),
            autocomplete: z.boolean().optional(),
            required: z.boolean().optional(),
            notSearchable: z.boolean().optional(),
        }),
    ),
    primaryKey: z.string(),
    dateToSortBy: z.string().optional(),
    partitionBy: z.string().optional(),
    features: z.array(featureSchema).optional(),
});
