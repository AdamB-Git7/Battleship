#!/bin/bash
set -euo pipefail

if ! id battleship >/dev/null 2>&1; then
    sudo useradd --system --no-create-home --shell /sbin/nologin battleship
fi

sudo mkdir -p /opt/battleship
sudo mv /tmp/battleship.jar /opt/battleship/battleship.jar
sudo chown -R battleship:battleship /opt/battleship

sudo mv /tmp/battleship.service /etc/systemd/system/battleship.service
sudo systemctl daemon-reload
sudo systemctl enable --now battleship
sudo systemctl status battleship --no-pager
