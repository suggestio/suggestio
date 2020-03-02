## artifactory для sio jenkins.

FROM docker-registry.suggest.io/sio/sio2/os-base-jre
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=artifactory
ARG group=$user
ARG uid=784
ARG gid=$uid
ARG ARTIFACTORY_HOME=/opt/artifactory

RUN groupadd -g ${gid} ${group} && \
  useradd --system -d "$ARTIFACTORY_HOME" --no-create-home -u ${uid} -g ${gid} -s /bin/nologin ${user}

RUN $PACMAN -S --noconfirm --needed --asdeps net-tools tar

## Пакет требует эти зависимости:
#RUN $PACMAN -S --noconfirm --needed inetutils jre11-openjdk

## Берём PKGBUILD прямо с github, т.к. в AUR как-то сыровато и слишком завязано на rwx-доступ в $ARTIFACTORY_HOME.
#RUN echo "Packaging artifactory using PKGBUILD from dNhax/artifactory-oss ..." >&2 && \
#   cd "$BUILD_HOME" && \
#   sudo -u $BUILD_USER git clone "https://github.com/dNhax/artifactory-oss" && \
#   cd "artifactory-oss" && \
#   ## makepkg -i устанавливает systemd. Поэтому makepkg без -i, а остальное ставится вручную выше.
#   sudo -u $BUILD_USER makepkg --noconfirm -frc && \
#   $PACMAN --noconfirm -U artifactory-oss-*.pkg.tar* && \
#   cd $BUILD_HOME && \
#   rm -rf $package

RUN sudo -u $PACWRAP $PACWRAP --noconfirm -Sy artifactory-oss

## tar нужен, чтобы начальный etc распаковать в volume,
## для запуска скрипта artifactory нужен gawk:
RUN $PACMAN -S --noconfirm --needed tar gzip gawk && \
    pacman -D --asexplicit gawk tar gzip

RUN sio-finalize-container.sh


## 2020-03-02 - С новой версией PKGBUILD возникли сложности с правами и корректной конфигурацией.
RUN chown -R $user:$group $ARTIFACTORY_HOME && \
    ln -s $ARTIFACTORY_HOME /var/lib/artifactory

## Нужно начальные данные etc забэкапить, чтобы, сделав из конфигов ещё один volume, распаковать начальные конфиги в пустой volume.
ENV artifactory_etc_init_tar_gz=/var/lib/artifactory-etc-initial.tar.gz \
    ARTIFACTORY_ETC=$ARTIFACTORY_HOME/etc

RUN cd "${ARTIFACTORY_ETC}" && \
    tar -czpf "${artifactory_etc_init_tar_gz}" .

## Создать необходимые для работы директории:
RUN for dir in /run/artifactory; do \
      mkdir -p $dir && \
      chown $user:$group $dir; \
    done

## Установка и настройка clamav:
RUN systemctl enable artifactory.service

## Общение с антивирусом в контейнере происходит через указанный tcp-порт, пробрасываемый наружу:
EXPOSE 8081/tcp

COPY "docker-entrypoint.sh" "/sbin/docker-entrypoint.sh"

ENTRYPOINT ["/sbin/docker-entrypoint.sh"]

CMD ["/bin/systemctl", "default"]

VOLUME "$ARTIFACTORY_ETC"
VOLUME "$ARTIFACTORY_HOME/data"
VOLUME "$ARTIFACTORY_HOME/access"
VOLUME "$ARTIFACTORY_HOME/backup"
VOLUME "$ARTIFACTORY_HOME/replicator"
VOLUME "$ARTIFACTORY_HOME/support"

