## Базовый образ для контейнеров, которые завязаны на nodejs.

FROM docker.suggest.io:5000/sio/os-base
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Установить всю необходимую утиль
RUN $PACMAN --noconfirm -Sy --needed --asdeps nodejs
