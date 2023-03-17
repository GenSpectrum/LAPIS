import {expect} from "chai";
import {lapisClient} from "./common";

describe('SILO-LAPIS', () => {
    it('should return count for aggregated data', async () => {
        const result = await lapisClient.aggregated({filterParameter: {}});

        expect(result.count).equals(100);
    });
});