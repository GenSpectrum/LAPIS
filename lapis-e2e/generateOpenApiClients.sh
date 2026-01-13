#!/usr/bin/env bash

set -euo pipefail

cd "../lapis"
./gradlew generateOpenApiDocs
./gradlew generateOpenApiDocs -Psegmented=true

cd -
npm run generateLapisClient
npm run generateLapisClientMultiSegmented
