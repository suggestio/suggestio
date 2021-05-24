## Образ контейнера для сборки sioweb с sbt.
## Следует монтировать ~/.ivy2/cache хотя бы как hostPath.

FROM docker-registry.suggest.io:5000/sio/sio2/os-base-www
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Поставить зависимости для сборки sio.
## openssh нужен для legacy-deploy на sio2/sio3/sio5, а так - не нужен.
RUN $PACMAN -Sy --noconfirm --needed npm nodejs yarn sbt jdk15-openjdk docker openssh rsync sed

## /usr/bin/sbt needs sed
RUN pacman -D --asexplicit sed

## remove all useless stuff
RUN sio-finalize-container.sh

RUN mkdir -p /root/.ivy2/cache /root/.sbt/1.0

COPY "entrypoint.sh" "/sbin/entrypoint.sh"

ENTRYPOINT ["/sbin/entrypoint.sh"]

