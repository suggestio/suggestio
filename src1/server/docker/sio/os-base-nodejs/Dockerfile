## Базовый образ для контейнеров, которые завязаны на nodejs.

FROM docker-registry.suggest.io:5000/sio/sio2/redis
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Установить всю необходимую утиль
RUN $PACMAN --noconfirm -Sy --needed --asdeps nodejs-lts-erbium
