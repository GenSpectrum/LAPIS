import { expect } from 'chai';
import {
  basePathWithIssuerUriAuth,
  basePathWithJwkSetUriAuth,
  basePathWithPublicKeyAuth,
  lapisClientWithAuth,
} from './common';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

// This token was created with the test private keys in testData/oauth-keys
const validTokenForPublicKey =
  'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJsYXBpcy1hcGkiLCJpc3MiOiJsYXBpcy10ZXN0In0.Xu50CdL21tWvzfBE_BuQ-BPKx6C8Dj0EDaW5skK1IrYwRwpsKrocDc_l_51gDy_sYylJV1M3JYfhSMum_4e-iVpoGbRvf2J-T-yQD5oC-IKE27b2g6vXmFrbSottD4GHeUnn2Xh5aFrPyPu5SmF-_Vhfc2W-kqVE-zjxCj3uf28aHesV77FuvnXob9pZJjGHP-xyKfxqD88u5HunEor-YT1H-3Z3_aRazZis9kDDADa-4PLoDiYpfLMEaO4PMeIHN5412qhuOcwOmA28dNKmiJT9ye9osb2VLzDe9fl_CakJpkjlXF9D-_4Of81Q2X-BCOGPOnxHAXb6JrNBr_SWaQ';

describe('LAPIS that requires authentication', () => {
  const configs = [
    { name: 'via public key', basePath: basePathWithPublicKeyAuth, fetchToken: () => validTokenForPublicKey },
    {
      name: 'via Keycloak jwk-set-uri',
      basePath: basePathWithJwkSetUriAuth,
      fetchToken: fetchTokenFromKeycloak,
    },
    {
      name: 'via Keycloak issuer-uri',
      basePath: basePathWithIssuerUriAuth,
      fetchToken: fetchTokenFromKeycloakViaContainer,
    },
  ];

  for (let { name, basePath, fetchToken } of configs) {
    describe(name, () => {
      it('should reject call without token', async () => {
        const result = await fetch(basePath + '/sample/aggregated');

        expect(result.status).to.equal(401);
        expect(result.headers.get('www-authenticate')).to.equal('Bearer');
      });

      it('should reject call with invalid token', async () => {
        const result = await fetch(basePath + '/sample/aggregated', {
          headers: { Authorization: `Bearer invalidToken` },
        });

        expect(result.status).to.equal(401);
        expect(result.headers.get('www-authenticate')).to.contain('Jwt: Malformed token');
      });

      it('should allow call with valid token', async () => {
        const accessToken = await fetchToken();

        const response = await lapisClientWithAuth({
          basePath,
          accessToken,
        }).postAggregated({
          aggregatedPostRequest: {},
        });

        expect(response.data[0].count).to.equal(100);
      });
    });
  }
});

async function fetchTokenFromKeycloak() {
  const response = await fetch('http://localhost:8180/realms/test-realm/protocol/openid-connect/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'lapis-client',
      username: 'test-user',
      password: 'test123',
    }),
  });
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  const data = await response.json();
  return data.access_token;
}

/**
 * Fetches a JWT token from Keycloak via the Docker container network.
 *
 * This is necessary for issuer-uri authentication because:
 * 1. Keycloak sets the JWT 'iss' (issuer) claim based on the URL used to request the token
 * 2. When accessed from host via localhost:8180, tokens get iss: "http://localhost:8180/realms/test-realm"
 * 3. When accessed from container via keycloak:8080, tokens get iss: "http://keycloak:8080/realms/test-realm"
 * 4. LAPIS is configured with issuer-uri=http://keycloak:8080/realms/test-realm (internal Docker hostname)
 * 5. Spring Security validates that the token's 'iss' claim matches the configured issuer-uri
 *
 * By fetching the token from within the container network, we ensure the 'iss' claim matches
 * what LAPIS expects, allowing issuer validation to succeed.
 *
 * Note: jwk-set-uri authentication doesn't have this issue because it doesn't validate the issuer claim.
 */
async function fetchTokenFromKeycloakViaContainer() {
  const { exec } = await import('child_process');
  const { promisify } = await import('util');
  const execAsync = promisify(exec);

  const __filename = fileURLToPath(import.meta.url);
  const __dirname = path.dirname(__filename);
  const composeDir = path.resolve(__dirname, '../../lapis');

  // Fetch token from within the Docker network using the internal Keycloak hostname
  const command = `docker compose exec -T lapisIssuerUri wget -q -O - --post-data='grant_type=password&client_id=lapis-client&username=test-user&password=test123' --header='Content-Type: application/x-www-form-urlencoded' http://keycloak:8080/realms/test-realm/protocol/openid-connect/token`;

  try {
    const { stdout } = await execAsync(command, { cwd: composeDir });
    const data = JSON.parse(stdout);
    return data.access_token;
  } catch (error) {
    throw new Error(`Failed to fetch token from container: ${error}`);
  }
}
