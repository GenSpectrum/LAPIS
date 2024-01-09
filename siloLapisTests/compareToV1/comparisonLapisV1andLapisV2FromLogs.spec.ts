import fs from 'fs';
import lodash from 'lodash';
import { z } from 'zod';

import { config } from 'dotenv';
import { expect } from 'chai';

config({
  path: `.env`,
});

const queriesPath = __dirname + '/queries';
const filePath = `${queriesPath}/logs_200.jsonl`;

const LAPIS_V1_URL = 'https://lapis.cov-spectrum.org';
const LAPIS_V2_URL = 'http://s1.int.genspectrum.org';

const v1AccessKey = process.env.LAPIS_V1_ACCESS_KEY;
const v2AccessKey = process.env.LAPIS_V2_ACCESS_KEY;

const lapisInstance = 'gisaid';

const filterDataSchema = z.union([z.string(), z.number()]);
const filterSchema = z.record(z.string(), z.union([filterDataSchema, z.array(filterDataSchema)]));

const logLineSchema =
  z.object({
    endpoint: z.string(),
    filter: filterSchema.optional(),
  });
type LogLine = z.infer<typeof logLineSchema>;


describe('Compare LAPIS v1 and LAPIS v2 GET requests from logs', () => {
  const logLines = readLogFile(filePath);

  for (const line of logLines) {
    if (line === undefined) {
      continue;
    }

    const shortEndpoint = line.endpoint.split('/').slice(-1)[0];
    it(`should compare requests: ${shortEndpoint}`, async () => {
      const requestV1 = createRequestUrl(line, 'v1', lapisInstance);
      const requestV2 = createRequestUrl(line, 'v2', lapisInstance);
      await compareRequests(requestV1, requestV2);
    });
  }
});

export type LapisVersion = 'v1' | 'v2';
export type DataInstance = 'open' | 'gisaid';

export async function compareRequests(requestV1: URL, requestV2: URL) {
  const resultV1 = await fetch(requestV1);
  const resultV2 = await fetch(requestV2);


  if ((resultV1.status !== resultV2.status) || (resultV1.status !== 200) || (resultV2.status !== 200)) {
    console.log('V1 status: ', resultV1.status);
    console.log('V2 status: ', resultV2.status);
    console.log(requestV1.toString());
    console.log(requestV2.toString());
    console.log('Error v1:', await getError(resultV1));
    console.log('Error v2:', await getError(resultV2));
  }

  expect(resultV1.status).equals(resultV2.status);

  const dataV1 = await getData(resultV1);
  const dataV2 = await getData(resultV2);

  const notInV1 = lodash.differenceWith(dataV2, dataV1, lodash.isEqual)
  const notInV2 = lodash.differenceWith(dataV1, dataV2, lodash.isEqual)

  if (notInV1.length > 0 || notInV2.length > 0) {
    // to get the nice difference view
    console.log(requestV1.toString());
    console.log(requestV2.toString());

    const notInV1 = lodash.differenceWith(dataV2, dataV1, lodash.isEqual)
    console.log('Not in v1: ', notInV1)

    const notInV2 = lodash.differenceWith(dataV1, dataV2, lodash.isEqual)
    console.log('Not in v2: ', notInV2)

    const dataV1Sorted = lodash.sortBy(dataV1, getFieldToSortBy(requestV2));
    const dataV2Sorted = lodash.sortBy(dataV2, getFieldToSortBy(requestV2));

    expect(dataV1Sorted).equals(dataV2Sorted);
  }
}

function createRequestUrl(logLine: LogLine, lapisVersion: LapisVersion, dataInstance: DataInstance): URL {
  const url = new URL(`${generateLapisBaseUrl(lapisVersion, dataInstance)}${logLine.endpoint}`);
  addGisaidFilter(url, dataInstance);
  addSearchParams(url, logLine, lapisVersion);
  addAccessKey(url, lapisVersion);
  mapToPathV2(url, lapisVersion);

  return url;
}

export function generateLapisBaseUrl(lapisVersion: LapisVersion, dataInstance: DataInstance): string {
  const { v1, v2 } = getLapisUrls(dataInstance);

  switch (lapisVersion) {
    case 'v1':
      return v1;
    case 'v2':
      return v2;
  }
}

function getLapisUrls(dataInstance: DataInstance) {
  switch (dataInstance) {
    case 'open':
      return {
        v1: `${LAPIS_V1_URL}/open`,
        v2: `${LAPIS_V2_URL}/open`,
      };
    case 'gisaid':
      return {
        v1: `${LAPIS_V1_URL}/gisaid`,
        v2: `${LAPIS_V2_URL}/gisaid`,
      };
  }
}

export function addGisaidFilter(url: URL, dataInstance: DataInstance) {
  switch (dataInstance) {
    case 'open':
      return;
    case 'gisaid':
      url.searchParams.append('nextcladeCoverageFrom', '0.01');
      break;
  }
}

export function mapToPathV2(url: URL, lapisVersion: LapisVersion) {
  if (lapisVersion === 'v2') {

    const replacements = [
      ['/nuc-mutations', '/nucleotideMutations'],
      ['/aa-mutations', '/aminoAcidMutations'],
      ['/nuc-insertions', '/nucleotideInsertions'],
      ['/aa-insertions', '/aminoAcidInsertions'],
      ['/v1', ''],
    ];

    replacements.forEach(([from, to]) => {
      url.pathname = url.pathname.replace(from, to);
    });
  }
}

export function addAccessKey(url: URL, lapisVersion: LapisVersion) {
  switch (lapisVersion) {
    case 'v1':
      url.searchParams.append('accessKey', v1AccessKey ?? '');
      break;
    case 'v2':
      url.searchParams.append('accessKey', v2AccessKey ?? '');
      break;
  }
}


function readLogFile(filePath: string) {
  return fs.readFileSync(filePath).toString().split('\n').map((line) => {
    try {
      const logObject = JSON.parse(line);
      return logLineSchema.parse(logObject);
    } catch (error) {
      console.log('Error parsing line: ', line);
      console.log(error);
    }
  });
}

function addSearchParams(url: URL, logLine: LogLine, lapisVersion: LapisVersion) {
  if (logLine.filter === undefined) {
    return;
  }
  Object.entries(logLine.filter).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      if (value.length === 0) {
        return;
      }

      switch (lapisVersion) {
        case 'v1':
          url.searchParams.append(mapSearchParam(key, lapisVersion), value.join(','));
          break;
        case 'v2':
          value.forEach((value) => {
            url.searchParams.append(mapSearchParam(key, lapisVersion), value.toString());
          })
          break;
      }
    } else {
      url.searchParams.append(key, value.toString());
    }
  });
}

export function mapSearchParam(key: string, lapisVersion: LapisVersion) {
  if (lapisVersion === 'v1') {
    return key;
  }
  switch (key) {
    case 'nucMutations':
      return 'nucleotideMutations';
    case 'aaMutations':
      return 'aminoAcidMutations';
    case 'nucInsertions':
      return 'nucleotideInsertions';
    case 'aaInsertions':
      return 'aminoAcidInsertions';
  }
  return key;
}

function getFieldToSortBy(url: URL) {
  switch (url.pathname.split('/').slice(-1)[0]) {
    case 'nucleotideMutations':
      return 'mutation';
    case 'aminoAcidMutations':
      return 'mutation';
    case 'nucleotideInsertions':
      return 'insertion';
    case 'aminoAcidInsertions':
      return 'insertion';
  }

  const fields = url.searchParams.get('fields')?.split(',').map(field => field.toLowerCase());

  if (fields === undefined) {
    return 'count';
  }
  return fields
}

async function getData(response: Response) {
  const responseText = await response.text();
  try {
    return JSON.parse(responseText).data;
  } catch (error) {
    console.log('Error parsing response: ', responseText, 'Response status: ', response.status, 'Response url: ', response.url);
    throw error;
  }
}

async function getError(response: Response) {
  const responseText = await response.text();
  try {
    return JSON.parse(responseText).error;
  } catch (error) {
    console.log('Error parsing response: ', responseText, 'Response status: ', response.status, 'Response url: ', response.url);
    throw error;
  }
}
