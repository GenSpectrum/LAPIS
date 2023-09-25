import fs from 'fs';
import {
  addAccessKey,
  addGisaidFilter,
  compareRequests,
  DataInstance,
  generateLapisBaseUrl,
  LapisVersion,
  mapSearchParam,
  mapToPathV2,
} from './comparisonLapisV1andLapisV2FromLogs.spec';


const queriesPath = __dirname + '/queries';
const getRequestsFilename = 'get_requests.json';


type GetRequest = {
  url: string;
  sortResultsBy?: string;
};

const lapisInstance = 'gisaid';
describe('Compare LAPIS v1 and LAPIS v2 GET requests', () => {
  const getRequests: GetRequest[] = JSON.parse(
    fs.readFileSync(`${queriesPath}/${getRequestsFilename}`).toString(),
  );

  getRequests.forEach(request => {
    it('should be the same request result: ' + request.url, async () => {

      const v1Url = createRequestUrl(request, 'v1', lapisInstance);
      const v2Url = createRequestUrl(request, 'v2', lapisInstance);

      await compareRequests(v1Url, v2Url);
    });
  });
});

function createRequestUrl(getRequest: GetRequest, lapisVersion: LapisVersion, dataInstance: DataInstance): URL {
  const url = new URL(`${generateLapisBaseUrl(lapisVersion, dataInstance)}${getRequest.url}`);
  addGisaidFilter(url, dataInstance);
  addAccessKey(url, lapisVersion);
  mapToPathV2(url, lapisVersion);
  mapSearchParams(url, lapisVersion);

  return url;
}

function mapSearchParams(url: URL, lapisVersion: LapisVersion) {
  if (lapisVersion === 'v1') {
    return;
  }

  url.searchParams.forEach((value, key) => {
    const newKey = mapSearchParam(key, lapisVersion);
    if(newKey !== key) {
      url.searchParams.append(newKey, value);
      url.searchParams.delete(key);
    }
  });
}

