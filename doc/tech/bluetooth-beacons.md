# Bluetooth beacons

Suggest.io have transparent support for Bluetooth Low-Energy (BLE) beacons. Users can:
- Register new EddyStone beacons in Suggest.io system.
- Advertise ads in beacons (including paid advertisements and monetization of other advertisements)
- View beacon ads-contents in showcase in same way as other ads.


## Add EddyStone beacon
There are two ways to register a beacon.
Internally, "register a beacon" means to create a node with beacon `_id` and beacon node-type.

### Add via mobile app
0. Ensure your EddyStone-UID enabled.
1. Install & run Suggest.io mobile application
   for [Android](https://play.google.com/store/apps/details?id=io.suggest.appsuggest)
   or [iOS](https://apps.apple.com/ru/app/id1501737715).
2. Open app's left menu => nodes management.
3. Open found beacon from beacons category.
4. Click on add/register button. Type beacon name, select parent node, press "Save" button.

### Add via personal cabinet
1. [Login](https://suggest.io/id) via browser. Go to your node and ads list.
2. On current node, on left panel click open node.
3. On right panel press Nodes button.
4. Choose parent node on the tree, click on "Create..." button.
5. Type beacon UUID, beacon name and choose parent node. Click "Save" button.


## Further steps
Beacons are visible in lk-adv-geo form popups, if parent ADN-node visible on the map.
It can be used for monetization.

Also, choosing ad in list personal cabinet > "Manage" button > Nodes (right panel) can be used
for change yours ad's visiblity on yours beacons.


## Limitations
Nothing serious here, just for information.
- Bluetooth beacon signal detection currently supported in Suggest.io mobile app for Android and iOS.
- [WebBluetooth implementation status](https://github.com/WebBluetoothCG/web-bluetooth/blob/master/implementation-status.md)
  not very happy about readyness of WebBluetooth in common web-browsers for `watchAdvertisements()`
  and/or `Scanning API`. Currently, WebBluetooth support in Suggest.io not implemented, but planned.
- Only EddyStone-UID beacons supported.
- iBeacon support was removed forever.
  - It is impossible to properly collect all iBeacon signals on iOS. iBeacon bluetooth-advertisements are hidden from
    BLE scanning results by iOS. iBeacons interactions implemented inside CoreLocation API with zero bluetooth scanning abilities.
  - iBeacon uses BLE-incompatible frames. So, it is impossible to limit scanning by ServiceUUID to reduce power-consumption on device.
    (e.g. on Android: need to dump & analyze all bluetooth frames - produces heavy load).
    Also, it makes impossible to start bluetooth background scanning (when device sleeps and screen is off).
