FROM archimg/base
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

## Для ускорения работы pacman'а, выставить только быстрые зеркала:
RUN : \
  && echo 'Server = http://mirror.yandex.ru/archlinux/$repo/os/$arch' > /etc/pacman.d/mirrorlist \
  && echo 'Server = http://mirror.rol.ru/archlinux/$repo/os/$arch' > /etc/pacman.d/mirrorlist

RUN pacman --noconfirm -Sy imagemagick unzip jre8-openjdk-headless wkhtmltopdf xorg-server-xvfb
## TODO yaourt -S aur/mozjpeg

RUN : \
  ## Install locales
  && echo en_US.UTF-8 UTF-8 > /etc/locale.gen \
  && echo ru_RU.UTF-8 UTF-8 >> /etc/locale.gen \
  && echo LANG=ru_RU.UTF-8 > /etc/locale.conf \
  && locale-gen \
  ## install custom scripts
  && echo 'xvfb-run -a -s "-screen 0 1024x768x16" wkhtmltoimage $@' > /usr/local/bin/wkhtmltoimage-xvfb \
  && chmod +x /usr/local/bin/wkhtmltoimage-xvfb \
  ## remove all useless stuff
  && rm -rf /usr/share/man/* /var/cache/pacman/pkg/* /var/lib/pacman/sync/* /etc/pacman.d/mirrorlist.pacnew /usr/share/licenses/*

#RUN echo 'xvfb-run -a -s "-screen 0 1024x768x16" /usr/bin/wkhtmltopdf $@' > /usr/local/bin/wkhtmltopdf-xvfb
#RUN chmod +x /usr/local/bin/wkhtmltopdf-xvfb



CMD ["/bin/bash"]
