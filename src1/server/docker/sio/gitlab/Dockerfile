
# Configure your /etc/webapps/gitlab/gitlab.yml
# Set up your redis to run on /var/run/redis/redis.sock or configure gitlab to use redis TCP
# Put a secret bytestring to /etc/webapps/gitlab/secret
# Copy /usr/share/doc/gitlab/secrets.yml.example to /etc/webapps/gitlab/secrets.yml and configure it
# Copy a database example config from /usr/share/doc/gitlab/ to /etc/webapps/gitlab/database.yml and configure it
# Setup the database:
# su - gitlab -s /bin/sh -c "cd '/usr/share/webapps/gitlab'; EXECJS_RUNTIME=Disabled bundle-2.5 exec rake gitlab:setup RAILS_ENV=production"
# Finally run the following commands to check your installation:
# su - gitlab -s /bin/sh -c "cd '/usr/share/webapps/gitlab'; EXECJS_RUNTIME=Disabled bundle-2.5 exec rake gitlab:env:info RAILS_ENV=production"
# su - gitlab -s /bin/sh -c "cd '/usr/share/webapps/gitlab'; EXECJS_RUNTIME=Disabled bundle-2.5 exec rake gitlab:check RAILS_ENV=production"
# Дополнительные зависимости для 'gitlab'
#    postgresql: database backend [установлено]
#    mysql: database backend
#    python2-docutils: reStructuredText markup language support
#    smtp-server: mail server in order to receive mail notifications

FROM docker-registry.suggest.io/sio/redis
LABEL maintainer="konstantin.nikiforov@cbca.ru"

ARG gitlab_home="/var/lib/gitlab"
ARG gitlab_user="gitlab"
ARG gitlab_group=$gitlab_user
ARG gitlab_uid=682
ARG gitlab_gid=${gitlab_uid}
## TODO Вписывать порт в redis.conf, но для этого надо entrypoint-скрипт. Сейчас лень.
ARG gitlab_port=8080

ENV HOMEDIR=${gitlab_home} \
    gitlab_user=${gitlab_user} \
    gitlab_group=${gitlab_group}

## TODO Поддержка репликации - нужно replicaof <masterip> <masterport> в config.

RUN groupadd -g ${gitlab_gid} ${gitlab_group} && \
  useradd --system -d ${gitlab_home} --no-create-home -u ${gitlab_uid} -g ${gitlab_gid} \
  -s /usr/share/webapps/gitlab-shell/bin/gitlab-shell-ruby ${gitlab_user}

## Создать необходимые для работы директории
RUN for dir in ${gitlab_home} /run/gitlab /var/log/gitlab; do \
      mkdir -p $dir && \
      chown $gitlab_user:${gitlab_group} $dir && \
      chmod 700 $dir; \
    done

## пофиксить права доступа на менее суровые:
RUN chmod 750 ${gitlab_home} && \
    chmod 770 /var/log/gitlab

VOLUME ${gitlab_home}

## redis - внутри этого же контейнера, т.к. слишком мал для отдельного контейнера.
RUN $PACMAN --noconfirm -Sy --needed gitlab

RUN sio-finalize-container.sh

EXPOSE ${gitlab_port}

RUN RESQUE_CONF="/etc/webapps/gitlab/resque.yml" && \
    echo > $RESQUE_CONF && \
    for mode in "development" "test" "production"; do \
      echo "$mode: unix:/run/redis/redis.sock" >> $RESQUE_CONF; \
    done

COPY docker-entrypoint.sh /sbin/docker-entrypoint.sh

## TODO Redis, postgresql, настройка gitlab, запуск
RUN systemctl enable gitlab.target && \
    systemctl enable gitlab-gitaly.service

ENTRYPOINT ["/sbin/docker-entrypoint.sh"]

