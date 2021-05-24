#!/bin/sh

helm upgrade --install \
  ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.externalTrafficPolicy="Local" \
  --set controller.service.type="NodePort" \
  --set controller.hostNetwork=true
  #--set controller.service.customPorts={222}

