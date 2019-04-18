#!/bin/bash

## Настройка sshd.

set -e

## TODO scp
cp -f ssh/* /etc/ssh/
#scp ssh/* $REMOTE_USER@$REMOTE_HOST:/etc/ssh

systemctl reload sshd

