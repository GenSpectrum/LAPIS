import { expect } from 'chai';
import { basePath } from './common';

describe('Error handling: UnknownUrl', () => {
  it('should return a 404 JSON response by default', async () => {
    const result = await fetch(basePath + '/unknownUrl');

    expect(result.status).equals(404);
    expect(result.headers.get('Content-Type')).equals('application/json');
    expect((await result.json())?.error).equals('Not Found');
  });

  it('should return a 404 HTML response when a browser asks for HTML', async () => {
    const result = await fetch(basePath + '/unknownUrl', { headers: { Accept: 'text/html' } });

    expect(result.status).equals(404);
    expect(result.headers.get('Content-Type')).equals('text/html');

    let responseBody = await result.text();
    expect(responseBody).contains('Page not found');
    expect(responseBody).contains('<a href="http://localhost:8080/swagger-ui/index.html">');
  });
});
