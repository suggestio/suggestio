#!/bin/bash

set -e

pacman -S --needed --noconfirm docker ethtool ebtables socat

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

## TODO /etc/kubernetes/kubelet: удалить/закомментить/обнулить строку с --api-servers
## TODO /etc/kubernetes/kubelet: KUBELET_ARGS="--cgroup-driver=system"
## TODO /etc/kubernetes/kubelet - настроить по смыслу с учётом ip-адреса хоста и https://wiki.archlinux.org/index.php/Kubernetes#Using_kubeadm

echo "***"
echo "*** Now, run 'kubeadm', and follow instructions." >&2
echo "***"

