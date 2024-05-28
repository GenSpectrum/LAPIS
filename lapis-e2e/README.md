# End-to-end tests

These end-to-end test the integration of SILO and LAPIS.

How to execute the tests
(Given that you have a running LAPIS instance listening on localhost:8090, e.g. via `docker compose up`):

- Generate the OpenAPI docs for LAPIS: `cd ../lapis && ./gradlew generateOpenApiDocs`
- Switch to test directory: `cd ../lapis-e2e/`
- Install NPM dependencies: `npm install`
- Generate a Typescript client for LAPIS: `npm run generateLapisClient`
- Execute the tests: `npm run test`
