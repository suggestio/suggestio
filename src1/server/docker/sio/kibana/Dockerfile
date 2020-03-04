## Контейнер kibana.
## docker build --build-arg ELASTICSEARCH_URL=http://elasticsearch:9200

FROM docker-registry.suggest.io:5000/sio/sio2/os-base-nodejs
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## В контейнере недоступен systemd-sysusers, поэтому создать юзера внучную:
ARG user=kibana
ARG group=$user
ARG uid=206
ARG gid=$uid

RUN groupadd -g ${gid} ${group} && \
  useradd --system -d /dev/null --no-create-home -u ${uid} -g ${gid} -s /bin/false ${user}

RUN sudo -u $PACWRAP $PACWRAP -S --noconfirm --needed kibana5

RUN sio-finalize-container.sh

RUN systemctl enable kibana.service

## Номер порта:
ARG KIBANA_PORT=5601

EXPOSE $KIBANA_PORT/tcp

ARG HOMEDIR=/usr/share/kibana/data

## Обязательная ссылка на elasticsearch.
ENV ELASTICSEARCH_URL="http://localhost:9200"

## Заполнить конфиг:
RUN echo "server.port: $KIBANA_PORT" > /etc/kibana/kibana.yml && \
    echo 'server.host: "0.0.0.0"' >> /etc/kibana/kibana.yml

COPY start.sh /sbin/

RUN chown -R kibana:kibana /etc/kibana/* /usr/share/kibana/data /usr/share/kibana/optimize

VOLUME $HOMEDIR

## Создать необходимые для работы директории:
#RUN for dir in $HOMEDIR /usr/share/kibana/optimize; do \
#      mkdir -p $dir && \
#      chown $user:$group $dir; \
#    done

CMD ["/sbin/start.sh"]

