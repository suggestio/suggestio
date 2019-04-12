## Базовый образ для seaweedfs-контейнеров (master, volume, ...).
## Собирает seaweedfs из исходников, создаёт домашнюю директорию и объявляет volume.
## Всё остальное должно быть реализовано на уровне volume/master-докерфайлов.

FROM docker.suggest.io:5000/sio/os-base
LABEL maintainer="konstantin.nikiforov@cbca.ru"

RUN sudo -u $PACWRAP $PACWRAP --noconfirm -Sy seaweedfs-git

RUN sudo -u $PACWRAP $PACWRAP --noconfirm -S supercronic-git

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=weed
ENV user=$user \
    group=$user \
    homedir="/data"
ARG uid=126
ARG gid=$uid

RUN groupadd -g ${gid} ${group} && \
  useradd --system -m -d $homedir -u ${uid} -g ${gid} -s /bin/false ${user}

VOLUME $homedir

# volume server gprc port
EXPOSE 18080
# volume server http port
EXPOSE 8080
# filer server gprc port
EXPOSE 18888
# filer server http port
EXPOSE 8888
# master server shared gprc port
EXPOSE 19333
# master server shared http port
EXPOSE 9333
# s3 server http port
#EXPOSE 8333

## Для некоторых кусков entrypoint.sh нужен hostname:
RUN $PACMAN --noconfirm -S inetutils

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

RUN sio-finalize-container.sh

ENTRYPOINT ["/entrypoint.sh"]

