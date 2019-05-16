#!/bin/bash

## Запускалка для redis на основе переменных окружения.
## - PG_HOST, PG_DATABASE, PG_PORT, PG_USERNAME, PG_PASSWORD
##   Реквизиты связи с СУБД PostgreSQL.
##
## Если ошибка 500 в project/settings/ci_cd:
## su - gitlab -s /bin/sh -c '/usr/share/webapps/gitlab/bin/rails dbconsole'
## UPDATE Projects SET runners_token_encrypted = NULL WHERE Name = 'sio2';
## По мотивам https://forum.gitlab.com/t/project-ci-cd-settings-broken/25131/1

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

## Организовать хостнейм для gitlab в конфиге:
if [ "x$GITLAB_HOST" = "x" ]; then
    GITLAB_HOST="source.suggest.io"
fi
## Пробросить trusted proxies:
if [ "x$GITLAB_TRUSTED_PROXIES" = "x" ]; then
  GITLAB_TRUSTED_PROXIES="10.0.0.0/8"
fi
## Если наружу торчит не-22-порт для ssh, то надо об этом сообщить.
if [ "x$GITLAB_SSH_PORT" = "x" ]; then
  ## Пока сразу выбрасываем наружу tcp:222, на будущее надо перейти на дефолтовый порт.
  GITLAB_SSH_PORT=222
fi

_mkEnvConfigs() {
  ## Сгенерить конфиги на основе env.
  echo > /etc/webapps/gitlab/database.yml
  for mode in production development test; do
    cat >> /etc/webapps/gitlab/database.yml <<EOF
$mode:
  adapter: postgresql
  encoding: unicode
  host: $PG_HOST
  port: $PG_PORT
  database: $PG_DATABASE
  pool: 10
  username: $PG_USERNAME
  password: "$PG_PASSWORD"

EOF
  done

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

  HOST_RE="\s*host:\s*"
  sed -r "0,/$HOST_RE.*/s/^($HOST_RE).*/\1${GITLAB_HOST}/" -i /etc/webapps/gitlab/gitlab.yml

  TRUSTED_PROXIES_RE="trusted_proxies:"
  sed -r "0,/^\s*$TRUSTED_PROXIES_RE/s/^(\s*)($TRUSTED_PROXIES_RE)/\1\2\n\1- $GITLAB_TRUSTED_PROXIES" -i /etc/webapps/gitlab/gitlab.yml

  SSH_PORT_RE="ssh_port:"
  sed -r "0,/$SSH_PORT_RE/s/(\s*)(#\s*)?($SSH_PORT_RE).*/\1\3 $GITLAB_SSH_PORT/" -i /etc/webapps/gitlab/gitlab.yml
}


## TODO Проброс параметров следует позаимствовать из https://github.com/sameersbn/docker-gitlab assets/runtime/functions
## Там происходит подстановка множества переменных окружения в конфиги по шаблонам.

## Общий код вызова rake target.
_rakeCmd() {
  su - gitlab -s /bin/sh -c "cd '/usr/share/webapps/gitlab'; bundle-2.5 exec rake $1 RAILS_ENV=production" 
  return $?
}

_initDb() {
  _rakeCmd "gitlab:setup DISABLE_DATABASE_ENVIRONMENT_CHECK=$1"
}


_fatal() {
  echo "FATAL: $1" >&2
  sleep 300
  exit 1
}

## Для инициализации требуется stdin, ввод ответа (ответов) на вопросы, и осознанное решение. 
## Поэтому инициализация - вручную.
_doInit() {
  echo "${gitlab_home}/ appears empty. First vol.mount. Re-initialize dir.content..."
  tar -C "${gitlab_home}" -xvpf "${gitlab_initial_backup_tar_gz}" || _fatal "Cannot unpack ${gitlab_initial_backup_tar_gz} into ${gitlab_home} ! Waiting for admin from ssh..."

  _mkEnvConfigs

  ## Развернуть схему БД:
  echo "1. starting redis..."
  systemctl start redis.service || _fatal "CANNOT START redis. WAITING ssh FROM ADMIN..."

  echo "2. starting gitaly..."
  systemctl start gitlab-gitaly.service || echo "Hope, gitaly really started. Currently, it is unclear for systemctl." # _fatal "CANNOT START gitlab-gitaly. WAITING ssh FROM ADMIN..."

  echo "3. will init db..."
  _initDb
}


## Разобраться с ssh-ключами:
_beforeStart() {
  SSH_VAR_D="/var/lib/gitlab/ssh"
  if [ -z "$(ls -A ${SSH_VAR_D})" ]; then
    echo "generating new ssh keys..."
    ssh-keygen -A
    ## Скопировать ssh-ключи сервера в /var/lib/gitlab/ssh
    mkdir -p $SSH_VAR_D
    chown root:root $SSH_VAR_D
    chmod 700 $SSH_VAR_D
    cp /etc/ssh/ssh_host_*_key* $SSH_VAR_D/
  else
    ## Восстановить предыдущие ssh-ключи сервера из /var/lib/gitlab/ssh в /etc/ssh
    echo "restoring ssh keys..."
    cp $SSH_VAR_D/* /etc/ssh/
  fi
  ## redis уже сконфигурирован в докере.

  ## Нужно запилить секретные seed'ы, которые должны выживать между обновлениями контейнера.
  for secretFile in "gitlab/secret" "gitlab-shell/secret"; do
    tgSecretFilePath="/etc/webapps/$secretFile"
    if [ ! -s "$tgSecretFilePath" ]; then
      ## Ещё нет файла - первый запуск.
      mkdir -p `dirname "$tgSecretFilePath"`
      ## Если обычный (не-yaml) файл, то создать его, заполнив рандомной строкой данных:
      hexdump -v -n 64 -e '1/1 "%02x"' /dev/urandom > "$tgSecretFilePath"
      chmod 640 "$tgSecretFilePath"
      chown root:${gitlab_group} "$tgSecretFilePath"
    fi
  done
}


## Обработка $1 - в зависимости от значения аргумента (или отсутствия значения) произвести действие.
if [ "x$1" = "x" ]; then
  ## Если /var/lib/gitlab пустая, то это первое монтирование. Надо закинуть туда содержимое $gitlab_initial_backup_tar_gz.
  if [ -z "$(ls -A ${gitlab_home})" ]; then
    _fatal "Login here via ssh and use '$0 init' to initialize all, OR '$0 initdb' for only DB init."
  else
    ## Стандартный запуск:
    _beforeStart
    _mkEnvConfigs
    ## redis запускаем с опережением, иначе gitlab не может запуститься из-за race condition.
    systemctl start redis.service
    exec systemctl --init default
  fi

elif [ "x$1" = "xmigrate" ]; then
  echo "Upgrading db..."
  _mkEnvConfigs
  _rakeCmd "db:migrate"

elif [ "x$1" = "xcheck" ]; then
  echo "Check..."
  _mkEnvConfigs
  _rakeCmd "gitlab:check"

elif [ "x$1" = "xinfo" ]; then
  echo "Info..."
  _mkEnvConfigs
  _rakeCmd "gitlab:env:info"
  return $?

elif [ "x$1" = "xinit" ]; then
  echo "Full init..."
  _doInit
  
elif [ "x$1" = "xinitdb" ]; then
  echo "Init DB..."
  _mkEnvConfigs
  _initDb $2

else
  ## Исполнение произвольных команд:
  exec $@

fi

