# Contributing

## LAPIS Developer Documentation

This section is a high-level overview to help you understand the LAPIS codebase.
It is intended for developers who want to contribute to LAPIS.

### LAPIS' Architecture

LAPIS is divided into three main components (from outer to inner):

* the HTTP layer
* the query mapping layer
* the SILO client layer

#### HTTP Layer

The HTTP layer is responsible for handling incoming requests and sending responses.
It mainly relies on [Spring Boot](https://spring.io/projects/spring-boot).
It is responsible for:

* parsing the incoming request and handling different request types
    * GET
    * POST with JSON body
    * POST with form data
* handling response data formats, such as JSON and CSV
* setting response headers, such as for the "Content-Disposition" header for the "download as file" feature
* response compression
* catching exceptions and making sure that LAPIS returns appropriate HTTP status codes

LAPIS makes extensive use of Spring hooks such as the `OncePerRequestFilter` to implement some of these features.
This approach has the advantage that it doesn't need to be implemented for every endpoint individually.

#### Query Mapping Layer

Once the data is extracted from the request, it is passed to the query mapping layer.
This layer maps the request to a 
[SILO query](https://github.com/GenSpectrum/LAPIS-SILO/blob/main/documentation/query_documentation.md).
A SILO query consists of two parts:

* the **query action** (aggregated, details, etc.): 
  The action type is directly determined by which **LAPIS endpoint** has been called.
  LAPIS offers endpoints for every SILO query action.
  Some request parameters (e.g. `fields` and `limit`) are used as parameters for the action.
* the **query filter**: This is determined by the **request parameters**.
  LAPIS tries to match the request parameters on the metadata fields defined in the database configuration.

There are some concepts that we use throughout the LAPIS code:

* Metadata fields are defined by the instance maintainer in the database configuration.
* "Sequence filters" are derived from the metadata fields.
  Available sequence filters depend on the types of metadata fields.
  Filters that match metadata field names (such as country or date) perform equality checks,
  while others support advanced filtering such as matching regular expressions
  or range filtering (such as `country.regex`, `dataFrom`, or `dateTo`).
* We called all request properties that are statically known "special properties".
  Those contain the mutation and insertion filters
  and fields that either relevant for the SILO action (such as `limit` or `orderBy`)
  or are used for LAPIS features (such as `dataFormat` or `compression`).
* The requests that LAPIS accepts are "sequence filters + special properties".
  Every request property that is not "special" is assumed to be a sequence filter.

#### SILO Client Layer

The SILO client layer is responsible for sending the query to SILO and processing the response.
This layer makes sure that LAPIS sends syntactically correct queries to SILO and handles the response.
It is also responsible for caching the responses of certain queries.

### OpenAPI Documentation And Swagger UI

LAPIS generates an OpenAPI documentation.
This can only happen at runtime,
because the request properties depend on the database configuration which is not known at compile time.
Thus, we cannot only rely on inference from the controller methods, but it needs customization.
As a developer, it is your responsibility to make sure that the OpenAPI documentation is correctly generated.

The OpenAPI documentation is important for users.
It should be their main source for information about which request parameters are available on the specific LAPIS
instance.

The Swagger UI is generated from the OpenAPI documentation.
It's a nice and convenient way to explore the LAPIS API (both for developers and users).
Testing a feature manually in LAPIS should usually involve testing it in the Swagger UI.

### Test Concept

#### Unit Tests

Unit tests test the individual layers of LAPIS separately.

* There are many **controller tests** that test the HTTP layer.
  They mock the query mapping layer.
  They use `MockMvc` to send a request to the controller and check the response.
  That allows the tests to test the full HTTP layer, including Spring's request processing.
* There are tests that test certain parts of the **query mapping layer**.
  Those are quite straight forward: Given a certain request, make sure that it yields the expected SILO query.
* There are tests that make sure that the **SILO client** sends the correct query to SILO.

#### End-to-End Tests

The end-to-end tests test the whole LAPIS stack.
They spin up SILO and LAPIS in Docker containers with a dummy dataset and run queries against them.
The main purpose is to test that **LAPIS and SILO work together**.

The tests generate a Typescript client from the OpenAPI documentation that the tests use to send requests to LAPIS.

* This has the advantage that we can **"test the OpenAPI spec"**.
    * We don't aim for full test coverage. This is rather meant as a basic drive-by sanity check.
* The generated client has some limitations, so there will be some tests that use plain `fetch` requests.
  This is e.g. the case for GET requests (because the generated client doesn't support them properly)
  and for testing error cases (because the generated client is designed to handle success cases).
  When possible, using the generated client is preferred.

## Creating A Release

This project uses [Release Please](https://github.com/google-github-actions/release-please-action) to generate releases.
On every commit on the `main` branch, it will update a Pull Request with a changelog.
When the PR is merged, the release will be created.
Creating a release means:

* A new Git tag is created.
* The Docker images of lapis and lapis-docs are tagged with the new version.
    * Suppose the created version is `2.4.5`, then it creates the tags `2`, `2.4` and `2.4.5`.

### Commit Messages

The changelog and the version number are determined by the commit messages.
Therefore, commit messages on the `main` branch should follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.
Since we squash-merge pull requests, the PR title should also follow conventional commits
(because it will become the commit message of the squashed commit).

Also refer to the Release Please documentation for more information on how to write commit messages.
If you want to indicate a breaking change, you can use the `BREAKING CHANGE` keyword in the commit message,
followed by the description of the breaking change.
