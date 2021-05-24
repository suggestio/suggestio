## postgresql-контейнер для кластера.

FROM docker-registry.suggest.io:5000/sio/sio2/os-base
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=postgres
ARG group=$user
ARG uid=765
ARG gid=$uid

ARG HOMEDIR=/var/lib/postgres

RUN groupadd -g ${gid} ${group} && \
  useradd -d $HOMEDIR --no-create-home -u ${uid} -g ${gid} -s /bin/false ${user}

## Установка и настройка clamav:
RUN $PACMAN --noconfirm -Sy postgresql postgresql-old-upgrade && \
  systemctl enable postgresql.service

## В docker-entrypoint.sh используется команда telnet для паузы перед запуском slave:
RUN $PACMAN --noconfirm -S inetutils

RUN pacman -D --asexplicit sudo

RUN sio-finalize-container.sh


EXPOSE 5432/tcp

## Создать необходимые для работы директории:
RUN for dir in /run/postgresql $HOMEDIR; do \
      mkdir -p $dir && \
      chown $user:$group $dir; \
    done

## Базы clam должны переживать перезагрузки:
VOLUME $HOMEDIR

ENV PGDATA="$HOMEDIR/data" \
    HOMEDIR=$HOMEDIR \
    PG_MAX_WAL_SENDERS=8 \
    PG_WAL_KEEP_SEGMENTS=8 \
    POSTGRES_USER=postgres \
    POSTGRES_PASSWORD=postgres

ENTRYPOINT ["/bin/docker-entrypoint.sh"]
CMD ["postgres"]

COPY docker-entrypoint.sh /bin/
#CMD ["/bin/systemctl", "default"]

## Crunches for ru+RU DB. TODO Make locale db to en_US.
COPY ru_RU /usr/share/i18n/locales/
RUN echo ru_RU.UTF-8 UTF-8 >> /etc/locale.gen && \
    locale-gen

