#!/bin/bash

## Запускалка для redis на основе переменных окружения.
## - PG_HOST, PG_DATABASE, PG_PORT, PG_USERNAME, PG_PASSWORD
##   Реквизиты связи с СУБД PostgreSQL.

set -e


## Инициализировать переменные окружения с postgres:
if [ "x$PG_HOST" = "x" ]; then
  echo "PG_HOST undefined." >&2
  exit 1
fi

if [ "x$PG_DATABASE" = "x" ]; then
  PG_DATABASE="gitlab"
fi

if [ "x$PG_PORT" = "x" ]; then
  PG_PORT="5432"
fi

if [ "x$PG_USERNAME" = "x" ]; then
  PG_USERNAME="gitlab"
fi

if [ "x$PG_PASSWORD" = "x" ]; then
  PG_PASSWORD=$PG_USERNAME
fi

## Сгенерить конфиги на основе env.
cat > /etc/webapps/gitlab/database.yml <<EOF
production:
  adapter: postgresql
  encoding: unicode
  host: $PG_HOST
  port: $PG_PORT
  database: $PG_DATABASE
  pool: 10
  username: $PG_USERNAME
  password: "$PG_PASSWORD"
EOF

cat >> /etc/webapps/gitlab-shell/config.yml << EOF
# Redis settings used for pushing commit notices to gitlab 
redis:
  bin: /usr/bin/redis-cli
  host: 127.0.0.1
  port: 6379
  # pass: redispass # Allows you to specify the password for Redis
  database: 5 # Use different database, default up to 16
  socket: /run/redis/redis.sock # uncomment this line
  namespace: resque:gitlab
EOF

## Если /var/lib/gitlab пустая, то это первое монтирование. Надо закинуть туда содержимое $gitlab_initial_backup_tar_gz.
if [ -z "$(ls -A ${gitlab_home})" ]; then
  echo "${gitlab_home}/ appears empty. First vol.mount. Re-initialize dir.content..."
  tar -C "${gitlab_home}" -xvpf "${gitlab_initial_backup_tar_gz}"
fi

## redis уже сконфигурирован в докере.

## Нужно запилить секретные seed'ы, которые должны выживать между обновлениями контейнера.
for secretFile in "gitlab/secret" "gitlab-shell/secret"; do
    volSecretFilePath="${gitlab_home}/_secrets/$secretFile"
    tgSecretFilePath="/etc/webapps/$secretFile"
    if [ ! -s volSecretFilePath ]; then
      ## Ещё нет файла - первый запуск.
      mkdir -p `dirname "$volSecretFilePath"`
      hexdump -v -n 64 -e '1/1 "%02x"' /dev/urandom > "$volSecretFilePath"
      chmod 600 "$volSecretFilePath"
      chown root:root "$volSecretFilePath"
    fi
    ## Уже есть этот файл с прошлых запусков. Скопипастить в нужное место, выставив права.
    cp "$volSecretFilePath" "$tgSecretFilePath"
    chown root:${gitlab_group} "$tgSecretFilePath"
    chmod 640 "$tgSecretFilePath"
done


_initDb() {
  echo "Initializing new DB..."
  su - gitlab -s /bin/sh -c "cd '/usr/share/webapps/gitlab'; bundle-2.5 exec rake gitlab:setup RAILS_ENV=production"
  return $?
}

_upgradeDb() {
  echo "Upgrading db..."
  su - gitlab -s /bin/sh -c "cd '/usr/share/webapps/gitlab'; bundle-2.5 exec rake db:migrate RAILS_ENV=production"
  return $?
}

_printEnv() {
  su - gitlab -s /bin/sh -c "cd '/usr/share/webapps/gitlab'; bundle-2.5 exec rake gitlab:check RAILS_ENV=production"
}

_check() {
  su - gitlab -s /bin/sh -c "cd '/usr/share/webapps/gitlab'; bundle-2.5 exec rake gitlab:env:info RAILS_ENV=production"
  return $?
}


if [ "x$1" = "x" ]; then
  systemctl start redis.service
  ## Стандартный запуск. Попытаться обновить БД, и запустить.
  _upgradeDb
  _printEnv
  exec systemctl --init default

else
  ## Исполнение произвольных команд:
  exec $@

fi

