# LAPIS Documentation

This documentation is a website built with
[Starlight](https://starlight.astro.build/) and [Astro](https://docs.astro.build).

## Commands

| Command                   | Action                                           |
|:--------------------------|:-------------------------------------------------|
| `npm install`             | Installs dependencies                            |
| `npm run dev`             | Starts local dev server at `localhost:4321`      |
| `npm run build`           | Build your production site to `./dist/`          |
| `npm run preview`         | Preview your build locally, before deploying     |
| `npm run astro ...`       | Run CLI commands like `astro add`, `astro check` |
| `npm run astro -- --help` | Get help using the Astro CLI                     |

For running and building the website, the environment variables `LAPIS_URL` and `CONFIG_FILE` must be set, e.g.:

```shell
CONFIG_FILE=../lapis-e2e/testData/testDatabaseConfig.yaml REFERENCE_GENOMES_FILE=../siloLapisTests/testData/reference_genomes.json LAPIS_URL=http://localhost:8080 npm run dev
```

## Deploying

Starlight is meant to be used to generate static HTML files that can be hosted by any standard web server.
This documentation however is meant to be specific for a given database configuration for LAPIS and SILO.

Thus, the documentation can only be built at deployment time (i.e. when the config is known), and not ahead of time.
We provide Docker images that can be used to build the documentation, and then serve it.

See [the Docker compose file](./test-docker-compose.yml) for an example of how to use the Docker image:
* The database config must be mounted to `/config/database_config.yaml`.
* The environment variable `LAPIS_URL` must be set to the URL of the backing LAPIS instance. 
This is used to generate links to that instance.
* Astro recommends to set the [`site` config option](https://docs.astro.build/en/reference/configuration-reference/#site).
This can be done via the environment variable `ASTRO_SITE`.

```shell
IMAGE=ghcr.io/genspectrum/lapis-docs docker compose -f test-docker-compose.yml up
```

### Deploying behind a proxy

The environment variable `BASE_URL` can be set to change the base URL of the documentation.
This is especially necessary when deploying to a subdirectory of a domain.

Suppose you want to host LAPIS on `your.domain` and the documentation on `your.domain/docs`,
and the documentation container will be running on `127.0.0.1:3000`.
Then you can set `BASE_URL` to `/docs/` and
configure your proxy to forward requests to `your.domain/docs` to `localhost:3000/docs`.
