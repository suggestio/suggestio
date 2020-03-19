#!/bin/bash

## Начальная установка wireguard для защищённой связи с другими пирами.
## По мотивам https://wiki.archlinux.org/index.php/WireGuard#Using_systemd-networkd

. ./inc/_build.sh

set -e
pacman -S wireguard-tools wireguard-arch wireguard-lts

WG_CONF_PREFIX="/etc/systemd/network/zz-wireguard-server"

umask 077

cat > $WG_CONF_PREFIX.netdev << EOF
[NetDev]
Name = wg0
Kind = wireguard
Description = WireGuard

[WireGuard]
ListenPort = 51820
PrivateKey = $(wg genkey)


EOF


cat > $WG_CONF_PREFIX.network << EOF
[Match]
Name = wg0

[Network]
Address = 10.200.200.1/32

[Route]
Gateway = 10.200.200.1
Destination = 10.200.200.0/24

EOF


systemctl daemon-reload


echo "** Wireguard installed with dummy configuration." >&2
echo "** Please, initialize wireguard peers and ip in $WG_CONF_PREFIX* conf.files." >&2
echo "** After, run # systemctl restart systemd-networkd" >&2

## Не запускаем автоматом, т.к. эта пуская конфигурация бесполезна по факту: ip с потолка, пиров нет.
## TODO Реализовать авторазвёртывание ключей пиров и определение wg ip.
#systemctl restart systemd-networkd

