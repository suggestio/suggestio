
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
FROM docker-registry.suggest.io/sio/os-base
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Установить всю необходимую утиль:
## И пусть redis будет локальным, чтобы общение шло через redis.sock.
## postgresql - использовать тот же, что и для биллинга.
RUN $PACMAN --noconfirm -Sy --needed gitlab

## TODO Redis, postgresql, настройка gitlab, запуск

