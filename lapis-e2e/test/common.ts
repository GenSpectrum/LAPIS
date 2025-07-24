import {
  Configuration,
  InfoControllerApi,
  LapisControllerApi,
  Middleware,
  SingleSegmentedSequenceControllerApi,
} from './lapisClient';
import { LapisControllerApi as LapisControllerApiMultiSegmented } from './lapisClientMultiSegmented';
import { expect } from 'chai';

import {
  LapisControllerApi as LapisControllerApiProtected,
  Configuration as ConfigurationProtected,
} from './lapisClientProtected';
import { MultiSegmentedSequenceControllerApi } from './lapisClientMultiSegmented';

export const basePath = 'http://localhost:8090';
export const basePathProtected = 'http://localhost:8092';
export const basePathMultiSegmented = 'http://localhost:8094';

const middleware: Middleware = {
  onError: errorContext => {
    if (errorContext.response) {
      console.log('Response status code: ', errorContext.response.status);
      console.log('Response body: ', errorContext.response.json());
    }
    if (errorContext.error) {
      console.error(errorContext.error);
    }
    return Promise.resolve();
  },
  post: async responseContext => {
    if (responseContext.response.status >= 300) {
      const response = responseContext.response.clone();
      console.log('Request:', responseContext.request);
      console.log('Request URL:', responseContext.url);
      console.log('Response status code:', response.status);
      console.log('Response body:', await response.json());
    }
    return Promise.resolve(responseContext.response);
  },
};

export const lapisClient = new LapisControllerApi(new Configuration({ basePath })).withMiddleware(middleware);
export const lapisInfoClient = new InfoControllerApi(new Configuration({ basePath })).withMiddleware(
  middleware
);
export const lapisClientProtected = new LapisControllerApiProtected(
  new ConfigurationProtected({ basePath: basePathProtected })
).withMiddleware(middleware);
export const lapisClientMultiSegmented = new LapisControllerApiMultiSegmented(
  new Configuration({ basePath: basePathMultiSegmented })
).withMiddleware(middleware);

export const lapisSingleSegmentedSequenceController = new SingleSegmentedSequenceControllerApi(
  new Configuration({ basePath })
).withMiddleware(middleware);

export const lapisMultiSegmentedSequenceController = new MultiSegmentedSequenceControllerApi(
  new Configuration({ basePath: basePathMultiSegmented })
).withMiddleware(middleware);

export function sequenceData(serverResponse: string) {
  const lines = serverResponse.split('\n').filter(line => line.length > 0);
  const primaryKeys = lines.filter(line => line.startsWith('>'));
  const sequences = lines.filter(line => !line.startsWith('>'));

  return {
    primaryKeys,
    sequences,
  };
}

export function expectIsZstdEncoded(arrayBuffer: ArrayBuffer) {
  const first4Bytes = new Uint8Array(arrayBuffer).slice(0, 4);

  expect([...first4Bytes]).deep.equals([Number('0x28'), Number('0xb5'), Number('0x2f'), Number('0xfd')]);
}

export function expectIsGzipEncoded(arrayBuffer: ArrayBuffer) {
  const first2Bytes = new Uint8Array(arrayBuffer).slice(0, 2);

  expect([...first2Bytes]).deep.equals([Number('0x1f'), Number('0x8b')]);
}
