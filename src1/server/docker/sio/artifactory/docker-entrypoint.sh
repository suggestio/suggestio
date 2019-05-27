#!/bin/bash

## Подготовка к запуску контейнера artifactory.

set -x

_fatal() {
  echo "FATAL: $1" >&2
  sleep 300
  exit 1
}


## Распаковать начальные etc-данные, если etc-директория пуста.
if [ -z "$(ls -A ${ARTIFACTORY_ETC})" ]; then
  echo "Initializing empty ${ARTIFACTORY_ETC} with data..." 2>&1
  tar -C "${ARTIFACTORY_ETC}" -xvpf "${artifactory_etc_init_tar_gz}" || _fatal "Cannot unpack ${artifactory_etc_init_tar_gz} into ${ARTIFACTORY_ETC}! Waiting for admin from ssh..."
fi

## Продолжить нормальное исполнение исходной команды:
exec $@

