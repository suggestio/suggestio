#!/bin/bash

docker run -ti \
    -e PG_HOST="localhost" \
    -e PG_DATABASE="gitlabhq_production" \
    -e PG_USERNAME="gitlab" \
    -e PG_PASSWORD="gitlab" \
    --net=host \
    docker-registry.suggest.io/sio/gitlab $@
