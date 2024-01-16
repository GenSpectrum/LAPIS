import { z } from 'zod';
import { metadataTypeSchema, opennessLevelSchema } from './configContext.tsx';

export const pathoplexusConfigSchema = z.object({
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
    website: z.object({
        tableColumns: z.array(z.string()),
    }),
    silo: z.object({
        dateToSortBy: z.string().optional(),
        partitionBy: z.string().optional(),
    }),
});
