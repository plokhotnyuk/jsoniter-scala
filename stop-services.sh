#!/bin/bash
sudo systemctl stop proc-sys-fs-binfmt_misc.automount
sudo systemctl stop bluetooth.service
sudo systemctl stop cron.service
sudo systemctl stop cups.path
sudo systemctl stop cups.socket
sudo systemctl stop cups.service
sudo systemctl stop colord.service
sudo systemctl stop cups-browsed.service
sudo systemctl stop fwupd.service
sudo systemctl stop gnome-remote-desktop.service
sudo systemctl stop iio-sensor-proxy.service
sudo systemctl stop ModemManager.service
sudo systemctl stop packagekit.service
sudo systemctl stop power-profiles-daemon.service
sudo systemctl stop syslog.socket
sudo systemctl stop rsyslog.service
sudo systemctl stop snapd.service
sudo systemctl stop snapd.socket
sudo systemctl stop systemd-journald.service
sudo systemctl stop systemd-journald.socket
sudo systemctl stop systemd-journald-dev-log.socket
sudo systemctl stop systemd-oomd.service
sudo systemctl stop systemd-oomd.socket
sudo systemctl stop systemd-hostnamed.service
sudo systemctl stop systemd-resolved.service
sudo systemctl stop systemd-timesyncd.service
sudo systemctl stop switcheroo-control.service
sudo systemctl stop thermald.service
sudo systemctl stop udisks2.service
sudo systemctl stop unattended-upgrades.service
sudo systemctl stop upower.service
sudo service apparmor stop
sudo service apport stop
sudo service bluetooth stop
sudo service openvpn stop
sudo service plymouth-log stop
sudo service ufw stop
sudo service sysstat stop
