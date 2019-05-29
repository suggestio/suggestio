#!/bin/sh

## Сохранение ssh-файлов из текущей папки.

KEY="id_ed25519"
KEY_PUB="$KEY.pub"
KNOWN_HOSTS="known_hosts"

NAME="deploy-ssh-keys"

kubectl delete secret $NAME -n gitlab-managed-apps

kubectl create secret generic $NAME \
    --from-file=$KEY \
    --from-file=$KEY_PUB \
    --from-file=$KNOWN_HOSTS \
    -n gitlab-managed-apps

