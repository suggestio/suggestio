#!/bin/bash

## Запуск фазы на исполнение на удалённом хосте.
## $1 - хостнейм (обязательно).
## $2 $3 ... - имена скриптов на исполнение: 10_init.sh 20_do_something.sh ...
## (необязательно - все скрипты , по умолчанию).

set -e

test $1 || {
    echo "Example: $0 root@localhost"
    exit 1
}

#scp -r ./phases $1:sio2kubHost/

ssh $1 'for file in $(find ~/sio2kubHost/phases/ -type f -name "*_*.sh"); do bash $file; done;'

