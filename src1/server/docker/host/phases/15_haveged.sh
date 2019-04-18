set -e

pacman -S --needed --noconfirm haveged
systemctl daemon-reload
systemctl start haveged
systemctl enable haveged
