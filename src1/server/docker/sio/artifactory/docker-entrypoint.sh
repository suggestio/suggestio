#!/bin/bash

## Подготовка к запуску контейнера artifactory.

set -x

_fatal() {
  echo "FATAL: $1" >&2
  sleep 300
  exit 1
}


## Распаковать начальные var-данные, если var-директория пуста.
if [ -z "$(ls -A ${ARTIFACTORY_VAR})" ]; then
  echo "Initializing empty ${ARTIFACTORY_VAR} with data..." 2>&1
  tar -C "${ARTIFACTORY_VAR}" -xvpf "${artifactory_var_init_tar_gz}" || _fatal "Cannot unpack ${artifactory_var_init_tar_gz} into ${ARTIFACTORY_var}! Waiting for admin from ssh..."
fi

## Продолжить нормальное исполнение исходной команды:
exec $@

