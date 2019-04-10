## clamav для sioweb, который проверяет файлы в upload-контроллере через tcp.
## clamav запускается долго, но его не требуется часто перезапускать.
##
## Пример запуска контейера:
##   docker volume create sio-clamav
##   docker run -p 3310:3310 -v sio-clamav:/var/lib/clamav sio/clamav

FROM sio/os-base
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=clamav
ARG group=$user
ARG uid=64
ARG gid=$uid

RUN groupadd -g ${gid} ${group} && \
  useradd -d /dev/null --no-create-home -u ${uid} -g ${gid} -s /bin/false ${user}

## Установка и настройка clamav:
RUN $PACMAN --noconfirm -Sy clamav && \
  systemctl enable clamav-freshclam.service && \
  systemctl enable clamav-daemon.service

RUN sio-finalize-container.sh

ARG HOMEDIR=/var/lib/clamav

## Создать необходимые для работы директории
RUN for dir in /var/log/clamav /run/clamav $HOMEDIR; do \
      mkdir -p $dir && \
      chown $user:$group $dir; \
    done

## Базы clam должны переживать перезагрузки:
VOLUME $HOMEDIR

## Общение с антивирусом в контейнере происходит через указанный tcp-порт, пробрасываемый наружу:
ARG CLAMAV_TCP_PORT=3310

## Сказать, чтобы Открыть tcp-порт
## 127.0.0.1 нельзя в контейнере (точнее можно, но работать не будет).
RUN echo -e "TCPSocket $CLAMAV_TCP_PORT\nTCPAddr 0.0.0.0" >> /etc/clamav/clamd.conf

## Открыть tcp-порт clam для клиентуры.
EXPOSE $CLAMAV_TCP_PORT/tcp

CMD ["/bin/systemctl", "default"]

