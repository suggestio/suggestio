## Базовый образ для образов, работающих с sioweb, в т.ч. jenkins, gitlab-ci и т.д.

FROM docker-registry.suggest.io/sio/sio2/os-base-jre
LABEL maintainer="konstantin.nikiforov@cbca.ru"

## Установить всю необходимую утиль
RUN $PACMAN --noconfirm -Sy imagemagick unzip wkhtmltopdf xorg-server-xvfb \
  && echo 'xvfb-run -a -s "-screen 0 1024x768x16" wkhtmltoimage $@' > /usr/local/bin/wkhtmltoimage-xvfb \
  && chmod +x /usr/local/bin/wkhtmltoimage-xvfb

