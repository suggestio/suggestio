## Базовый образ для образов, работающих с sioweb, в т.ч. jenkins и т.д.

FROM docker-registry.suggest.io:5000/sio/sio2/os-base
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Установить всю необходимую утиль
#RUN $PACMAN --noconfirm -Sy --needed jre11-openjdk-headless
RUN sudo -u $PACWRAP $PACWRAP --noconfirm -S jdk11-graalvm-bin
