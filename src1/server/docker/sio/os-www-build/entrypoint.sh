#!/bin/bash

## Подготовка к запуску контейнера сборки s.io.
## Нужно заполнить конфиги ivy2 и sbt, чтобы всё хорошо собиралось.

set -ex

cd $HOME

SBT_VERSION_ABI=1.0

mkdir -p $HOME/.ivy2 $HOME/.sbt/${SBT_VERSION_ABI} $HOME/.docker

## Заполнить конфиг для локального кэширования артефактов из artifactory:
echo ".ivy2/.credentials..."
cat > $HOME/.ivy2/.credentials <<EOF
realm=$K8S_SECRET_IVY2_REALM
host=$K8S_SECRET_IVY2_HOST
user=$K8S_SECRET_IVY2_USER
password=$K8S_SECRET_IVY2_PASSWORD
EOF

## Local Artifactory support for global.sbt was here until 2021-05-24

_fatal() {
  echo "$1\n Waiting for admin for debugging..." >&2
  sleep 600
  exit 1
}

## Выставить кол-во RAM в sbtopts.
SBT_OPTS="/etc/sbt/sbtopts"
if [ -z $K8S_SECRET_SBT_MEM ]; then
  K8S_SECRET_SBT_MEM=4096
fi
echo "-mem $K8S_SECRET_SBT_MEM" > "$SBT_OPTS"

## взять файлы из /root/.ssh-mount и скопировать в .ssh с правильными правами.
## .ssh-mount монтируется как read-only и с неправильными правами, поэтому вручную копипастим всё куда надо.
## Иначе ssh отказывается работать: unprotected private key.
SSH_MOUNT_DIR="/root/.ssh-mount"
SSH_DIR="/root/.ssh"
if [ -d $SSH_MOUNT_DIR ]; then
  mkdir -p $SSH_DIR &&
  cp $SSH_MOUNT_DIR/* $SSH_DIR/ &&
  chmod 0700 $SSH_DIR &&
  chmod 600 $SSH_DIR/* ||
    _fatal "Cannot copy secret keys from $SSH_MOUNT_DIR into $SSH_DIR ."
else
  _fatal "$SSH_MOUNT_DIR missing! deploy will not be possible." >&2
fi

## Продолжить выполнение исходных операций:
exec $@ || exit 0

