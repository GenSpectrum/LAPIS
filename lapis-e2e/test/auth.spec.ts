import { expect } from 'chai';
import { basePathWithJwkSetUriAuth, basePathWithPublicKeyAuth, lapisClientWithAuth } from './common';

// This token was created with the test private keys in testData/oauth-keys
const validToken =
  'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJsYXBpcy1hcGkiLCJpc3MiOiJsYXBpcy10ZXN0In0.Xu50CdL21tWvzfBE_BuQ-BPKx6C8Dj0EDaW5skK1IrYwRwpsKrocDc_l_51gDy_sYylJV1M3JYfhSMum_4e-iVpoGbRvf2J-T-yQD5oC-IKE27b2g6vXmFrbSottD4GHeUnn2Xh5aFrPyPu5SmF-_Vhfc2W-kqVE-zjxCj3uf28aHesV77FuvnXob9pZJjGHP-xyKfxqD88u5HunEor-YT1H-3Z3_aRazZis9kDDADa-4PLoDiYpfLMEaO4PMeIHN5412qhuOcwOmA28dNKmiJT9ye9osb2VLzDe9fl_CakJpkjlXF9D-_4Of81Q2X-BCOGPOnxHAXb6JrNBr_SWaQ';

describe('LAPIS that requires authentication', () => {
  const configs = [
    { name: 'via public key', basePath: basePathWithPublicKeyAuth, fetchToken: () => validToken },
    {
      name: 'via Keycloak jwk-set-uri',
      basePath: basePathWithJwkSetUriAuth,
      fetchToken: fetchTokenFromKeycloak,
    },
  ];

  for (let { name, basePath, fetchToken } of configs) {
    describe(name, () => {
      it('should reject call with without token', async () => {
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
