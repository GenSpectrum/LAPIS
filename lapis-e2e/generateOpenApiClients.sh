#!/usr/bin/env bash

set -euo pipefail

cd "../lapis"
./gradlew generateOpenApiDocs
./gradlew generateOpenApiDocs -PopennessLevel=protected
./gradlew generateOpenApiDocs -Psegmented=true

cd -
npm run generateLapisClient
npm run generateLapisClientProtected
npm run generateLapisClientMultiSegmented
