## Базовый образ для seaweedfs-контейнеров (master, volume, ...).
## Собирает seaweedfs из исходников, создаёт домашнюю директорию и объявляет volume.
## Всё остальное должно быть реализовано на уровне volume/master-докерфайлов.

FROM docker.suggest.io:5000/sio/os-base
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=docker-registry
ENV user=$user \
    group=$user \
    homedir="/var/lib/docker-registry"
ARG uid=6442
ARG gid=$uid

RUN groupadd -g ${gid} ${group} && \
  useradd --system -m -d $homedir -u ${uid} -g ${gid} -s /bin/false ${user}

RUN sudo -u $PACWRAP $PACWRAP --noconfirm -Sy docker-registry2-git

VOLUME $homedir

# Docker registry 2.0 HTTP port:
EXPOSE 5000

RUN sio-finalize-container.sh

## TODO Добавить юзера для доступа:
COPY "docker-registry.htpasswd" "/etc/docker-registry/.htpasswd"

ENV REGISTRY_HTTP_SECRET="zoofai9Ohchi5nei9eex"

RUN systemctl enable docker-registry

CMD ["/sbin/systemctl", "default"]

