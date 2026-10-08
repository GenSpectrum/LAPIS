import { expect } from 'chai';
import { actuatorClient } from './common';

describe('The /actuator/health endpoint', () => {
  it('should return health status with rhydb component', async () => {
    const health = await actuatorClient.health();

    expect(health).to.have.property('status');
    expect(health.status).to.equal('UP');

    expect(health).to.have.property('components');
    expect(health.components).to.have.property('rhydb');

    const rhydbComponent = health.components.rhydb;
    expect(rhydbComponent).to.have.property('status');
    expect(rhydbComponent.status).to.equal('UP');

    expect(rhydbComponent).to.have.property('details');
    expect(rhydbComponent.details).to.have.property('dataVersion');
    expect(rhydbComponent.details.dataVersion).to.match(/\d+/);
    expect(rhydbComponent.details).to.have.property('rhydbVersion');
    expect(rhydbComponent.details.rhydbVersion).to.be.a('string').and.not.be.empty;
  });
});
