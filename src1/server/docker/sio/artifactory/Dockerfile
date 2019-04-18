## artifactory для sio jenkins.

FROM docker.suggest.io:5000/sio/os-base-jre
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=artifactory
ARG group=$user
ARG uid=784
ARG gid=$uid

ENV ARTIFACTORY_HOME=/var/lib/artifactory

RUN groupadd -g ${gid} ${group} && \
  useradd --system -d ${ARTIFACTORY_HOME} --no-create-home -u ${uid} -g ${gid} -s /bin/nologin ${user}

RUN pacman -S --noconfirm --needed --asdeps net-tools

## Берём PKGBUILD прямо с github, т.к. в AUR как-то сыровато и слишком завязано на rwx-доступ в /opt/artifactory.
RUN echo "Packaging artifactory using PKGBUILD from dNhax/artifactory-oss ..." >&2 && \
   cd "$BUILD_HOME" && \
   sudo -u $BUILD_USER git clone "https://github.com/dNhax/artifactory-oss" && \
   cd "artifactory-oss" && \
   ## makepkg -i устанавливает systemd. Поэтому makepkg без -i, а остальное ставится вручную выше.
   sudo -u $BUILD_USER makepkg --noconfirm -frc && \
   $PACMAN --noconfirm -U artifactory-oss-*.pkg.tar* && \
   cd $BUILD_HOME && \
   rm -rf $package

RUN sio-finalize-container.sh

## Создать необходимые для работы директории:
RUN for dir in $ARTIFACTORY_HOME /run/artifactory /var/log/artifactory; do \
      mkdir -p $dir && \
      chown $user:$group $dir; \
    done

## Создать необходимые для работы директории
## Базы clam должны переживать перезагрузки:
## TODO Какая-то бредятина с artifactory из AUR: бинари лежат в home.
VOLUME ${ARTIFACTORY_HOME}

## Установка и настройка clamav:
RUN systemctl enable artifactory.service

## Общение с антивирусом в контейнере происходит через указанный tcp-порт, пробрасываемый наружу:
EXPOSE 8081/tcp

CMD ["/bin/systemctl", "default"]

