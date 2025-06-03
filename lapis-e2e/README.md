# End-to-end tests

These end-to-end test the integration of SILO and LAPIS.

How to execute the tests
(Given that you have a running LAPIS instance listening on localhost:8090, e.g. via `docker compose up`):

- Install NPM dependencies: `npm install`
- Generate the Typescript clients for LAPIS: `./generateOpenApiClients.sh`
- Execute the tests: `npm run test`

To only run single tests:

```
npm test -- --grep <Test Name>
```
