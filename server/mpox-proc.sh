#!/bin/bash
set -euo pipefail

cd /app

# Download data
echo "----- Download data -----"
wget https://data.nextstrain.org/files/workflows/monkeypox/metadata.tsv.gz
wget https://data.nextstrain.org/files/workflows/monkeypox/sequences.fasta.xz
gunzip metadata.tsv.gz
unxz sequences.fasta.xz

# Nextclade
echo "----- Run Nextclade -----"
./nextclade run \
  --input-dataset nextclade-data \
  --output-all nextclade-output \
  sequences.fasta

# Import
echo "----- Import into LAPIS -----"
java -Xmx2g -jar /app/lapis.jar --config /app/lapis-config.yml Lapis --update-data load-mpox,transform-mpox,switch-in-staging
