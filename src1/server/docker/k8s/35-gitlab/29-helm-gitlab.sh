## https://docs.gitlab.com/charts/installation/deployment.html
## certmanager: https://docs.gitlab.com/charts/installation/tls.html#external-cert-manager-and-internal-issuer

## NOTICE: You've installed GitLab Runner without the ability to use 'docker in docker'.
## The GitLab Runner chart (gitlab/gitlab-runner) is deployed without the `privileged` flag by default for security purposes.
## This can be changed by setting `gitlab-runner.runners.privileged` to `true`. Before doing so, please read the GitLab Runner chart's documentation on why we
## chose not to enable this by default. See https://docs.gitlab.com/runner/install/kubernetes.html#running-docker-in-docker-containers-with-gitlab-runners


helm upgrade \
  --install gitlab gitlab/gitlab \
  --timeout 600s \
  --set global.hosts.domain="a.b.c" \
  --set global.hosts.externalIP=192.168.0.1 \
  --set certmanager-issuer.email="@cbca.ru" \
  --set global.edition=ce \
  --set certmanager.install=false \
  --set global.ingress.annotations."kubernetes\.io/tls-acme"=true \
  --namespace gitlab-ce --create-namespace \
  --set gitlab-runner.runners.privileged=true
