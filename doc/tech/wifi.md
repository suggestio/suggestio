# Wi-Fi

Suggest.io have support for using Wi-Fi zones as radio-beacons for attaching content to it.
Feature can be used to place content information inside buildings in zero-cost way
(re-using current wi-fi infrastructure).

No Wi-Fi router tweaks needed, publicitly-available MAC-addresses are used for regions identification.

## End-users need Suggest.io mobile app installed
Yes, end-users need installed application on Android/iOS, because Wi-Fi radio-scanning supported only
from operating system API, not from browser.

To help your customers to install Suggest.io mobile application, flash and use [NFC-tags](nfc.md)
with Suggest.io data-records inside.

## Background scanning.
May work. As of july-2021, need to debug/test this.

## Add, manage, etc
Same way, as [bluetooth beacons](bluetooth-beacons.md).

## Limitations
- Only Android mobile app support Wi-Fi scanning since version 4.5.
  iOS and standard web-browsers have no API access to Wi-Fi radio scanning.
