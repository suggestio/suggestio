#!/bin/bash

## Запускалка postgresql с поддержкой репликации.
##
## Запилена по мотивам:
## - https://github.com/DanielDent/docker-postgres-replication/blob/master/docker-entrypoint.sh
## - https://github.com/DanielDent/docker-postgres-replication/blob/master/setup-replication.sh
##
## Если !CMD или $1 == postgres, то обычный запуск постгреса (systemctl) с возможной инициализацией базы.
## Иначе - простой проброс команд в консоль.
##
## env REPLICATE_FROM - наличие хостнейма мастера определяет реплику.
## Если REPLICATE_FROM отсутствует, то запуск мастера.
##

set -e

# Forwards-compatibility for old variable names (pg_basebackup uses them)
if [ "x$PGPASSWORD" = "x" ]; then
    export PGPASSWORD=$POSTGRES_PASSWORD
fi

if [ "$1" = 'postgres' ]; then

  ## Дефолтовый запуск постгреса. Запустить initdb, если требуется.
  if [ ! -s "$PGDATA/PG_VERSION" ]; then

      ## 10. Инициализировать начальную БД.
      if [ "x$REPLICATE_FROM" == "x" ]; then
	echo "Initializing MASTER..."

        ## Нет БД на мастере. Инициализировать новую БД...
        sudo -u postgres initdb --locale ru_RU.UTF-8 -D $PGDATA

        ## 20. Сгенерить базовый конфиг pg-мастера
        # check password first so we can output the warning before postgres
        # messes it up
	if [ "$POSTGRES_PASSWORD" ]; then
            pass="PASSWORD '$POSTGRES_PASSWORD'"
            authMethod=md5
        else
	    # The - option suppresses leading tabs but *not* spaces. :)
	    cat >&2 <<-'EOWARN'
		****************************************************
		WARNING: No password has been set for the database.
		This will allow anyone with access to the
		Postgres port to access your database. In
		Docker's default configuration, this is
		effectively any other container on the same
		system.
		Use "-e POSTGRES_PASSWORD=password" to set
		it in "docker run".
		****************************************************
		EOWARN
	    pass=
	    authMethod=trust
	fi

	sudo -u postgres touch "$PGDATA/pg_hba.conf"
	cat >> "$PGDATA/pg_hba.conf" <<-EOHBA
		host replication all 0.0.0.0/0 $authMethod
		host all all 0.0.0.0/0 $authMethod
		EOHBA

	## Если переменные не заданы, то инициализировать дефолтовыми значениями.
	: ${POSTGRES_DB:=$POSTGRES_USER}
	export POSTGRES_USER POSTGRES_DB

	psql=( psql -v ON_ERROR_STOP=1 )

	if [ "$POSTGRES_DB" != 'postgres' ]; then
	  "${psql[@]}" --username postgres <<-EOSQL
		CREATE DATABASE "$POSTGRES_DB" ;
		EOSQL
	  echo
	fi

	if [ "$POSTGRES_USER" = 'postgres' ]; then
	  op='ALTER'
	else
	  op='CREATE'
	fi

	echo "Will init pg-user $POSTGRES_USER..."
	## Ограниченно временно запустить мастер-сервер, чтобы залить туда имя-пароль юзера
	sudo -u postgres pg_ctl -D "$PGDATA" -o "-c listen_addresses='localhost'" -w start

	"${psql[@]}" --username postgres <<-EOSQL
		$op USER "$POSTGRES_USER" WITH SUPERUSER $pass ;
		EOSQL

	sudo -u postgres pg_ctl -D "$PGDATA" -m fast -w stop
	echo "pg-user $POSTGRES_USER done."

      else
	echo "Initializing SLAVE..."
        ## Это реплика - она должна дождаться доступности мастер-сервера и скачать сбэкапить с него начальную БД.
	## По мотивам https://github.com/DanielDent/docker-postgres-replication/blob/master/docker-entrypoint.sh#L36
	until ping -c 1 -W 1 ${REPLICATE_FROM}
	do
	    echo "Waiting for master to ping..."
	    sleep 1s
	done
	until sudo -u postgres pg_basebackup -h ${REPLICATE_FROM} -D ${PGDATA} -U ${POSTGRES_USER} -v -P -w
	do
	    echo "Waiting master for initial recovery..."
	    sleep 1s
	done
	
      fi

  else if [ -f ${PGDATA}/postmaster.pid ]; then
      rm -f "${PGDATA}/postmaster.pid"
  fi
  fi

  
  if [ "x$REPLICATE_FROM" == "x" ]; then
    ## Закачать данные мастера в recovery.conf.
    : ${PG_MAX_WAL_SENDERS:=8}
    : ${PG_WAL_KEEP_SEGMENTS:=8}
    cat > ${PGDATA}/postgresql.conf <<-EOF
	listen_addresses = '*'
	wal_level = hot_standby
	max_wal_senders = $PG_MAX_WAL_SENDERS
	wal_keep_segments = $PG_WAL_KEEP_SEGMENTS
	hot_standby = on
	EOF

  else

    ## Инициализировать recovery.conf реплики.
    cat > ${PGDATA}/recovery.conf <<-EOF
	standby_mode = on
	primary_conninfo = 'host=${REPLICATE_FROM} port=5432 user=${POSTGRES_USER} password=${POSTGRES_PASSWORD}'
	trigger_file = '/tmp/touch_me_to_promote_to_me_master'
	EOF
	
    chown postgres ${PGDATA}/recovery.conf
    chmod 600 ${PGDATA}/recovery.conf
  fi
  
  exec systemctl default

else
  exec $@
fi

