## Базовый образ для образов, работающих с sioweb, в т.ч. jenkins и т.д.

FROM docker-registry.suggest.io/sio/sio2/os-base
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Установить всю необходимую утиль
RUN $PACMAN --noconfirm -Sy --needed --asdeps go
