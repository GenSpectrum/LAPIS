/**
 * Prefix an absolute docs path with Astro's BASE_URL so the wizard's links
 * work both at the site root and at a sub-path deployment.
 */
export const withBaseUrl = (path: string): string => {
    const base = (import.meta.env.BASE_URL ?? '').replace(/\/$/, '');
    return base + path;
};
