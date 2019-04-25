## Контейнер elasticsearch, который нужен для sio.
## 
## Пример запуска контейера:
##   docker volume create sio-clamav
##   docker run -p 3310:3310 -v sio-clamav:/var/lib/clamav sio/clamav

FROM docker-registry.suggest.io/sio/os-base-jre
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=elasticsearch
ARG group=$user
ARG uid=6473
ARG gid=$uid

RUN groupadd -g ${gid} ${group} && \
  useradd --system -d /dev/null --no-create-home -u ${uid} -g ${gid} -s /bin/false ${user}

ARG elasticsearch=elasticsearch5

## почему-то в PKGBUILD.depends упомянут systemd. Нужно пофиксить текст PKGBUILD'а и собрать через makepkg:
## Иначе, можно было бы использовать простое sudo -u $PACWRAP $PACWRAP -S --noconfirm elasticsearch5
RUN cd /tmp && \
  sudo -u $PACWRAP $PACWRAP -G $elasticsearch && \
  cd $elasticsearch/ && \
  sed -e "s@ 'systemd'@@" -i PKGBUILD && \
  sudo -u $PACWRAP makepkg -ic --noconfirm && \
  cd .. && \
  rm -rf $elasticsearch/

## скрипт bin/elasticsearch нуждается в ps и hostname
RUN $PACMAN -S --noconfirm inetutils procps-ng

RUN sio-finalize-container.sh

ARG HOMEDIR=/var/lib/elasticsearch

EXPOSE 9200/tcp \
       9300/tcp

## Создать необходимые для работы директории:
RUN for dir in /var/log/elasticsearch $HOMEDIR /run/elasticsearch; do \
      mkdir -p $dir && \
      chown $user:$group $dir; \
    done

VOLUME $HOMEDIR

CMD ["/bin/systemctl", "default"]

## Запретить демонизация процесса в фон.
RUN sed -e "s@ -d @ @g" -i "/usr/lib/systemd/system/elasticsearch.service" && \
  systemctl enable elasticsearch.service

COPY elasticsearch.yml /etc/elasticsearch/elasticsearch.yml
COPY log4j2.properties /etc/elasticsearch/log4j2.properties

