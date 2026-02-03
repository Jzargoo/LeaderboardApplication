#!/bin/bash

for f in /opt/custom/configs/connection/*.json; do
    CONNECTOR_NAME=$(basename "$f" .json)
    curl -X POST -H "Content-Type: application/json" --data @"$f" http://localhost:8083/connectors
done