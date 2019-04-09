FROM archlinux/base
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

RUN : \
  ## Install locales
  && echo en_US.UTF-8 UTF-8 > /etc/locale.gen \
  && echo ru_RU.UTF-8 UTF-8 >> /etc/locale.gen \
  && echo LANG=ru_RU.UTF-8 > /etc/locale.conf \
  && locale-gen \
  ## Для ускорения работы pacman'а, выставить только быстрые зеркала:
  && echo 'Server = http://mirror.yandex.ru/archlinux/$repo/os/$arch' > /etc/pacman.d/mirrorlist

## Скачать индексы репозиториев для установки пакетов на последующих шагах.
RUN pacman --noconfirm -Sy
