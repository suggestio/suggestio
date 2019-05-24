## Образ контейнера для сборки sioweb с sbt.

FROM docker-registry.suggest.io/sio/sio2/os-base-www
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Поставить зависимости для сборки sio:
RUN $PACMAN -Sy --noconfirm --needed npm nodejs yarn sbt

## remove all useless stuff
RUN sio-finalize-container.sh

