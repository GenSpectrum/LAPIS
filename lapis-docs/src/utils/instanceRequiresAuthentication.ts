import { getLapisUrl } from '../lapisUrl.ts';

export async function instanceRequiresAuthentication() {
    try {
        const response = await fetch(`${getLapisUrl()}/sample/info`);
        return response.status === 401;
    } catch (error) {
        console.error('Error checking authentication requirement:', error);
        return false;
    }
}
