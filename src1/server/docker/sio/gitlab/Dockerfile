
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
ARG gitlab_uid=105
ARG gitlab_gid=${gitlab_uid}
## TODO Вписывать порт в redis.conf, но для этого надо entrypoint-скрипт. Сейчас лень.
ARG gitlab_port=8080

ENV gitlab_home=${gitlab_home} \
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

RUN $PACMAN --noconfirm -Sy --needed gitlab postgresql-libs tar

## Для распаковки /var/lib/gitlab из gitlab_initial_backup_tar_gz требуется, чтобы tar был в контейнере.
RUN pacman -D --asexplicit tar

ENV gitlab_initial_backup_tar_gz=/var/lib/gitlab-initial.tar.gz

## Надо эвакуировать содержимое gitlab_home, т.к. директории там создаются прямо из пакета.
## Содержимое будет распаковано в entrypoint, если директория ${gitlab_home} пустая.
RUN cd ${gitlab_home} && tar -czpf ${gitlab_initial_backup_tar_gz} .

## зависит от libaudit, нужно вызывать ДО sio-finalize-container.sh
RUN gpasswd -a ${gitlab_user} ${redis_group}

## Система должна раздавать asset'ы. GitLab считает, что это должен делать nginx, а у нас будет без таких оптимизаций, по-старинке:
RUN sed -r "s/(config.public_file_server.enabled *= *).*/\1true/" -i /usr/share/webapps/gitlab/config/environments/production.rb

RUN sio-finalize-container.sh

VOLUME ${gitlab_home}

EXPOSE ${gitlab_port}

RUN RESQUE_CONF="/etc/webapps/gitlab/resque.yml" && \
    echo > $RESQUE_CONF && \
    for mode in "development" "test" "production"; do \
      echo "$mode: unix:/run/redis/redis.sock" >> $RESQUE_CONF; \
    done

## Выставить, чтобы соединения принимались с любого интерфейса, а не только с локалхоста:
RUN sed -r "s/^(listen\s+\")([0-9]+[.:]?){5}\"/\10.0.0.0:${gitlab_port}\"/" -i /etc/webapps/gitlab/unicorn.rb

COPY docker-entrypoint.sh /sbin/docker-entrypoint.sh

## TODO Redis, postgresql, настройка gitlab, запуск
RUN systemctl enable gitlab-gitaly.service && \
    systemctl enable gitlab-unicorn.service && \
    systemctl enable gitlab-workhorse.service && \
    systemctl enable gitlab-sidekiq.service

ENTRYPOINT ["/sbin/docker-entrypoint.sh"]

COPY smtp_settings.rb /usr/share/webapps/gitlab/config/initializers/smtp_settings.rb
