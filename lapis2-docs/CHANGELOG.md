# Changelog

## 1.0.0 (2024-05-06)


### Features

* add config generator page ([45c9827](https://github.com/GenSpectrum/LAPIS/commit/45c9827de2231138b342a922c4c162c19333c167))
* **docs:** add LAPIS host to query Url [#522](https://github.com/GenSpectrum/LAPIS/issues/522) ([305e91c](https://github.com/GenSpectrum/LAPIS/commit/305e91cc690f98e47dda90d28a5344ce35c613a7))
* **docs:** add more important parts of the arc42 docs [#535](https://github.com/GenSpectrum/LAPIS/issues/535) ([13644e4](https://github.com/GenSpectrum/LAPIS/commit/13644e40153597e01e03cf943b016f94bfe35a24))
* **docs:** database config spec [#561](https://github.com/GenSpectrum/LAPIS/issues/561) ([a1c21d9](https://github.com/GenSpectrum/LAPIS/commit/a1c21d9dafd2aa67ba09b81ce32c7ae76001b833))
* **docs:** description of references/fields [#604](https://github.com/GenSpectrum/LAPIS/issues/604) ([5b02ca8](https://github.com/GenSpectrum/LAPIS/commit/5b02ca81414651403f8cce6e2534d920ab3b1ef2))
* **docs:** document how to provide data to the SILO preprocessing [#565](https://github.com/GenSpectrum/LAPIS/issues/565) ([2e0080d](https://github.com/GenSpectrum/LAPIS/commit/2e0080d0723f2e8a2719fb89e5617321fb73ca31))
* **docs:** document that there is a cache now [#137](https://github.com/GenSpectrum/LAPIS/issues/137) ([88906a4](https://github.com/GenSpectrum/LAPIS/commit/88906a499974cf8e36bdbd4f018ab4b209ba861d))
* **docs:** generate correct URLs for nucleotide sequences endpoints in multi-segmented case [#521](https://github.com/GenSpectrum/LAPIS/issues/521) ([6058b99](https://github.com/GenSpectrum/LAPIS/commit/6058b9923f399c89f99dbc7fbc57eac6c5d231af))
* **docs:** implement order by, limit and offset in query generator [#524](https://github.com/GenSpectrum/LAPIS/issues/524) ([6995d28](https://github.com/GenSpectrum/LAPIS/commit/6995d288b57bcd7a0229ddcc7f51e5cb324c1703))
* **docs:** improve introduction with ChatGPT [#561](https://github.com/GenSpectrum/LAPIS/issues/561) ([e026ccd](https://github.com/GenSpectrum/LAPIS/commit/e026ccda9bd7386c8ee5a0a2672b53fa802f1c5b))
* **docs:** improve reference/filters [#604](https://github.com/GenSpectrum/LAPIS/issues/604) ([31bb706](https://github.com/GenSpectrum/LAPIS/commit/31bb706d576359d652f4636f19c6d885c850508e))
* **docs:** list valid nucleotide and amino acid symbols [#573](https://github.com/GenSpectrum/LAPIS/issues/573) ([35ccf58](https://github.com/GenSpectrum/LAPIS/commit/35ccf58f7cc128e2eae26200dfdbf46b46c6b412))
* **docs:** move architecture docs to Starlight docs [#530](https://github.com/GenSpectrum/LAPIS/issues/530) ([a663067](https://github.com/GenSpectrum/LAPIS/commit/a663067729bde32b75b7d2a9d385e3f1a09b4b96))
* **docs:** reference how to start LAPIS and SILO [#569](https://github.com/GenSpectrum/LAPIS/issues/569) [#570](https://github.com/GenSpectrum/LAPIS/issues/570) ([0cc0d40](https://github.com/GenSpectrum/LAPIS/commit/0cc0d4048c10285d1ce9af9e891cb0b4e1de46e7))
* **docs:** references/additional-request-properties [#604](https://github.com/GenSpectrum/LAPIS/issues/604) ([837e474](https://github.com/GenSpectrum/LAPIS/commit/837e4741ce27a2d85641a452c3ac9ab2d5f5490e))
* **docs:** separate maintainer docs from user docs [#561](https://github.com/GenSpectrum/LAPIS/issues/561) ([98a9e30](https://github.com/GenSpectrum/LAPIS/commit/98a9e30edf233386b10f37263d6bebc78cf06fb9))
* **docs:** transfer ambiguous symbols explanation [#551](https://github.com/GenSpectrum/LAPIS/issues/551) ([066f5b6](https://github.com/GenSpectrum/LAPIS/commit/066f5b65d9a4be7ca55a508aa66ec69b3f1f435a))
* **docs:** transfer lapis and silo tutorial [#547](https://github.com/GenSpectrum/LAPIS/issues/547) ([db4932d](https://github.com/GenSpectrum/LAPIS/commit/db4932d50ca49163671afe91e979c559aee55cce))
* **docs:** update mutation filters to reflect the reference genomes ([03079d7](https://github.com/GenSpectrum/LAPIS/commit/03079d720887a582db408692a26729e654727ba5))
* fix authentication, don't document authentication for now [#553](https://github.com/GenSpectrum/LAPIS/issues/553) ([f593073](https://github.com/GenSpectrum/LAPIS/commit/f593073c8571f295af0ca0c6fcdcb8da7a9143aa))
* implement a request id to trace requests [#586](https://github.com/GenSpectrum/LAPIS/issues/586) ([194e7fd](https://github.com/GenSpectrum/LAPIS/commit/194e7fd2682d62080ef4f32c0a93aec5472a2451))
* **lapis2-docs:** configure base to handle links behind reverse proxy ([64c3cb7](https://github.com/GenSpectrum/LAPIS/commit/64c3cb7ec35c7ea3bc8630f345288750441100e2))
* **lapis2-docs:** configure LAPIS URL via environment variable ([2ede4aa](https://github.com/GenSpectrum/LAPIS/commit/2ede4aac2d17f8f4f0c2327da58e561d1021fe2b))
* **lapis2-docs:** document url encoded forms ([8c3ba60](https://github.com/GenSpectrum/LAPIS/commit/8c3ba601452415e6f3c1cea5209bdfdc0398efa1)), closes [#765](https://github.com/GenSpectrum/LAPIS/issues/765)
* return structured mutation and insertion responses [#718](https://github.com/GenSpectrum/LAPIS/issues/718) ([af38e93](https://github.com/GenSpectrum/LAPIS/commit/af38e93a22dd83fc8cceb096443ecac78abe88cb))
* support order by random [#658](https://github.com/GenSpectrum/LAPIS/issues/658) ([598b05f](https://github.com/GenSpectrum/LAPIS/commit/598b05ffa793631e8448c294a1bffd1e435e0673))
* try whether also caching aggregated queries makes cov spectrum faster ([bbfaf22](https://github.com/GenSpectrum/LAPIS/commit/bbfaf2270ddb0870122c8be7928d9d86be0713cd))


### Bug Fixes

* **docs:** display everything on the mutation filters page ([d912f59](https://github.com/GenSpectrum/LAPIS/commit/d912f5903ab74e8bd32f8089b211b181fb964b7d))
* **docs:** schema for uploaded config ([ecd2f9c](https://github.com/GenSpectrum/LAPIS/commit/ecd2f9c32f31bf651b9d803d25752a00320a9b70))
* **docs:** some minor improvements ([c77bf6f](https://github.com/GenSpectrum/LAPIS/commit/c77bf6f09a0727e5dde39d6d646051ecc9051d02))
* end to end tests failing due to timeout and new pango lineage alias in return ([d265809](https://github.com/GenSpectrum/LAPIS/commit/d2658098bb8178d92b42b4b79cf3768be7d9b259))
* file ending when downloading compressed file [#685](https://github.com/GenSpectrum/LAPIS/issues/685) ([6a51de0](https://github.com/GenSpectrum/LAPIS/commit/6a51de0da7c0cab4ef6bbf366f6902f5635c8c1c))
* **lapis2-docs:** tailwind config file name in Docker ([28add2b](https://github.com/GenSpectrum/LAPIS/commit/28add2bff3ffa98cdc446bba779d3a196d32e97a))
* **lapis2-docs:** typo in link ([5943e11](https://github.com/GenSpectrum/LAPIS/commit/5943e11bee467520bf233f17020cc52126c4cb17))
* **lapis2-docs:** use relative links so that they work when deployed behind a proxy [#725](https://github.com/GenSpectrum/LAPIS/issues/725) ([0cfa8ec](https://github.com/GenSpectrum/LAPIS/commit/0cfa8ec9d467df9de76c02ac1f60d5624b344d7e))
* update e2e tests: deletions are also mutations (from SILO) ([886a8f1](https://github.com/GenSpectrum/LAPIS/commit/886a8f17ddc271e2ea6629d4824bfd67366a19a0))
* use authorization filter before other filters [#660](https://github.com/GenSpectrum/LAPIS/issues/660) ([0dab1e7](https://github.com/GenSpectrum/LAPIS/commit/0dab1e7e765a1250eb1ed06bd894ec7dc0713b5a))
