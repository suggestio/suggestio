#!/bin/bash

## Сборка и установка aur-клиента с pacman-like-интерфейсом.

. ./inc/_build.sh

set -e

BUILD_USER="builder"
BUILD_HOME=/home/$BUILD_USER

pacman -S --noconfirm --needed --asdeps base-devel
pacman -S --noconfirm --needed sudo asp

groupadd $BUILD_USER || echo "group already exists"
useradd -d "$BUILD_HOME" -m -g $BUILD_USER $BUILD_USER || echo "user already exists"

printf "\\n$BUILD_USER ALL=(ALL) NOPASSWD: $(which pacman)\\n" | tee -a /etc/sudoers
passwd -d $BUILD_USER

curl 'https://aur.archlinux.org/cgit/aur.git/snapshot/pikaur.tar.gz' > pikaur-pkg.tar.gz
PACWRAP_ORIG="pikaur"
PACWRAP="y"

cd $BUILD_HOME
for package in $PACWRAP_ORIG ; do
  PKG_TAR_GZ="thepkgsnapshot.tar.gz"
  printf "\n***\n*** Manually building [$package] from AUR...\n***\n\n" >&2
  curl https://aur.archlinux.org/cgit/aur.git/snapshot/$package.tar.gz -o $PKG_TAR_GZ
  sudo -u $BUILD_USER tar -xf $PKG_TAR_GZ
  cd $BUILD_HOME/$package
  sudo -u $BUILD_USER makepkg --noconfirm -sifrc
  cd $BUILD_HOME
  rm -rf $package $PKG_TAR_GZ
done
$PACWRAP_ORIG --version
## Создать универсальную постоянную ссылку на установленный pacman-wrapper,
ln -s `which $PACWRAP_ORIG` /bin/$PACWRAP

