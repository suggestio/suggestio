#!/bin/sh

## 1. Copy command or script somewhere.
## 2. Set passwords below.

CREDS=/tmp/admin-creds-values.yaml

## The default username and password of Artifactory are:
## User: admin
## Pass: password
cat > $CREDS << EOF
artifactory:
  admin:
    ip: "10.0.0.0/8, 127.0.0.0/8, 192.168.0.0/16" # Example: "*" to allow access from anywhere
    username: "admin"
    password: "password"
EOF
chmod 600 $CREDS


helm upgrade --install artifactory \
  --set artifactory.nginx.enabled=false \
  --set artifactory.ingress.enabled=true \
  --set artifactory.ingress.hosts[0]="ci.suggest.io" \
  --set artifactory.artifactory.service.type=NodePort \
  --set postgresql.enabled=true \
  --set artifactory.postgresql.enabled=true \
  #--set artifactory.postgresql.postgresqlPassword="TODO" \
  --namespace artifactory-oss --create-namespace \
  jfrog/artifactory-oss

rm $CREDS
