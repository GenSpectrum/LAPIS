#! /bin/bash

set -euo pipefail

if [ $# -eq 0 ]; then
    echo "No arguments supplied. Use with '--token=<github token> --target-branch=<branch>'"
    exit 1
fi

dry_run_output=$(npm run --silent release-please-dry-run -- "$@")

echo "$dry_run_output" | grep --silent "Would open 1 pull request" || exit 0

extract_everything_between_tripledashes="/---/,/---/p"
drop_first_and_last_line='1d;$d'
echo "$dry_run_output" | sed -n "$extract_everything_between_tripledashes" | sed "$drop_first_and_last_line"
