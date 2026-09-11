const requiredMajorVersion = 24;
const actualMajorVersion = Number(process.versions.node.split('.')[0]);

if (actualMajorVersion < requiredMajorVersion) {
  throw new Error(
    `These tests require Node ${requiredMajorVersion} or newer, but you are running Node ${process.versions.node}. ` +
      'fetch only decompresses responses with Content-Encoding: zstd from Node 24 onwards, ' +
      'so on older versions the compression tests fail even when LAPIS is correct. ' +
      'Use the version pinned in .nvmrc at the repository root.'
  );
}
