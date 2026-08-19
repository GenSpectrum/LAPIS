import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';

// Wrapper so that hydration uses this optimized dep instead of a
// raw node_modules URL, which Vite 8's stricter CJS interop would break.
export default function SwaggerUIWrapper({ url }: { url: string }) {
    return <SwaggerUI url={url} />;
}
