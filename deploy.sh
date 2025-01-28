#!/bin/bash

set -xe

dist="$(dirname $0)/target/site"

if [ ! -d "$dist" ]; then
    echo "$dist does not exist, run `make $dist` first"
    exit 1
fi

bucket="s3://replicant.fun/"

if [ -z "$distribution_id" ]; then
    distribution_id="E3A521EYVGRJXF"
fi

cd $dist

# Sync over bundles, cacheable for a year
aws s3 sync . $bucket --cache-control max-age=31536000,public,immutable --exclude "*" --metadata-directive REPLACE --include "bundles/*"

# Sync pages, do not cache
aws s3 sync . $bucket --cache-control "no-cache,must-revalidate" --exclude "bundles/*"

# Delete older bundles etc
aws s3 sync . $bucket --delete

# Invalidate Cloudfront cache
AWS_MAX_ATTEMPTS=10 aws cloudfront create-invalidation --distribution-id $distribution_id --paths /alias/ /build-options/ /bundles/ /event-handlers/ /hiccup/ /in-the-wild/ /index.html /keys/ /learn/ /life-cycle-hooks/ /nil/ /top-down/ /tutorials/
