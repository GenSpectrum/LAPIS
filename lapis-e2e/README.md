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

## Test Data

The test data used for the singleSegmented case is a minimal sars_cov-2 dataset which should support generalized advanced queries as well as the SARS-COV2 variant queries. It uses lineages definitions defined in the `lineage_definitions.yaml` to support lineage queries. It additionally supports phylogenetic queries using a tree constructed using [augur](https://docs.nextstrain.org/projects/augur/en/29.1.0/index.html) from the example sequences. The tree can be reproduced by installing augur and running:

```bash
mkdir temp
jq -r '
  # for each JSON object (.),
  # grab the ID and sequence, then format as FASTA
  . as $obj
  | ">" + ($obj.metadata.primaryKey)
    + "\n"
    + ($obj.alignedNucleotideSequences.main)
' testData/singleSegmented/input_file.ndjson > temp/output.fasta

# Remove some sequences from the tree for testing
sed -e 's/^>key_1408408/>UNNAMED_1/' \
    -e 's/^>key_1749899/>UNNAMED_2/' \
    temp/output.fasta > temp/renamed.fasta

augur align --sequences  temp/renamed.fasta --output temp/aligned.fasta
augur tree --alignment temp/aligned.fasta --output temp/phylogenetic_tree_unlabeled_internals.nwk
augur refine --tree temp/phylogenetic_tree_unlabeled_internals.nwk --output-tree testData/singleSegmented/phylogenetic_tree.nwk
```
