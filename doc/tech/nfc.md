# NFC

Suggest.io mobile applications and standard-compilant browsers (see [Limitations](#limitations)) supports NFC interaction.

Feature can be used as very cheap solution for end-users, and does not needs pre-installed Suggest.io application
on end-user devices.

NFC-tags does NOT need any power-supply, have NO battery inside.

## How it works?
0. Modern mobile devices scans NFC-tags in background, when screen is ON and unlocked.
1. NFC tag owner writes URL and Android Application Record (AAR) into tag. Tag is placed somewhere.
2. User reads NFC tag using its smartphone.
3. On the device screen, Suggest.io app will be opened automatically.
   Or application install offering (Google Play page).
   Or suggest.io site in default web-browser.
4. User see node showcase or/and custom content (encoded into URI inside NFC-tag) inside app/browser.

## How to write proper URL (records) into NFC tag?
1. Install app to your smartphone.
2. Login, open "Nodes management" on menu bar. 
3. Choose one of your node in nodes tree (displayed if logged in and have any node in account).
4. Press NFC button on node toolbar.
5. Press "WRITE NFC-TAG" button.
6. Bring your device to tag, so NFC-tag will be detected, and data will be written.
7. (Optional) You may also protect NFC-tag from further writes using "MAKE READ-ONLY" button.
   Note that, this action cannot be undone.

You may open some ad in background, and ad id will be encoded and written into NFC-tag.

## Limitations
- Making NFC-tag read-only supported on Android, not supported on iOS.
- Writing NFC-tag from Web-browser is implemented, but low-priority option and needs testing/debugging.
  As of 2021 year, only Chrome browser on Android implements WebNFC standard.
  See related [WebNFC MDN page](https://developer.mozilla.org/en-US/docs/Web/API/Web_NFC_API) and compatibility table.
