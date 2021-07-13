#!/bin/sh

## https://docs.gitlab.com/charts/installation/deployment.html
## certmanager: https://docs.gitlab.com/charts/installation/tls.html#external-cert-manager-and-internal-issuer
## external ingress: https://docs.gitlab.com/charts/advanced/external-nginx/

## You may retrieve the CA root for these certificates from the `gitlab-wildcard-tls-ca` secret, via the following command. It can then be imported to a web browser or system store.
##   kubectl get secret -n gitlab-ce gitlab-wildcard-tls-ca -ojsonpath='{.data.cfssl_ca}' | base64 --decode > source.suggest.io.ca.pem

## After install, ensure one line:
## $ kubectl -n ingress-nginx edit deploy
## Check args list inside spec/template/spec/container/args
## Ensure line appended:
##    - --tcp-services-configmap=gitlab-ce/gitlab-nginx-ingress-tcp

## Ensure "gitlab-smtp-password" secret is created (fill & apply 25-smtp-secret.yaml).
## Else, webservice and sidekiq containers will fail to init due to mount failures.

NAMESPACE="gitlab-ce"

## Need to gitlab-runner to call API only inside cluster:
WEBSERVICE_PORT=8080

## WH port used inside runner config.toml clone_url parameter.
WORKHORSE_PORT=8181

helm upgrade \
  --install gitlab gitlab/gitlab \
  --namespace $NAMESPACE --create-namespace \
  --timeout 600s \
  --set global.hosts.https=true \
  --set global.ingress.tls.enabled=true \
  --set global.hosts.domain="suggest.io" \
  --set global.hosts.externalIP=192.168.100.100 \
  --set gitlab.webservice.service.externalPort=$WEBSERVICE_PORT \
  --set gitlab.webservice.service.internalPort=$WEBSERVICE_PORT \
  --set gitlab.webservice.service.workhorseExternalPort=$WORKHORSE_PORT \
  --set gitlab.webservice.service.workhorseInternalPort=$WORKHORSE_PORT \
  --set global.hosts.gitlab.name="source.suggest.io" \
  --set certmanager-issuer.email="konstantin.nikiforov@cbca.ru" \
  --set global.edition=ce \
  --set certmanager.install=false \
  --set gitlab-runner.install=true \
  --set gitlab-runner.gitlabUrl="http://gitlab-webservice-default.gitlab-ce.svc.cluster.local:$WEBSERVICE_PORT/" \
  --set gitlab-runner.runners.privileged=true \
  --set nginx-ingress.controller.service.type=NodePort \
  --set tcp.222="$NAMESPACE/gitlab-gitlab-shell:22" \
  --set global.ingress.class="nginx" \
  --set nginx-ingress.enabled=false \
  --set certmanager.install=false \
  --set global.ingress.configureCertmanager=false \
  --set minio.persistence.size=2Gi \
  --set gitlab.gitaly.persistence.size=5Gi \
  --set gitlab.task-runner.backups.cron.persistence.size=5Gi \
  --set prometheus.server.persistentVolume.size=3Gi \
  --set postgresql.persistence.size=2Gi \
  --set redis.master.persistence.size=1Gi \
  --set redis.replica.persistence.size=1Gi \
  --set global.smtp.enabled=true \
  --set global.smtp.password.secret="gitlab-smtp-password" \
  --set global.smtp.port=25 \
  --set global.smtp.domain="suggest.io" \
  --set global.smtp.authentication="login" \
  --set global.smtp.starttls_auto=false \
  --set global.smtp.address="TODO" \
  --set global.smtp.user_name="TODO"

## TODO These just doesn't work (does nothing):
#  --set gitlab.webservice.replicaCount=0 \
#  --set gitlab.gitlab-shell.replicaCount=0 \
#  --set gitlab.sidekiq.replicas=0 \
#  --set registry.enabled=false \
## To reduce replicaes, use `sh ./30-patch-scalers-after-helm-install.sh`

# --set global.ingress.annotations."kubernetes\.io/tls-acme"=true \

## Error: execution error at (gitlab/charts/gitlab/charts/task-runner/templates/deployment.yaml:219:23): A valid backups.objectStorage.config.secret is needed!
#  --set global.minio.enabled=false \  

## global.hosts.https=true. If false, HTTP 422, CSRF token validation fail on any action.

