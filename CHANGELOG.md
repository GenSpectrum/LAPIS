# Changelog

## [0.3.3](https://github.com/GenSpectrum/LAPIS/compare/v0.3.2...v0.3.3) (2024-09-25)


### Features

* **lapis:** abort on startup when silo url has invalid syntax ([373d662](https://github.com/GenSpectrum/LAPIS/commit/373d6625abfb74c46b0ec65f020126c7c87089c4)), closes [#939](https://github.com/GenSpectrum/LAPIS/issues/939)


### Bug Fixes

* **lapis-docs:** Sequence file naming scheme uses indexes now not names ([1e7cd60](https://github.com/GenSpectrum/LAPIS/commit/1e7cd604a3c9ecbd8c5d21e0fa07903ad1aebbe9))

## [0.3.2](https://github.com/GenSpectrum/LAPIS/compare/v0.3.1...v0.3.2) (2024-09-10)


### Features

* **lapis:** add healthcheck to Docker containers ([0944990](https://github.com/GenSpectrum/LAPIS/commit/0944990e1aa97e47bfee43d8356571b23b190c58)), closes [#813](https://github.com/GenSpectrum/LAPIS/issues/813)
* **lapis:** add instance name to landing page json ([6c5f81b](https://github.com/GenSpectrum/LAPIS/commit/6c5f81b1eb445f15813d2166c195331effd5495e))

## [0.3.1](https://github.com/GenSpectrum/LAPIS/compare/v0.3.0...v0.3.1) (2024-08-26)


### Features

* **lapis:** omit sequences with `null` values in unaligned fasta downloads ([f1931a6](https://github.com/GenSpectrum/LAPIS/commit/f1931a686d0046e88016b18c90adb3502ea1575f)), closes [#912](https://github.com/GenSpectrum/LAPIS/issues/912)

## [0.3.0](https://github.com/GenSpectrum/LAPIS/compare/v0.2.11...v0.3.0) (2024-08-26)


### ⚠ BREAKING CHANGES

* **lapis:** use dot symbol as string search regex separator

### Features

* **lapis:** use dot symbol as regex separator [#908](https://github.com/GenSpectrum/LAPIS/issues/908) ([3728c7e](https://github.com/GenSpectrum/LAPIS/commit/3728c7efccdc06a36915e563490d9a5e9c2345e0))

## [0.2.11](https://github.com/GenSpectrum/LAPIS/compare/v0.2.10...v0.2.11) (2024-08-22)


### Features

* **lapis-docs:** document string search feature ([ae098a9](https://github.com/GenSpectrum/LAPIS/commit/ae098a91692ecafb1c96d8a2a10b0c197475f36b))
* **lapis:** hint to which regex syntax is used in Swagger docs ([ca4fcdc](https://github.com/GenSpectrum/LAPIS/commit/ca4fcdc7550240bd448a8a3960c5e206096bd80f)), closes [#903](https://github.com/GenSpectrum/LAPIS/issues/903)

## [0.2.10](https://github.com/GenSpectrum/LAPIS/compare/v0.2.9...v0.2.10) (2024-08-21)


### Features

* **lapis:** only allow regex search for fields that have `lapisAllowsRegexSearch: true` configured ([d146717](https://github.com/GenSpectrum/LAPIS/commit/d146717f9f39cc7d6fa786142b1389a125a55c0b)), closes [#898](https://github.com/GenSpectrum/LAPIS/issues/898)
* **lapis:** regex filtering for string columns [#898](https://github.com/GenSpectrum/LAPIS/issues/898) ([b3e7775](https://github.com/GenSpectrum/LAPIS/commit/b3e777554fde05e6ee9e6b789560468f947237e6))
* **lapis:** throw error when filtering by "equals" and "regex" for the same string field ([540d63a](https://github.com/GenSpectrum/LAPIS/commit/540d63a5e41c2693f93a39f6331736206e511416)), closes [#899](https://github.com/GenSpectrum/LAPIS/issues/899)

## [0.2.9](https://github.com/GenSpectrum/LAPIS/compare/v0.2.8...v0.2.9) (2024-08-19)


### Bug Fixes

* **lapis:** fix computation of downloaded file ending with multiple accept headers ([3b679a8](https://github.com/GenSpectrum/LAPIS/commit/3b679a86faec8b90f52c295d2d7ec659027ba4fb)), closes [#890](https://github.com/GenSpectrum/LAPIS/issues/890)
* **lapis:** never show whitelabel error page ([eb03345](https://github.com/GenSpectrum/LAPIS/commit/eb0334551dcf6c3a3972e4b95a887ffd056d8d07)), closes [#890](https://github.com/GenSpectrum/LAPIS/issues/890)

## [0.2.8](https://github.com/GenSpectrum/LAPIS/compare/v0.2.7...v0.2.8) (2024-07-30)


### Bug Fixes

* **lapis:** fix download file ending for csv-without-headers data format ([65afba0](https://github.com/GenSpectrum/LAPIS/commit/65afba063d6258f0d8efe7dc398d996da5f3881c)), closes [#871](https://github.com/GenSpectrum/LAPIS/issues/871)

## [0.2.7](https://github.com/GenSpectrum/LAPIS/compare/v0.2.6...v0.2.7) (2024-07-25)


### Features

* **lapis:** allow customizable filename for file downloads ([817d3f4](https://github.com/GenSpectrum/LAPIS/commit/817d3f45741e42c863aeee485c6a54a0b86aef99)), closes [#869](https://github.com/GenSpectrum/LAPIS/issues/869)

## [0.2.6](https://github.com/GenSpectrum/LAPIS/compare/v0.2.5...v0.2.6) (2024-07-18)


### Features

* **lapis:** auto remove entries from cache when heap limit is reached ([f804004](https://github.com/GenSpectrum/LAPIS/commit/f8040047450371191273086e9b159add3d5d2cb0))

## [0.2.5](https://github.com/GenSpectrum/LAPIS/compare/v0.2.4...v0.2.5) (2024-07-11)


### Features

* **lapis:** implement landing page when calling `/` [#856](https://github.com/GenSpectrum/LAPIS/issues/856) ([b886b7d](https://github.com/GenSpectrum/LAPIS/commit/b886b7d7b8e4d2a6bd939ceee2914a371c871b85))

## [0.2.4](https://github.com/GenSpectrum/LAPIS/compare/v0.2.3...v0.2.4) (2024-06-24)


### Bug Fixes

* **lapis-docs:** config generator: dateToSortBy, partitionBy and features is optional ([4e905e8](https://github.com/GenSpectrum/LAPIS/commit/4e905e8178eaebfe937764fc2fc95911db58dbe4))

## [0.2.3](https://github.com/GenSpectrum/LAPIS/compare/v0.2.2...v0.2.3) (2024-06-19)


### Bug Fixes

* **lapis:** nucleotideInsertionContains correctly handles segment ([4776a3e](https://github.com/GenSpectrum/LAPIS/commit/4776a3e9f48ed834ed64f24af28b05535e98d917))

## [0.2.2](https://github.com/GenSpectrum/LAPIS/compare/v0.2.1...v0.2.2) (2024-05-29)


### Features

* **lapis2:** use entrypoint.sh to allow passing JVM_OPTS through env variable ([#823](https://github.com/GenSpectrum/LAPIS/issues/823)) ([9b35b8a](https://github.com/GenSpectrum/LAPIS/commit/9b35b8aa1c76ab487fc775db42946bcac52a6b7f)), closes [#821](https://github.com/GenSpectrum/LAPIS/issues/821)

## [0.2.1](https://github.com/GenSpectrum/LAPIS/compare/v0.2.0...v0.2.1) (2024-05-27)


### Features

* **lapis2:** delete insertions metadata types [#804](https://github.com/GenSpectrum/LAPIS/issues/804) ([dd0ecd1](https://github.com/GenSpectrum/LAPIS/commit/dd0ecd12ad80f1e61b217d2f60d7247aff3ac2d9))

## [0.2.0](https://github.com/GenSpectrum/LAPIS/compare/v0.1.1...v0.2.0) (2024-05-23)


### ⚠ BREAKING CHANGES

* **lapis2:** throw exception on invalid variant query #797

### Features

* **lapis2:** clear cache when SILO is restarting ([6100bff](https://github.com/GenSpectrum/LAPIS/commit/6100bff487f6f0a9d3012414b66584019924e355))
* **lapis2:** make "maybe" mutation queries case-insensitive [#797](https://github.com/GenSpectrum/LAPIS/issues/797) ([0af9c7f](https://github.com/GenSpectrum/LAPIS/commit/0af9c7f01740d39768135d4fd4e1112c87a75af1))


### Bug Fixes

* **lapis2-docs:** relative links must not end with a `/` ([992210d](https://github.com/GenSpectrum/LAPIS/commit/992210d8cb0441de289fa71e1108bd3c1b35eecf))
* **lapis2:** also log which line of the SILO response failed to parse ([d1badea](https://github.com/GenSpectrum/LAPIS/commit/d1badea53d501435c0a26048d6ff0e874659ce39))
* **lapis2:** bring back request id header in OpenAPI docs ([eb75391](https://github.com/GenSpectrum/LAPIS/commit/eb7539164962f814d60e4920818d3f13c76c67e1)), closes [#627](https://github.com/GenSpectrum/LAPIS/issues/627)
* **lapis2:** log request id again ([26f83c9](https://github.com/GenSpectrum/LAPIS/commit/26f83c91eb51a0ccf0a30dd471480ac752d1dacb))
* **lapis2:** only accept a single variant query in a request [#798](https://github.com/GenSpectrum/LAPIS/issues/798) ([54df8e5](https://github.com/GenSpectrum/LAPIS/commit/54df8e5bfa432ea8c3ffdf00ea47a5fd3af29633))
* **lapis2:** throw exception on invalid variant query [#797](https://github.com/GenSpectrum/LAPIS/issues/797) ([980806a](https://github.com/GenSpectrum/LAPIS/commit/980806ac762e7186546edff3ea338569683dcc90))

## [0.1.1](https://github.com/GenSpectrum/LAPIS/compare/v0.1.0...v0.1.1) (2024-05-08)


### Features

* **lapis2:** allow filtering for null ([f680305](https://github.com/GenSpectrum/LAPIS/commit/f680305320a8a8c03a56902fd8754f25d167e935))

## [0.1.0](https://github.com/GenSpectrum/LAPIS/compare/v0.0.1...v0.1.0) (2024-05-07)


### ⚠ BREAKING CHANGES

* **lapis2:** read data from SILO as NDJSON

### Features

* adapt to changed SILO response structure ([6f58b5c](https://github.com/GenSpectrum/LAPIS/commit/6f58b5c27de8edfcc16e1ac931978a9fca10b36a))
* adapt variantFilter to Filters with columns and update tests ([88acb65](https://github.com/GenSpectrum/LAPIS/commit/88acb655616472d4af78733689da331c2f127994))
* add /sample to query paths [#501](https://github.com/GenSpectrum/LAPIS/issues/501) ([7ad9746](https://github.com/GenSpectrum/LAPIS/commit/7ad9746c52d7b80c9805b7f14df1c05d3d41b063))
* add access key parameter to /sample/info and remove downloadAsFile ([340f2ac](https://github.com/GenSpectrum/LAPIS/commit/340f2acf08ae49d6f9fb1965f1971fd22e21f618))
* add alignedNucleotideSequence, aminoAcidSequence endpoints ([575070a](https://github.com/GenSpectrum/LAPIS/commit/575070a31dfee6a3ff01e443d9b9241dcde11ae3))
* add aminoAcidInsertions endpoint returning json,csv and tsv ([bb3358e](https://github.com/GenSpectrum/LAPIS/commit/bb3358e84cd605728d1138db51f6f0a8f8f948a7))
* add dataFormat to swaggerUI for GET requests ([1514df7](https://github.com/GenSpectrum/LAPIS/commit/1514df72afa1294eb470ab27efa96a22ec121532))
* add description to response schema ([829448b](https://github.com/GenSpectrum/LAPIS/commit/829448b6d88dc10a7809f57c829436ebd97d42e2))
* add feature isSingleSegmentedSequence ([a90e331](https://github.com/GenSpectrum/LAPIS/commit/a90e3312dd742e789885495c74aef5643683f4dc))
* add fields to aggregated endpoint ([d183a0f](https://github.com/GenSpectrum/LAPIS/commit/d183a0f77cd9bbc419315a5299151e080fd8a616))
* add folder log to gitignore ([67a9aa9](https://github.com/GenSpectrum/LAPIS/commit/67a9aa94e6f44e246ac41e6fd529fc10c0f4bfa1))
* add info controller ([78f9c69](https://github.com/GenSpectrum/LAPIS/commit/78f9c69d3d9bad85f140ad2a79bbe25593ed6043))
* add info to lapis response ([afff4ae](https://github.com/GenSpectrum/LAPIS/commit/afff4ae12090759c6a274fdd181725420d1d49bc))
* add lineage queries for nextstrain, nextclade and gisaid ([e01acda](https://github.com/GenSpectrum/LAPIS/commit/e01acda5ab248a62490d7f0e67cb53cb7b627c79))
* add Maybe and bracket expression ([835b9c1](https://github.com/GenSpectrum/LAPIS/commit/835b9c17a3dff528536e99fe2e0f5c5b4b3b8a26))
* add NOf query ([217704e](https://github.com/GenSpectrum/LAPIS/commit/217704ed4db7107de3a0e35959e6af15b2ca9759))
* add not expression to variantQuery ([4b59e14](https://github.com/GenSpectrum/LAPIS/commit/4b59e1416d1e9006525be7395ddbe9148c6f3296))
* add nucleotideInsertions endpoint returning json,csv and tsv ([829b8e9](https://github.com/GenSpectrum/LAPIS/commit/829b8e95a63d841ee451f9e6b4a88aef8988c736))
* add Or expression to variantQuery ([5a0ef38](https://github.com/GenSpectrum/LAPIS/commit/5a0ef385fb64198fff655a465c3de9bebfb6b3f4))
* add Pangolineage_query ([c159ace](https://github.com/GenSpectrum/LAPIS/commit/c159ace5d82554ed2757596a39d7482d37083657))
* add sample to info route ([b703a9c](https://github.com/GenSpectrum/LAPIS/commit/b703a9c2ebe569213507ca72726c325caf944aa2))
* add unaligned nucleotide sequence route ([adda1dc](https://github.com/GenSpectrum/LAPIS/commit/adda1dc2107c7a6d8b2a3590a753312868371088))
* add variant query to FilterExpressionMapper ([64f0c8b](https://github.com/GenSpectrum/LAPIS/commit/64f0c8babf8b21a414128613e99111e91b71db5e))
* allow features to be empty ([a5bf1ea](https://github.com/GenSpectrum/LAPIS/commit/a5bf1ea5614f24c2308d8ddc482dfe652491606a))
* allow loweer case for mutations ([caa8713](https://github.com/GenSpectrum/LAPIS/commit/caa8713df7887f01187957167a388af7705a36b5))
* allow lower case for genes in variant queries ([5434e49](https://github.com/GenSpectrum/LAPIS/commit/5434e491cfa643b118bfb449b47448fae4a82866))
* allow lower case for insertions ([79c9f2d](https://github.com/GenSpectrum/LAPIS/commit/79c9f2d1b240b556c2bd457a821d9802ab2dbeca))
* allow lower case for segments and genes ([74b41bb](https://github.com/GenSpectrum/LAPIS/commit/74b41bba2f74c3f235175cdd2e244a70d9f3a024))
* allow lower case letters for mutations and insertions in variant queries ([fe3f8da](https://github.com/GenSpectrum/LAPIS/commit/fe3f8da0f11a9a250f6952446c5107dfccabc8dd))
* allow string arrays as filter for string and pango lineage fields [#507](https://github.com/GenSpectrum/LAPIS/issues/507) ([4d06713](https://github.com/GenSpectrum/LAPIS/commit/4d067132962d9c482294f6edcfca7b196055619a))
* allow that `fields` only contains the primary key in protected mode [#623](https://github.com/GenSpectrum/LAPIS/issues/623) ([0ac0709](https://github.com/GenSpectrum/LAPIS/commit/0ac070986c7bb9e1468704228fa7835e3ca858d6))
* allow upper and lowercase for queries ([b4ce361](https://github.com/GenSpectrum/LAPIS/commit/b4ce36137fd4059ade99afaae49efeee623cfa44))
* also enable returning TSV [#284](https://github.com/GenSpectrum/LAPIS/issues/284) ([9597e28](https://github.com/GenSpectrum/LAPIS/commit/9597e28f7d5494d7b7eeb7c6489f381b9e5cc36b))
* also get database config from info controller as YAML ([cd6536d](https://github.com/GenSpectrum/LAPIS/commit/cd6536d7ed28f1268e818a61b6018759b0d63989))
* also write logs to stdout ([a31522c](https://github.com/GenSpectrum/LAPIS/commit/a31522c9a85ec0d9329ca77aa37f001952c99e7b))
* amino acid and nucleotide insertion filters are case-insensitive ([667f487](https://github.com/GenSpectrum/LAPIS/commit/667f487abdf9124af83cfce7c49486f4fb4a6412))
* base implementation to provide the openness level in the config ([aff116c](https://github.com/GenSpectrum/LAPIS/commit/aff116ce691d79543dd97c9cbaccee09aee53cd1)), closes [#218](https://github.com/GenSpectrum/LAPIS/issues/218)
* change content type to text/plain when asking for csv/tsv without headers [#930](https://github.com/GenSpectrum/LAPIS/issues/930) ([8313996](https://github.com/GenSpectrum/LAPIS/commit/831399692fdf1bcce6fc9e4021e950c4ee8bc00f))
* configure CORS - allow all origins ([a7230f6](https://github.com/GenSpectrum/LAPIS/commit/a7230f6e0cb4d70d94e62454242a1df92d753e0d))
* **docs:** generate correct URLs for nucleotide sequences endpoints in multi-segmented case [#521](https://github.com/GenSpectrum/LAPIS/issues/521) ([6058b99](https://github.com/GenSpectrum/LAPIS/commit/6058b9923f399c89f99dbc7fbc57eac6c5d231af))
* **docs:** references/additional-request-properties [#604](https://github.com/GenSpectrum/LAPIS/issues/604) ([837e474](https://github.com/GenSpectrum/LAPIS/commit/837e4741ce27a2d85641a452c3ac9ab2d5f5490e))
* **docs:** transfer ambiguous symbols explanation [#551](https://github.com/GenSpectrum/LAPIS/issues/551) ([066f5b6](https://github.com/GenSpectrum/LAPIS/commit/066f5b65d9a4be7ca55a508aa66ec69b3f1f435a))
* document access key in swagger docs ([0cd3070](https://github.com/GenSpectrum/LAPIS/commit/0cd307047218bcc8ceb6890ba237df8a4bfe9976)), closes [#218](https://github.com/GenSpectrum/LAPIS/issues/218)
* e2e tests for filter by insertions ([fe8f0b9](https://github.com/GenSpectrum/LAPIS/commit/fe8f0b984895ed0a3a6775083658f07485e9a72c))
* enable /aggregated to return the data as CSV ([893ccca](https://github.com/GenSpectrum/LAPIS/commit/893cccac1a98acfa037677c18e5a8bc3035294cc))
* enable for operating behind a proxy that modifies the url ([78f0231](https://github.com/GenSpectrum/LAPIS/commit/78f0231e9a653841d5838c3859f09aea9002d04a))
* filter by insertions at all endpoints ([3376cb7](https://github.com/GenSpectrum/LAPIS/commit/3376cb79c12e00cb99f27184825de01dd6994e16))
* fix authentication, don't document authentication for now [#553](https://github.com/GenSpectrum/LAPIS/issues/553) ([f593073](https://github.com/GenSpectrum/LAPIS/commit/f593073c8571f295af0ca0c6fcdcb8da7a9143aa))
* forward Retry-After header when SILO is not available yet [#667](https://github.com/GenSpectrum/LAPIS/issues/667) ([62bcc7c](https://github.com/GenSpectrum/LAPIS/commit/62bcc7ce7b1966c10a2bab6e6e287a932fd1a668))
* get access key from request and read valid access keys from file ([4e07a6b](https://github.com/GenSpectrum/LAPIS/commit/4e07a6bcf7a95880ebe0c5322fdff5ef1178fa45)), closes [#218](https://github.com/GenSpectrum/LAPIS/issues/218)
* handle int and float columns ([d978a04](https://github.com/GenSpectrum/LAPIS/commit/d978a041decba559308c2dc50b8f5ec0e0845bae))
* handle reverse proxy headers ([8b80214](https://github.com/GenSpectrum/LAPIS/commit/8b8021407986cf73c9e35ff8d56fd7d79eb4ca75))
* implement /aminoAcidMutations ([d270104](https://github.com/GenSpectrum/LAPIS/commit/d270104fdcb9f3784917c4a324fc0f6f5d4359ca))
* implement /details endpoint ([aed93e9](https://github.com/GenSpectrum/LAPIS/commit/aed93e9e32dc400b8c21b18d9ada5b661deffc11)), closes [#283](https://github.com/GenSpectrum/LAPIS/issues/283)
* implement a request cache for mutation and insertion queries [#137](https://github.com/GenSpectrum/LAPIS/issues/137) ([17ff89c](https://github.com/GenSpectrum/LAPIS/commit/17ff89cbeef2ef118e4e2c85b0c66d51e8034939))
* implement a request id to trace requests [#586](https://github.com/GenSpectrum/LAPIS/issues/586) ([194e7fd](https://github.com/GenSpectrum/LAPIS/commit/194e7fd2682d62080ef4f32c0a93aec5472a2451))
* implement amino acid mutation filters ([f33c0ff](https://github.com/GenSpectrum/LAPIS/commit/f33c0ff25294dfee8a4d104671aa0f508da48caa))
* implement amino acid mutations in advanced variant queries ([85d0d82](https://github.com/GenSpectrum/LAPIS/commit/85d0d82f049c8a663042721ecf6411aaf759dd6b))
* implement compressing the response as zstd and gzip [#600](https://github.com/GenSpectrum/LAPIS/issues/600) ([d6e8d9d](https://github.com/GenSpectrum/LAPIS/commit/d6e8d9d7d759da85eac0e1ef23e508751b24d460))
* implement downloadAsFile [#599](https://github.com/GenSpectrum/LAPIS/issues/599) ([c522296](https://github.com/GenSpectrum/LAPIS/commit/c52229612280b9d36b0ff1a2dfaa8cb9436cf558))
* implement leftover variant query parts [#498](https://github.com/GenSpectrum/LAPIS/issues/498) ([556552f](https://github.com/GenSpectrum/LAPIS/commit/556552f000aaa9f8233af0e98d7784eeee8c9ac1))
* implement maybe mutation filters [#551](https://github.com/GenSpectrum/LAPIS/issues/551) ([fab1c72](https://github.com/GenSpectrum/LAPIS/commit/fab1c72be10cb4ff978621d99fbf85f2cb32651b))
* implement orderBy, limit and offset ([0a1dbe7](https://github.com/GenSpectrum/LAPIS/commit/0a1dbe77cc4e0e4ac0ce22329c2559ccf33fc9a6)), closes [#290](https://github.com/GenSpectrum/LAPIS/issues/290)
* implement the header parameter "headers=false" to disable the header in the returned CSV/TSV [#624](https://github.com/GenSpectrum/LAPIS/issues/624) ([a216e0c](https://github.com/GenSpectrum/LAPIS/commit/a216e0cb4bf40abc500e79a2c22aad7af02becd4))
* include link to GitHub issues in LapisInfo [#692](https://github.com/GenSpectrum/LAPIS/issues/692) ([ea42ac2](https://github.com/GenSpectrum/LAPIS/commit/ea42ac2ad8527286662f86e44ce870d8f48564a5))
* introduce SiloNotImplementedError ([4f94a06](https://github.com/GenSpectrum/LAPIS/commit/4f94a06cbbbb6a07efa895fd2805f0c705199ff5))
* **lapis2:** implement boolean columns ([7718b4f](https://github.com/GenSpectrum/LAPIS/commit/7718b4fe2c5f9785df4d0372060133bec71fc25c)), closes [#740](https://github.com/GenSpectrum/LAPIS/issues/740)
* **lapis2:** read data from SILO as NDJSON ([767b19d](https://github.com/GenSpectrum/LAPIS/commit/767b19dfb4222b550affde62b77fa370f0337da3)), closes [#741](https://github.com/GenSpectrum/LAPIS/issues/741)
* **lapis2:** stream data from SILO ([8fcf360](https://github.com/GenSpectrum/LAPIS/commit/8fcf36058ac12c0d2faa91cab5a50a757da2bc16)), closes [#744](https://github.com/GenSpectrum/LAPIS/issues/744)
* **lapis2:** support url encoded form POST requests ([f2f62b1](https://github.com/GenSpectrum/LAPIS/commit/f2f62b16f4e46f041875464042948d7a2e621e3b)), closes [#754](https://github.com/GenSpectrum/LAPIS/issues/754)
* log to files again (additionally to stdout) [#692](https://github.com/GenSpectrum/LAPIS/issues/692) ([1acedad](https://github.com/GenSpectrum/LAPIS/commit/1acedad0abe29200258fa3762db8cc2ee0214311))
* log whether request was cached [#717](https://github.com/GenSpectrum/LAPIS/issues/717) ([3de90f1](https://github.com/GenSpectrum/LAPIS/commit/3de90f1b42374f0554543f2c6edac729c2d83e6e))
* make fields case-insensitive [#502](https://github.com/GenSpectrum/LAPIS/issues/502) ([45e931e](https://github.com/GenSpectrum/LAPIS/commit/45e931e5f098cf17d3820274b47457929ab80b0a))
* make headers accessible to browser despite CORS ([4877c04](https://github.com/GenSpectrum/LAPIS/commit/4877c04054b274d8db9c7cc73f337cc8120126b9))
* make it possible to return data from /details as CSV [#284](https://github.com/GenSpectrum/LAPIS/issues/284) ([151e18f](https://github.com/GenSpectrum/LAPIS/commit/151e18f88682d35b38075bfed1826b4265d2ba8b))
* Mention that it's LAPIS' error page. ([7c365ce](https://github.com/GenSpectrum/LAPIS/commit/7c365ce7d1e5837dbdea295f89b16246431e7657))
* nucleotideMutations ary aminoAcidMutations endpoint return CSV/TSV ([cc3926c](https://github.com/GenSpectrum/LAPIS/commit/cc3926c8cb029cbc3c02a96ecfc4d0afe84edab9))
* pass on errors from SILO ([7f6153d](https://github.com/GenSpectrum/LAPIS/commit/7f6153da72e1e245781ce3935368db3ea8eea063))
* pass request id to SILO ([901e08a](https://github.com/GenSpectrum/LAPIS/commit/901e08addc9f65725149a16772a896078622840c))
* provide data for SILO in e2e tests ([4f74c62](https://github.com/GenSpectrum/LAPIS/commit/4f74c626f495deabdf428a658782b8a0f952883e))
* provide default values for file locations in Docker image ([e2248ac](https://github.com/GenSpectrum/LAPIS/commit/e2248ac41500a02b9e43dbd0730259d2888e3c91))
* provide full reference genome, and database config to new endpoints ([e406023](https://github.com/GenSpectrum/LAPIS/commit/e40602300286b2948a9c8e16fdc8d4963e2a1f4a))
* return a standard problem detail instead of a custom error format ([5f89b50](https://github.com/GenSpectrum/LAPIS/commit/5f89b50f38fb0deb3a979e3e8438bb0b3f86bbc0))
* return bad request error when unknown compression format in request [#647](https://github.com/GenSpectrum/LAPIS/issues/647) ([decaea5](https://github.com/GenSpectrum/LAPIS/commit/decaea5a94b60553318b7df87c4f25ac793e885e))
* return data version in each header ([27ecd70](https://github.com/GenSpectrum/LAPIS/commit/27ecd70116a9526954c1b83c1091cfba9d11261b))
* return structured mutation and insertion responses [#718](https://github.com/GenSpectrum/LAPIS/issues/718) ([af38e93](https://github.com/GenSpectrum/LAPIS/commit/af38e93a22dd83fc8cceb096443ecac78abe88cb))
* set Content-Type header to application/gzip|zstd when the compression property in the request was set [#665](https://github.com/GenSpectrum/LAPIS/issues/665) ([5592857](https://github.com/GenSpectrum/LAPIS/commit/5592857a6d170b32c7318ae00a3aa132d1cd6d77))
* show instance name on "not found" page ([4e444f1](https://github.com/GenSpectrum/LAPIS/commit/4e444f14d7305e9063b8b84b67c66269f9e960d2))
* stick to the default of having config value keys in camel case ([b14c91e](https://github.com/GenSpectrum/LAPIS/commit/b14c91ef88968d93dd3c3aec06e54abc6191aed1))
* support order by random [#658](https://github.com/GenSpectrum/LAPIS/issues/658) ([598b05f](https://github.com/GenSpectrum/LAPIS/commit/598b05ffa793631e8448c294a1bffd1e435e0673))
* throw more specialized exception to handle bad requests ([b8b86c2](https://github.com/GenSpectrum/LAPIS/commit/b8b86c236fe128f2724547db8d4e12384d4e79c2))
* try whether also caching aggregated queries makes cov spectrum faster ([bbfaf22](https://github.com/GenSpectrum/LAPIS/commit/bbfaf2270ddb0870122c8be7928d9d86be0713cd))
* use multiple access keys ([3ecc11a](https://github.com/GenSpectrum/LAPIS/commit/3ecc11a039005b2d5ba36095af9d84fdd4bc329d))
* wrap response data to match LAPIS 1 structure ([#324](https://github.com/GenSpectrum/LAPIS/issues/324)) ([e1976cc](https://github.com/GenSpectrum/LAPIS/commit/e1976ccf7bcac2a31b77cdfd039c26ede91d6be1))


### Bug Fixes

* add info route to endpoints that server aggregated data to allow access ([5b10e2b](https://github.com/GenSpectrum/LAPIS/commit/5b10e2b12e1021041fb01f786ccb6ec8543193b6))
* allow access in authorized mode when `fields` is specified non-fine-grained ([eaa832c](https://github.com/GenSpectrum/LAPIS/commit/eaa832cf4cc90692fe26872f63998033ceae066b))
* allow stop codon symbol for AminoAcidMutations ([74cfa74](https://github.com/GenSpectrum/LAPIS/commit/74cfa748454c0b010664570a8bf5441ec6d262e2))
* consider context path in Swagger UI link ([4b12b53](https://github.com/GenSpectrum/LAPIS/commit/4b12b53c189fbed920569d00e36f749d38019158))
* consider only servlet URL when checking auth ([0006ac2](https://github.com/GenSpectrum/LAPIS/commit/0006ac2d604f0d485c7cce8f75517f5a05781de2))
* consider only servlet URL when checking auth behind a proxy ([6b1767a](https://github.com/GenSpectrum/LAPIS/commit/6b1767aab15338b68cf9b1a54ae3e5ae322780a7))
* correct spelling ([1971713](https://github.com/GenSpectrum/LAPIS/commit/197171397ded4cf0ce5fea7b3e83ccb5209b36cd))
* details does no longer return insertions ([3686658](https://github.com/GenSpectrum/LAPIS/commit/3686658caeb69eabd8b6723dc8e09b06dd32650f))
* don't set Transfer-Encoding twice [#600](https://github.com/GenSpectrum/LAPIS/issues/600) ([7e49aa5](https://github.com/GenSpectrum/LAPIS/commit/7e49aa5e03bf296f75ebe82fd96f7775efe83978))
* don't store logs in files [#405](https://github.com/GenSpectrum/LAPIS/issues/405) ([1ca90f7](https://github.com/GenSpectrum/LAPIS/commit/1ca90f7b4181121efbf675ba0ffa38931a86f5e5))
* exact n of query with more elements than n ([1402019](https://github.com/GenSpectrum/LAPIS/commit/140201999099378e5f847b472b6653044a6b5bb9))
* file ending when downloading compressed file [#685](https://github.com/GenSpectrum/LAPIS/issues/685) ([6a51de0](https://github.com/GenSpectrum/LAPIS/commit/6a51de0da7c0cab4ef6bbf366f6902f5635c8c1c))
* import of property ([848f4b6](https://github.com/GenSpectrum/LAPIS/commit/848f4b620158a75797cc172234970d195669f65e))
* **lapis2:** prefer zstd over gzip [#738](https://github.com/GenSpectrum/LAPIS/issues/738) ([1fb67ac](https://github.com/GenSpectrum/LAPIS/commit/1fb67acd22e272f897b005853bf038eddd5d6e0d))
* make stop codon a valid symbolFrom for amino acid mutations ([43e5e9e](https://github.com/GenSpectrum/LAPIS/commit/43e5e9e86410145e796ce2e0a2e6767eee19d0d7))
* orderBy is an array ([0260036](https://github.com/GenSpectrum/LAPIS/commit/02600361ca444a07b7999b73d47b9c824090f68b))
* recognition of which endpoint was called to determine the filename of a download ([6c1674c](https://github.com/GenSpectrum/LAPIS/commit/6c1674c985f877b3e23c8fe2d985a06d9977494a))
* reduce log level when unable to read from cached request ([6810696](https://github.com/GenSpectrum/LAPIS/commit/68106968d25a29ddfc0d32fd99122f6308b16b82))
* remove header output from mutations and insertion endpoints ([ffd6b55](https://github.com/GenSpectrum/LAPIS/commit/ffd6b558523c41d7b3566bf70a89ad35f61ee256))
* remove request id header -&gt; make Swagger UI work again [#610](https://github.com/GenSpectrum/LAPIS/issues/610) ([0afa2a5](https://github.com/GenSpectrum/LAPIS/commit/0afa2a5e07543554962146e69491713265303415))
* return valid gzip when LAPIS returns an error response [#656](https://github.com/GenSpectrum/LAPIS/issues/656) ([db1c6cb](https://github.com/GenSpectrum/LAPIS/commit/db1c6cb77a16fcc00e59dd78d61ddee6d1c0ce94))
* Sending unknown value in fields returns incomprehensible error ([107396d](https://github.com/GenSpectrum/LAPIS/commit/107396deca9ed1a2514529056db4c582d25349d5))
* set default min proportion ([d033b34](https://github.com/GenSpectrum/LAPIS/commit/d033b344395bedd1c44d4e860d6facd9852d953e))
* speed up compressing responses [#717](https://github.com/GenSpectrum/LAPIS/issues/717) ([1624580](https://github.com/GenSpectrum/LAPIS/commit/16245803659269ae239edb8abff9f73ba9357722))
* use authorization filter before other filters [#660](https://github.com/GenSpectrum/LAPIS/issues/660) ([0dab1e7](https://github.com/GenSpectrum/LAPIS/commit/0dab1e7e765a1250eb1ed06bd894ec7dc0713b5a))
* use updated config ([d9d5a1a](https://github.com/GenSpectrum/LAPIS/commit/d9d5a1a1f307a8909adf5c2fb6c9724c8089935f))
* variant query with nextcladePangolineageQuery ([9158575](https://github.com/GenSpectrum/LAPIS/commit/915857543f28e4d2ab5d56d8cfbceb6071952cef))
* write empty string to CSV/TSV for null values [#708](https://github.com/GenSpectrum/LAPIS/issues/708) ([c8872ba](https://github.com/GenSpectrum/LAPIS/commit/c8872badc135a7e86c522f0c360db447b4f62ee3))
