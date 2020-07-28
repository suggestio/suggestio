## Базовый образ для сборке всех остальных обычных контейнеров.

FROM archlinux/base
LABEL maintainer="konstantin.nikiforov@cbca.ru"

RUN : \
  ## Install locales
  && echo en_US.UTF-8 UTF-8 > /etc/locale.gen \
  && echo ru_RU.UTF-8 UTF-8 >> /etc/locale.gen \
  && echo LANG=ru_RU.UTF-8 > /etc/locale.conf \
  && locale-gen \
  ## Для ускорения работы pacman'а, выставить только быстрые зеркала:
  && echo 'Server = http://mirror.yandex.ru/archlinux/$repo/os/$arch' > /etc/pacman.d/mirrorlist

ENV PACMAN="pacman --assume-installed=systemd"


## Скачать индексы репозиториев для установки пакетов на последующих шагах.
## Нельзя объявлять base-devel, т.к. это потянет за собой systemd
## Подразумевается, что sed уже установлен, а curl если и не установлен, то подтянется перед git.
RUN $PACMAN --noconfirm -Syu --needed --asdeps git sudo make fakeroot automake autoconf gcc file pkgconf which

## /bin/kill лежит тут:
RUN pacman -D --asexplicit util-linux

## Некоторые пакеты (mini, pikaur и т.д.) надо собирать. Поэтому нужен отдельный юзер с отдельной папкой.
ARG BUILD_UID=54362
ARG BUILD_GID=$BUILD_UID
## pacwrap - название pacman wrapper'а, который вызывается для сборки из AUR вместо pacman:
## Хотелось задействовать pikaur, но он зависит от systemd PID=1
## Прячем нижележащий yaourt (или иную pacman-обёртку) за константой:
ARG PACWRAP_ORIG="yaourt"
ARG PACWRAP="y"
ARG BUILD_USER=$PACWRAP

## BUILD_USER - имя юзера, под которым идёт сборка.
ENV PACWRAP=$PACWRAP \
    BUILD_USER=$BUILD_USER \
    BUILD_GROUP=$BUILD_USER \
    BUILD_HOME="/var/sio-builder-home"

RUN mkdir -p $BUILD_HOME && \
  chown $BUILD_UID:$BUILD_GID $BUILD_HOME && \
  groupadd -g $BUILD_GID "$BUILD_GROUP" && \
  useradd -d "$BUILD_HOME" -u $BUILD_UID -g $BUILD_GID -m -s /bin/bash $BUILD_USER 2>&1 && \
  passwd -d $BUILD_USER && \
  printf "\\n$BUILD_USER ALL=(ALL) NOPASSWD: $(which pacman)\\n" | tee -a /etc/sudoers

## Нужно установить поддержку быстрой сборки из AUR:
RUN cd $BUILD_HOME && \
  for package in 'package-query' $PACWRAP_ORIG; do \
   PKG_TAR_GZ="thepkgsnapshot.tar.gz" && \
   printf "\n***\n*** Manually building [$package] from AUR...\n***\n\n" >&2 && \
   curl https://aur.archlinux.org/cgit/aur.git/snapshot/$package.tar.gz -o $PKG_TAR_GZ && \
   sudo -u $BUILD_USER tar -xf $PKG_TAR_GZ && \
   cd $BUILD_HOME/$package && \
   ## TODO makepkg -i устанавливает systemd. Надо пробросить --assume-installed systemd в pacman.
   ## Через env PACMAN= как-то не помогает оно...
   sudo -u $BUILD_USER makepkg --noconfirm -sifrc && \
   cd $BUILD_HOME && \
   rm -rf $package $PKG_TAR_GZ; \
  done && \
  $PACWRAP_ORIG --version && \
  ## Создать универсальную постоянную ссылку на установленный pacman-wrapper,
  ln -s `which $PACWRAP_ORIG` /bin/$PACWRAP
 
## package-query и остальные тянут за собой systemd. Надо его удалить вручную:
RUN pacman -Rdd --noconfirm systemd


## Нужно установить systemd-скрипт на освободившееся место systemd.
RUN cd $BUILD_HOME && \
  for package in 'docker-systemctl-replacement-git'; do \
   PKG_TAR_GZ="thepkgsnapshot.tar.gz" && \
   printf "\n***\n*** Manually building [$package] from AUR...\n***\n\n" >&2 && \
   curl https://aur.archlinux.org/cgit/aur.git/snapshot/$package.tar.gz -o $PKG_TAR_GZ && \
   sudo -u $BUILD_USER tar -xf $PKG_TAR_GZ && \
   cd $BUILD_HOME/$package && \
   ## Подредактировать PKGBUILD: добавить симлинк на systemctl, добавить provides=systemd, исправить python/python3
   ## TODO python2 ошибочно требовался для docker-systemctl-replacement-git на момент 2020-07-27, из-за ошибки в PKGBUILD. Удалить потом, когда исправят или окостылить PKGBUILD.
   sed -re 's@(arch=)@provides=("systemd" "systemctl")\n\1@' -e "s@systemctl.py@systemctl3.py@g" -i PKGBUILD && \
   ## TODO makepkg -i устанавливает systemd. Надо пробросить --assume-installed systemd в pacman.
   ## Через env PACMAN= как-то не помогает оно...
   sudo -u $BUILD_USER makepkg --noconfirm -sifrc && \
   cd $BUILD_HOME && \
   rm -rf $package $PKG_TAR_GZ; \
  done && \
  ln -s `which systemctl3.py` /bin/systemctl && \
  systemctl --version
 
## Скрипт финализации контейнеров закинуть в контейнер:
COPY sio-finalize-container.sh /bin/sio-finalize-container.sh

## Не удаляем никакие orphan packages, пусть всё остаётся для сборки на следующих этапах.
