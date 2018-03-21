FROM sio/os-base
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

RUN pacman --noconfirm -Sy imagemagick unzip jre8-openjdk-headless wkhtmltopdf xorg-server-xvfb
## TODO yaourt -S aur/mozjpeg

RUN : \
  ## install custom scripts
  && echo 'xvfb-run -a -s "-screen 0 1024x768x16" wkhtmltoimage $@' > /usr/local/bin/wkhtmltoimage-xvfb \
  && chmod +x /usr/local/bin/wkhtmltoimage-xvfb \
  ## remove all useless stuff
  && rm -rf /usr/share/man/* /var/cache/pacman/pkg/* /var/lib/pacman/sync/* /etc/pacman.d/mirrorlist.pacnew /usr/share/licenses/*

## TODO rm -rf /usr/share/doc/* - безопасно ли?

#RUN echo 'xvfb-run -a -s "-screen 0 1024x768x16" /usr/bin/wkhtmltopdf $@' > /usr/local/bin/wkhtmltopdf-xvfb
#RUN chmod +x /usr/local/bin/wkhtmltopdf-xvfb

CMD ["/bin/bash"]
