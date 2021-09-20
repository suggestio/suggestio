# Wi-Fi

Suggest.io have support for using Wi-Fi zones as radio-beacons for attaching content to it.
Feature can be used to place content information inside buildings in zero-cost way
(re-using current wi-fi infrastructure).

No Wi-Fi router tweaks needed, because only publicitly-available MAC-addresses are used for regions identification.

## End-users need Suggest.io mobile app installed
Yes, end-users need installed application on Android/iOS, because Wi-Fi radio-scanning supported only
from operating system API, not from browser.

To help your customers to install Suggest.io mobile application, flash and use [NFC-tags](nfc.md)
with Suggest.io data-records inside.

## Background scanning.
May work. As of july-2021, need to debug/test this.

## Add, manage, etc
Use [showcase nodes manager](showcase/nodes.md#registering-radio-beacon) to catch WiFi-signal and register it as node:
![Register WiFi router as node](../images/showcase-nodes-register-wifi.gif)

Nothing different here from [bluetooth beacons](bluetooth-beacons.md) scanning and registration.

## Limitations
- Apple iOS devices Wi-Fi support is limited by **at most one** currently associated Wi-Fi network's router MAC-address (BSSID).
  If user NOT connected to any Wi-Fi network, application always see zero Wi-Fi BSSIDs.
  If you need full iOS support over your area, take a look onto [bluetooth radio-beacons](bluetooth-beacons.md).
- Web-browsers are unsupported. They have zero API access to Wi-Fi scanning.
