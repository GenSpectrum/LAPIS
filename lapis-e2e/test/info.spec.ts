import { expect } from 'chai';
import { lapisInfoClient } from './common';

describe('The info endpoint', () => {
  it('should return all infos', async () => {
    const info = await lapisInfoClient.getInfo();

    expect(info.dataVersion).to.match(/\d+/);
    expect(info.lapisVersion).to.be.not.empty;
    expect(info.siloVersion).to.be.not.empty;
    expect(info.requestId).to.be.not.empty;
    expect(info.requestInfo).to.be.not.empty;
    expect(info.reportTo).to.be.not.empty;
  });
});
