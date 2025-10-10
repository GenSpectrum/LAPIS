import { expect } from 'chai';
import { basePath } from './common';

const expectAborted = (reason: any) => {
  const name = reason?.name;
  const code = reason?.code;
  const message = String(reason?.message || '');
  expect(
    name === 'AbortError' || code === 'ERR_ABORTED' || /aborted|abort/i.test(message),
    `Expected an aborted fetch/stream, got reason: ${message || JSON.stringify(reason)}`
  ).to.equal(true);
};

const consumeAndAbortOnFirstChunk = async (res: Response, controller: AbortController) => {
  expect(res.body, 'Response has no body; cannot test mid-stream abort').to.exist;

  const body: any = res.body as any;
  expect(typeof body?.getReader === 'function');
  const reader = body.getReader();
  try {
    while (true) {
      const first = await reader.read();
      if (!first.done) controller.abort(); // abort mid-stream
      expect(first.done, 'Stream ended before we could abort').to.equal(false);
    }
  } catch (err) {
    expectAborted(err);
  }
  return;
};

describe('Client cancellation robustness', () => {
  it('handles cancelling two requests, then a following request still succeeds', async () => {
    const urlAligned = `${basePath}/sample/alignedNucleotideSequences`;
    const url = `${basePath}/sample/unalignedNucleotideSequences`;

    const aborts: AbortController[] = [];
    const pending: Promise<void>[] = [];

    for (let i = 0; i < 5; i++) {
      const c = new AbortController();
      aborts.push(c);
      const res = await fetch(urlAligned, { signal: c.signal });
      expect(res.ok, `HTTP ${res.status}`).to.equal(true);
      const p = consumeAndAbortOnFirstChunk(res, c);
      pending.push(p);
    }

    await Promise.all(pending);

    const okRes = await fetch(url);
    expect(okRes.ok, `Expected 2xx, got ${okRes.status}`).to.equal(true);
    expect(okRes.status).to.be.within(200, 299);

    const ct = okRes.headers.get('Content-Type') || '';
    expect(ct).to.match(/^text\/x-fasta/i);
    expect(ct).to.match(/charset=utf-8/i);

    const bodyText = await okRes.text();
    expect(bodyText).to.be.a('string');
    expect(bodyText).to.match(/^>/, 'FASTA should start with ">" line');
  });
});
