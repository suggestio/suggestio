#!/bin/bash

## 1. Get the application URL by running these commands:
# export POD_NAME=$(kubectl get pods --namespace matrix -l "app.kubernetes.io/name=matrix-sio,app.kubernetes.io/instance=matrix-sio" -o jsonpath="{.items[0].metadata.name}")
# export CONTAINER_PORT=$(kubectl get pod --namespace matrix $POD_NAME -o jsonpath="{.spec.containers[0].ports[0].containerPort}")
# echo "Visit http://127.0.0.1:8080 to use your application"
# kubectl --namespace matrix port-forward $POD_NAME 8080:$CONTAINER_PORT

CHART_DIR="./matrix-sio"

helm upgrade --install \
    matrix-sio $CHART_DIR/ \
    --namespace matrix --create-namespace \
    -f $CHART_DIR/values.yaml
