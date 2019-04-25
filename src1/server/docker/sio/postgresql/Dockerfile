## postgresql-контейнер для кластера.

FROM docker-registry.suggest.io/sio/os-base
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

## В docker-entrypoint.sh используется команда ping для инициализации slave:
RUN $PACMAN --noconfirm -S iputils

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

