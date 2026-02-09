#!/usr/bin/env bash

set -euo pipefail

cd "../lapis"
./gradlew generateOpenApiDocs
./gradlew generateOpenApiDocs -Psegmented=true
./gradlew generateOpenApiDocs -Pauth=true

cd -
npm run generateLapisClient
npm run generateLapisClientMultiSegmented
npm run generateLapisClientWithAuth
