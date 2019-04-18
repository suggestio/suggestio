#!/bin/bash

pacman -Sy --needed vim mc

echo ru_RU.UTF-8 UTF-8 >> /etc/locale.gen
locale-gen

timedatectl set-timezone Europe/Moscow

systemctl enable systemd-timesyncd.service
systemctl start systemd-timesyncd.service

echo > /etc/vconsole.conf << EOF
KEYMAP=ru-ms
FONT=Cyr_a8x16
EOF

echo 'export EDITOR=vim' > /etc/profile.d/editor.sh

