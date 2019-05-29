#!/bin/bash

## Скрипт развёртывания sio2 на виртуалках sio2/3/5 (система до контейнеров).
## Обновление идёт по rsync+ssh, поэтому нужно заранее прописать ssh-ключи на нодах.
##
## $1 - локальна директория www
## $2 - удалённая директория www
## $3... - список нод для обновления: sio2.suggest.io sio3.suggest.io sio5.suggest.io

set -e

subdir=$1
rmDir=$2
echo -e " MyDir = $(pwd)\n Subdir from = $subdir\n remote dir to = $rmDir\n"
shift
shift
nodes=$@
echo "All nodes are = $nodes"

for srv in $nodes; do
  echo "Rsyncing to $srv/$rmDir... pwd=$PWD"
  rsync -avz -e ssh $subdir/target/universal/stage/* www@$srv:$rmDir/ && {
    fname="$rmDir/RUNNING_PID"
    ssh www@$srv << EOF
if test -f $fname; then
  pid=\$(cat $fname)
  echo "Old play instance is up and running. Killing pid \$pid for supervisor..."
  kill -SIGINT \$pid
  rm -f $fname
else
  echo "Looks like play is already down. No pidfile found at $fname. Skipping."
fi
EOF
  }
done

echo "Deploy+reload done."
