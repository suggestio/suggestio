#!/bin/bash

set -e

## xfsprogs - для rook-ceph
## python - для ansible
pacman -S --needed --noconfirm docker ethtool ebtables socat cni-plugins xfsprogs python

## Пробросить cni-плагины, иначе будет:
## ... from network weave-net/weave: failed to find plugin "portmap" in path [/opt/cni/bin]
## rpc error: code = Unknown desc = NetworkPlugin cni failed to teardown pod "coredns-fb8b8dccf-xv6pp_kube-system" network: failed to find plugin "portmap" in path [/opt/cni/bin]
mkdir -p /opt/cni/bin/
for src in `find /usr/lib/cni/`; do
  ln -s $src /opt/cni/bin/`basename $src`
done


#pikaur --noconfirm -S kubernetes-bin
## 2019-04-18 - Не собирается 1.14, нужно из PKGBUILD удалить строчку man.

. ./inc/_build.sh


PKG_TAR_GZ="thepkgsnapshot.tar.gz"
PACKAGE=kubernetes-bin

printf "\n***\n*** Manually building [$PACKAGE] from AUR...\n***\n\n" >&2
cd $BUILD_HOME
sudo -u $BUILD_USER pikaur -G kubernetes-bin
cd $PACKAGE/
## Пропатчить PKGBUILD, чтобы не было сборки man'ов, из-за которой все проблемы.
sed -e '/man\/man/d' -i PKGBUILD
sudo -u $BUILD_USER makepkg --noconfirm -sifrc
cd $BUILD_HOME
rm -rf $PACKAGE

systemctl enable kubelet.service
systemctl start kubelet.service

## Для работы weave нужен cni plugins -> portmap. Установить:
$PACWRAP -Sy --noconfirm --needed cni cni-plugins docker-gc-git

## TODO /etc/kubernetes/kubelet - настроить по смыслу с учётом ip-адреса хоста и https://wiki.archlinux.org/index.php/Kubernetes#Using_kubeadm
## TODO Это всё deprecated, уже надо в конфиге отдельном задавать:
## TODO Flag --address has been deprecated, This parameter should be set via the config file specified by the Kubelet's --config flag. See https://kubernetes.io/docs/tasks/administer-cluster/kubelet-config-file/
## TODO Flag --allow-privileged has been deprecated, will be removed in a future version
## TODO Flag --cgroup-driver has been deprecated, This parameter should be set via the config file specified by the Kubelet's --config flag. See https://kubernetes.io/docs/tasks/administer-cluster/kubelet-config-file/

## TODO /etc/kubernetes/kubelet, иначе будет panic anago-1.14...
# KUBELET_ADDRESS="--address=192.168.100.110 "
# KUBELET_ARGS="--cgroup-driver=systemd \
#              --bootstrap-kubeconfig=/etc/kubernetes/bootstrap-kubelet.conf \
#              --kubeconfig=/etc/kubernetes/kubelet.conf \
#              --config=/var/lib/kubelet/config.yaml \
#              --network-plugin=cni \
#              --pod-infra-container-image=k8s.gcr.io/pause:3.1"
# + удалить/закомментить/обнулить строку с --api-servers

## TODO на мастере: kubeadm create token
## на новой ноде: kubeadm join <token> ...

## TODO После добавления узла надо назначить его воркером, например:
##      kubectl label node k2.ku.suggest.io node-role.kubernetes.io/worker=worker


echo "***"
echo "*** Now, join kubernetes node into cluster." >&2
echo "***"

## На самом раннем шаге создания кластера, надо вызывать ещё и это:
## kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v1.10.1/src/deploy/recommended/kubernetes-dashboard.yaml
## kubectl apply -f "https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 | tr -d '\n')"

## TODO docker: /etc/systemd/system/docker.service.d/override.conf
## TODO docker: /etc/docker/docker.json (insecure registries)


