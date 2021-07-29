# Showcase nodes manager

On menu bar (left) nodes management button opens a window, containing several features
- Created, edit, delete user's nodes (if logged-in).
- Scan for radio signals (Wi-Fi, Bluetooth beacons, etc possibly).
- Register and manage radio-signal sources as own nodes (by Wi-Fi MAC-address, by Bluetooth beacon UID, etc).
- Write NFC tags.
- Publish/unpublish currently-opened ad on user's nodes.

## Registering radio-beacon
Wi-Fi or Bluetooth beacon signals can be attached to current user's account via "nodes management" inside showcase left menu.
Showcase have radio-scanner integrated, and usable radio-signals is visible inside nodes tree.

**Note**: You need application installed, because Wi-Fi networks scanning and bluetooth advertisements 
scanning NOT available inside standard web-browser.

Let's look, how to register any Wi-Fi router as your own node:

![Register some Wi-Fi router as node](../../images/showcase-nodes-register-wifi.gif)

Now, it is possible to use created WiFi-node as point of advertising and monetization:
- Owner (user) can attach content to WiFi-signal covering zone for free.
- Other Suggest.io users can place PAID advertisements and tags on registered Wi-Fi-signal zone using
  [lk-adv-geo form](../cabinet/adv-geo.md).
- Suggest.io end-users will see all WiFi-placed content and tags on the Suggest.io application screen,
  when device's antenna feels registered WiFi-router signal
  (no active Wi-Fi connection needed, only enabled wifi-antenna enought).
- Also, when Suggest.io app installed on end-user's device, and device screen is off, background scanning
  may produce a notification when new published content placed nearby inside WiFi/Bluetooth radio-signals.

Bluetooth-beacons are registered in very same way.

## Source code
[LkNodes form components](../../../src1/client/lk/nodes/form) are shared between showcase and personal cabinet form.
The main difference between two: active radio-scanner inside showcase (mobile application only). Server-side
HTTP-controller is [here](../../../src1/server/www/app/controllers/LkNodes.scala).
