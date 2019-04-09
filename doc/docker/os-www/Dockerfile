FROM sio/os-base-www
MAINTAINER Konstantin Nikiforov konstantin.nikiforov@cbca.ru

RUN : \
  ## remove all useless stuff
  pacman -Rns --noconfirm $(pacman -Qtdq) \
  && rm -rf /usr/share/man /var/cache/pacman/pkg/* /var/lib/pacman/sync/* /etc/pacman.d/mirrorlist.pacnew /usr/share/licenses /usr/share/doc \
  && mkdir -p /usr/share/man \
  && mkdir -p /usr/share/licenses \
  && mkdir -p /usr/share/doc

CMD ["/bin/bash"]
