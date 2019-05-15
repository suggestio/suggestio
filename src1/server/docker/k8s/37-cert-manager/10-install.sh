## See https://cert-manager.readthedocs.io/en/latest/getting-started/install.html

kubectl create namespace cert-manager

# Disable resource validation on the cert-manager namespace
kubectl label namespace cert-manager certmanager.k8s.io/disable-validation=true

# Install the CustomResourceDefinitions and cert-manager itself
kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.7/deploy/manifests/cert-manager.yaml

