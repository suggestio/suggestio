
## Чтобы helm правильно работал, надо пошаманить:
## https://github.com/helm/helm/issues/3130#issuecomment-372931407
kubectl --namespace kube-system create serviceaccount tiller

kubectl create clusterrolebinding tiller-cluster-rule \
 --clusterrole=cluster-admin --serviceaccount=kube-system:tiller

kubectl --namespace kube-system patch deploy tiller-deploy \
 -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}' 

## Ожидаем переразвёртывание pod'а.
echo "Sleeping 10 seconds..."
sleep 10

helm repo update

## https://earlruby.org/2018/12/using-rook-ceph-for-persistent-storage-on-kubernetes/
## Deploy Prometheus and Grafana

helm install --name prometheus stable/prometheus
helm install --name grafana stable/grafana
