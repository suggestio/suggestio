#!/bin/bash

## Настройка sshd.

set -e

cp -f ./etc/ssh/* /etc/ssh/

## Не убрираем старые dsa-rsa-ecdsa-ключи из /etc/ssh - или надо отрубить sshgenkeys. Потом.

systemctl reload sshd

