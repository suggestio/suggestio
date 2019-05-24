## Образ контейнера для сборки sioweb с sbt.

FROM docker-registry.suggest.io/sio/sio2/os-base-www
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Поставить зависимости для сборки sio:
RUN $PACMAN -Sy --noconfirm --needed npm nodejs yarn sbt

## remove all useless stuff
RUN sio-finalize-container.sh

## Надо выкачать начальные зависимости sbt:
##
# Getting org.scala-sbt sbt 1.2.7  (this may take some time)...
#downloading https://repo1.maven.org/maven2/org/scala-sbt/sbt/1.2.7/sbt-1.2.7.jar ...
#	[SUCCESSFUL ] org.scala-sbt#sbt;1.2.7!sbt.jar (49ms)
# ...
#RUN cd /tmp && sbt exit

