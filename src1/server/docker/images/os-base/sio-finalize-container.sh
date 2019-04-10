#!/bin/bash

## Это скрипт финализации контейнеров, т.е. для зачистки от всего ненужного.
## На промежуточных этапах это всё удалять может быть преждевременно или нежелательно,
## Зато, можно дёрнуть скрипт при сборке конечного контейнера, и будет красота.

set -e

echo "*** Will finalize container... ***"

$PACMAN --noconfirm -Rns $(pacman -Qtdq)

rm -rf /usr/share/man /var/cache/pacman/pkg/* /var/lib/pacman/sync/* /etc/pacman.d/mirrorlist.pacnew /usr/share/licenses /usr/share/doc
mkdir -p /usr/share/man
mkdir -p /usr/share/licenses
mkdir -p /usr/share/doc

echo "*** Finalize container done ***"

