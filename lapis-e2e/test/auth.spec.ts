import {expect} from 'chai';
import {basePath, basePathWithPublicKeyAuth, lapisClientWithPublicKeyAuth, lapisInfoClient} from './common';

// These tokens were created with the test private keys in testData/oauth-keys
const validToken =
    'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJsYXBpcy1hcGkiLCJpc3MiOiJsYXBpcy10ZXN0In0.Xu50CdL21tWvzfBE_BuQ-BPKx6C8Dj0EDaW5skK1IrYwRwpsKrocDc_l_51gDy_sYylJV1M3JYfhSMum_4e-iVpoGbRvf2J-T-yQD5oC-IKE27b2g6vXmFrbSottD4GHeUnn2Xh5aFrPyPu5SmF-_Vhfc2W-kqVE-zjxCj3uf28aHesV77FuvnXob9pZJjGHP-xyKfxqD88u5HunEor-YT1H-3Z3_aRazZis9kDDADa-4PLoDiYpfLMEaO4PMeIHN5412qhuOcwOmA28dNKmiJT9ye9osb2VLzDe9fl_CakJpkjlXF9D-_4Of81Q2X-BCOGPOnxHAXb6JrNBr_SWaQ';
const expiredToken =
    'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJsYXBpcy1hcGkiLCJleHAiOjE3NzAzNzgzMjEsImlhdCI6MTc3MDM3NDcyMSwiaXNzIjoibGFwaXMtdGVzdCIsInNjb3BlIjoicmVhZCB3cml0ZSIsInN1YiI6InRlc3QtdXNlciJ9.huk7bz1CcekEr7ek9RlOKKFaKflKL0ZyJtWq_oFycr6YPJXqJ8zqu06YV1U2mIOHloTWA9-9cb6mKmZzQXrwB6qTnzOYugybth9QAMkDqrAtDbtlCfo4WHwl-RSO_iBA6MPbFFlt_V-v4yJeNuY52flJRXp8LIIL6I2DhoInGfH7gklyUrPJK2Omi-GsAVB1Pn2unV0D0oNFQaYQFYZ50ro-KN-sdJVBvRIKMw0j86xQO6-Z31HNhbYUrUPWtZSSdHVc4ETBDbyvtH_3kyh-x3rVYnqtb7c3VuLOLQYwdrx_HtYVXjOZK3ff05lEWZiqgZQttYPHDakD4t-9F0-Akg';

describe('Auth via public key configuration', function () {
    it('should reject call with without token', async () => {
        const result = await fetch(basePathWithPublicKeyAuth + '/sample/aggregated');

        expect(result.status).to.equal(401);
        expect(result.headers.get('www-authenticate')).to.equal('Bearer')
    });

    it('should reject call with invalid token', async () => {
        const result = await fetch(basePathWithPublicKeyAuth + '/sample/aggregated', {headers: {'Authorization': `Bearer ${expiredToken}`}});

        expect(result.status).to.equal(401);
        expect(result.headers.get('www-authenticate')).to.contain('Jwt expired')
    });

    it('should allow call with valid token', async () => {
        const response = await lapisClientWithPublicKeyAuth({accessToken: validToken}).postAggregated({
            aggregatedPostRequest: {},
        });

        expect(response.data[0].count).to.equal(100);
    });
});
