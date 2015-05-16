#!/usr/bin/env sh

echo 'Removing folders under ./data/ and rebuilding'
cd ./data/ && rm -rf ./*/ && cd .. && ant
