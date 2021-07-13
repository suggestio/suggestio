#!/bin/sh

## replicasCount values are ignored, so patch them after installation:

kubectl patch -n gitlab-ce horizontalpodautoscaler.autoscaling/gitlab-gitlab-shell --patch '{"spec": {"maxReplicas": 1, "minReplicas": 1}}'
kubectl patch -n gitlab-ce horizontalpodautoscaler.autoscaling/gitlab-sidekiq-all-in-1-v1 --patch '{"spec": {"maxReplicas": 1, "minReplicas": 1}}'
kubectl patch -n gitlab-ce horizontalpodautoscaler.autoscaling/gitlab-webservice-default --patch '{"spec": {"maxReplicas": 1, "minReplicas": 1}}'
kubectl patch -n gitlab-ce horizontalpodautoscaler.autoscaling/gitlab-registry --patch '{"spec": {"maxReplicas": 1, "minReplicas": 1}}'
