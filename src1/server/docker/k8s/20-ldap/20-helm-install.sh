#!/bin/bash

## https://github.com/jp-gouin/helm-openldap#tldr
## Ensure, helm repo is added:
##   helm repo add helm-openldap https://jp-gouin.github.io/helm-openldap/
##   helm repo update

helm upgrade --install \
    ldap-sio helm-openldap/openldap-stack-ha \
    --namespace ldap --create-namespace \
    -f values.yaml


## Uninstall:
##   helm uninstall ldap-sio -n ldap
##   kubectl delete ns ldap
##   kubectl delete -f 30-pv.yaml
