import { expect } from 'chai';
import { actuatorClient } from './common';

describe('The /actuator/health endpoint', () => {
  it('should return health status with silo component', async () => {
    const health = await actuatorClient.health();

    expect(health).to.have.property('status');
    expect(health.status).to.equal('UP');

    expect(health).to.have.property('components');
    expect(health.components).to.have.property('silo');

    const siloComponent = health.components.silo;
    expect(siloComponent).to.have.property('status');
    expect(siloComponent.status).to.equal('UP');

    expect(siloComponent).to.have.property('details');
    expect(siloComponent.details).to.have.property('dataVersion');
    expect(siloComponent.details.dataVersion).to.match(/\d+/);
    expect(siloComponent.details).to.have.property('siloVersion');
    expect(siloComponent.details.siloVersion).to.be.a('string').and.not.be.empty;
  });
});
