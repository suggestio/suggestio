FROM docker-registry.suggest.io:5000/sio/sio2/redis
LABEL maintainer="konstantin.nikiforov@cbca.ru"

ARG peertube_home="/var/lib/peertube"

ARG user=peertube
ARG group=$user
ARG uid=796
ARG gid=$uid

RUN groupadd -g ${gid} ${group} && \
  useradd --system -d "${peertube_home}" --no-create-home -u ${uid} -g ${gid} -s /bin/nologin ${user}

RUN yes 1 | gpg --keyserver hkp://pool.sks-keyservers.net --recv-keys 583A612D890159BE && \
    sudo -u $PACWRAP $PACWRAP --noconfirm -Sy peertube nodejs-lts-erbium

