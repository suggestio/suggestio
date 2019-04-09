FROM archlinux/base
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

RUN : \
  ## Install locales
  && echo en_US.UTF-8 UTF-8 > /etc/locale.gen \
  && echo ru_RU.UTF-8 UTF-8 >> /etc/locale.gen \
  && echo LANG=ru_RU.UTF-8 > /etc/locale.conf \
  && locale-gen \
  ## Для ускорения работы pacman'а, выставить только быстрые зеркала:
  && echo 'Server = http://mirror.yandex.ru/archlinux/$repo/os/$arch' > /etc/pacman.d/mirrorlist

## Скачать индексы репозиториев для установки пакетов на последующих шагах.
RUN pacman --noconfirm -Sy --needed base-devel git curl sudo

## Некоторые пакеты (mini, pikaur и т.д.) надо собирать. Поэтому нужен отдельный юзер с отдельной папкой.
ARG BUILD_HOME="/var/sio-builder-home"
ARG BUILD_UID=54362
ARG BUILD_GID=$BUILD_UID
## pacwrap - название pacman wrapper'а, который вызывается для сборки из AUR вместо pacman:
ARG PACWRAP_ORIG="yaourt"
ENV PACWRAP="y"
## BUILD_USER - имя юзера, под которым идёт сборка.
ARG BUILD_USER=$PACWRAP
ARG BUILD_GROUP=$BUILD_USER

RUN mkdir -p $BUILD_HOME \
  && chown $BUILD_UID:$BUILD_GID $BUILD_HOME \
  && groupadd -g $BUILD_GID "$BUILD_GROUP" \
  && useradd -d "$BUILD_HOME" -u $BUILD_UID -g $BUILD_GID -m -s /bin/bash $BUILD_USER \
  && passwd -d $BUILD_USER \
  && printf "\\n$BUILD_USER ALL=(ALL) NOPASSWD: /usr/bin/pacman\\n" | tee -a /etc/sudoers


## Хотелось задействовать pikaur, но он зависит от systemd PID=1
## Прячем нижележащий yaourt (или иную pacman-обёртку) за константой:
ARG PACWRAP_PKG_TAR_GZ="${PACWRAP_ORIG}-pkg.tar.gz"

## Нужно установить поддержку быстрой сборки из AUR:
RUN cd $BUILD_HOME \
  && for package in 'package-query' $PACWRAP_ORIG; do \
   printf "\n***\n*** Manually building [$package] from AUR...\n***\n\n" >&2 && \
   curl https://aur.archlinux.org/cgit/aur.git/snapshot/$package.tar.gz -o $PACWRAP_PKG_TAR_GZ && \
   sudo -u $BUILD_USER tar -xf $PACWRAP_PKG_TAR_GZ && \
   cd $BUILD_HOME/$package && \
   sudo -u $BUILD_USER makepkg --noconfirm -sifrc && \
   cd $BUILD_HOME && \
   rm -rf $package $PACWRAP_PKG_TAR_GZ; \
  done \
  && $PACWRAP_ORIG --version

## Создать универсальную постоянную ссылку на установленный pacman-wrapper,
## чтобы другие docker-файлы могли вызывать установку из AUR, не думая о нижележащей реализации:
RUN ln -s `which $PACWRAP_ORIG` /bin/$PACWRAP

## AUR-клиент установлен. Надо установить tini для сборки правильных контейнеров и прочее счастье:
RUN sudo -u $BUILD_USER $PACWRAP -S --noconfirm tini docker-systemctl-replacement-git

## Не удаляем никакие orphan packages, пусть всё остаётся для сборки на следующих этапах.
