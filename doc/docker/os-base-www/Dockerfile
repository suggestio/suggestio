## Базовый образ для образов, работающих с sioweb, в т.ч. jenkins и т.д.

FROM sio/os-base
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

RUN \
  pacman --noconfirm -Sy imagemagick unzip jre8-openjdk-headless wkhtmltopdf xorg-server-xvfb \
  && echo 'xvfb-run -a -s "-screen 0 1024x768x16" wkhtmltoimage $@' > /usr/local/bin/wkhtmltoimage-xvfb \
  && chmod +x /usr/local/bin/wkhtmltoimage-xvfb

