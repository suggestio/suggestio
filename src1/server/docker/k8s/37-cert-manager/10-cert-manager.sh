#!/bin/sh

helm upgrade --install \
  --timeout 600s \
  cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --set installCRDs=true

