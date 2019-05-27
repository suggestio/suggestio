## artifactory для sio jenkins.

FROM docker-registry.suggest.io/sio/sio2/os-base-jre
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=artifactory
ARG group=$user
ARG uid=784
ARG gid=$uid

RUN groupadd -g ${gid} ${group} && \
  useradd --system -d "/opt/artifactory" --no-create-home -u ${uid} -g ${gid} -s /bin/nologin ${user}

RUN $PACMAN -S --noconfirm --needed --asdeps net-tools tar

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

## tar нужен, чтобы начальный etc распаковать в volume.
RUN $PACMAN -S --noconfirm --needed tar gzip

## Для запуска скрипта artifactory нужен gawk:
RUN pacman -D --asexplicit gawk tar gzip

RUN sio-finalize-container.sh


## Нужно начальные данные etc забэкапить, сделав из конфигов - ещё один volume
ENV artifactory_etc_init_tar_gz=/var/lib/artifactory-etc-initial.tar.gz \
    ARTIFACTORY_ETC=/etc/webapps/artifactory

RUN cd "${ARTIFACTORY_ETC}" && \
    tar -czpf "${artifactory_etc_init_tar_gz}" .

## Создать необходимые для работы директории:
RUN for dir in /run/artifactory /var/log/artifactory; do \
      mkdir -p $dir && \
      chown $user:$group $dir; \
    done

## Установка и настройка clamav:
RUN systemctl enable artifactory.service

## Общение с антивирусом в контейнере происходит через указанный tcp-порт, пробрасываемый наружу:
EXPOSE 8081/tcp

ARG entrypoint_sh="/sbin/docker-entrypoint.sh"

COPY docker-entrypoint.sh "$entrypoint_sh"

RUN chmod 755 "$entrypoint_sh"

ENTRYPOINT ["$entrypoint_sh"]

CMD ["/bin/systemctl", "default"]

VOLUME "$ARTIFACTORY_ETC"
VOLUME "/usr/share/webapps/artifactory/data"
VOLUME "/usr/share/webapps/artifactory/access"
VOLUME "/usr/share/webapps/artifactory/backup"
VOLUME "/usr/share/webapps/artifactory/replicator"
VOLUME "/usr/share/webapps/artifactory/support"

