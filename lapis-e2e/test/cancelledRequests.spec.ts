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

    const c1 = new AbortController();
    const res1 = await fetch(urlAligned, { signal: c1.signal });
    expect(res1.ok, `HTTP ${res1.status}`).to.equal(true);
    const p1 = consumeAndAbortOnFirstChunk(res1, c1);

    const c2 = new AbortController();
    const res2 = await fetch(urlAligned, { signal: c2.signal });
    expect(res2.ok, `HTTP ${res2.status}`).to.equal(true);
    const p2 = consumeAndAbortOnFirstChunk(res2, c2);

    const c3 = new AbortController();
    const res3 = await fetch(urlAligned, { signal: c3.signal });
    expect(res3.ok, `HTTP ${res3.status}`).to.equal(true);
    const p3 = consumeAndAbortOnFirstChunk(res3, c3);

    await Promise.all([p1, p2, p3]);

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
