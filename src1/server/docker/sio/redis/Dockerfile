## БД redis
FROM docker-registry.suggest.io/sio/os-base
LABEL maintainer="konstantin.nikiforov@cbca.ru"

ARG redis_home="/var/lib/redis"
ARG redis_user="redis"
ARG redis_group=$redis_user
ARG redis_uid=726
ARG redis_gid=${redis_uid}
## TODO Вписывать порт в redis.conf, но для этого надо entrypoint-скрипт. Сейчас лень.
ARG redis_port=6379

## TODO Поддержка репликации - нужно replicaof <masterip> <masterport> в config.

RUN groupadd -g ${redis_gid} ${redis_group} && \
  useradd --system -d ${redis_home} --no-create-home -u ${redis_uid} -g ${redis_gid} -s /bin/false ${redis_user}

## Создать необходимые для работы директории
RUN for dir in ${redis_home} /run/redis; do \
      mkdir -p $dir && \
      chown $redis_user:${redis_group} $dir && \
      chmod 755 $dir; \
    done

VOLUME ${redis_home}

EXPOSE ${redis_port}

RUN $PACMAN --noconfirm -Sy --needed redis

## Т.к. это родительский контейнер, то пусть чисткой занимаются дочерние контейнеры.
#RUN sio-finalize-container.sh

COPY redis.conf /etc/redis.conf

RUN systemctl enable redis

ENV redis_user=${redis_user} \
    redis_group=${redis_group} \
    redis_port=${redis_port}

CMD ["/sbin/systemctl", "default"]

