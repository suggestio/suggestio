#!/bin/sh

## Save ssh-keys for updating suggest.io on legacy virtual-machines.
## See at ~/projects/sio/-doc/ku.s.io/30-gitlab-runner-ssh-deploy

KEY="id_ed25519"
KEY_PUB="$KEY.pub"
KNOWN_HOSTS="known_hosts"
NAMESPACE="gitlab-ce"

NAME="deploy-ssh-keys"

kubectl delete secret $NAME -n $NAMESPACE || echo "No previous keys, first run, it's ok."

kubectl create secret generic $NAME \
    --from-file=$KEY \
    --from-file=$KEY_PUB \
    --from-file=$KNOWN_HOSTS \
    -n $NAMESPACE

