let lapisUrl: string | null = null;

export function getLapisUrl(): string {
    if (lapisUrl === null) {
        if (import.meta.env.LAPIS_URL === undefined) {
            throw new Error('LAPIS_URL environment variable is not set');
        }
        lapisUrl = (import.meta.env.LAPIS_URL as string).replace(/\/$/, '');
    }
    return lapisUrl;
}
