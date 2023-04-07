#!/bin/bash
set -e
cd ${0%/*}
cd ../../
docker compose up -d --pull always
docker compose logs -f