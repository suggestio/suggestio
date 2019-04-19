#!/bin/bash

set -e

ES_URL_PREFIX="elasticsearch.url:"
CONFIG=/etc/kibana/kibana.yml
grep $ES_URL_PREFIX < $CONFIG || {
  echo "elasticsearch.url: $ELASTICSEARCH_URL" >> $CONFIG
}

exec /sbin/systemctl default

