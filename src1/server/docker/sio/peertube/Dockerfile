FROM docker-registry.suggest.io:5000/sio/sio2/os-base-nodejs
LABEL maintainer="konstantin.nikiforov@cbca.ru"

ARG peertube_home="/var/lib/peertube"

ARG user=peertube
ARG group=$user
ARG uid=796
ARG gid=$uid

RUN groupadd -g ${gid} ${group} && \
    useradd --system -d "${peertube_home}" --no-create-home -u ${uid} -g ${gid} -s /bin/nologin ${user}

## Сразу установить yarn, чтобы сразу его переконфигурировать:
RUN pacman -S --asdeps yarn
RUN yarn config set --home enableTelemetry 0

## Загрузить пуб.ключ для проверки подписи релиза peertube:
ARG keyfileDest="$BUILD_HOME/chocobozzz.asc"
COPY chocobozzz.asc $keyfileDest
RUN sudo -u $BUILD_USER gpg --import $keyfileDest && rm $keyfileDest;

#RUN yes 1 | gpg --keyserver hkp://pool.sks-keyservers.net --recv-keys 583A612D890159BE && \
#RUN sudo -u $PACWRAP $PACWRAP --noconfirm -G peertube

## Ставим из AUR вручную, т.к. PKGBUILD и сборка требует некоторой коррекции.
RUN cd $BUILD_HOME && \
  for package in 'peertube'; do \
   PKG_TAR_GZ="thepkgsnapshot.tar.gz" && \
   printf "\n***\n*** Manually building [$package] from AUR...\n***\n\n" >&2 && \
   curl https://aur.archlinux.org/cgit/aur.git/snapshot/$package.tar.gz -o $PKG_TAR_GZ && \
   sudo -u $BUILD_USER tar -xf $PKG_TAR_GZ && \
   cd $BUILD_HOME/$package && \
   ## TODO PKGBUILD: Попытаться заменить depends=postgresql на postgresql-libs?
   sleep 1 && sudo -u $BUILD_USER makepkg --noconfirm -sifrc && \
   cd $BUILD_HOME && \
   rm -rf $package $PKG_TAR_GZ; \
  done

ENV PEERTUBE_HOME=/var/lib/peertube

## https://github.com/Chocobozzz/PeerTube/blob/develop/support/doc/production.md#prepare-peertube-directory
#RUN mkdir -p $PEERTUBE_HOME/config $PEERTUBE_HOME/storage $PEERTUBE_HOME/versions && \

RUN chown -R peertube:peertube $PEERTUBE_HOME


