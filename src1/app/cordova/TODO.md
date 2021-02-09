== HTML navigator.geolocation ==

navigator.geolocaiton - пробросить в background-geolocation.

Разобраться, кто к этому обращается, и что делать.
Явно, кто-то обращается: удаление плагина cordova-plugin-geolocation приводит
к "оголению" HTML Geolocation API, и iOS начинает спрашивать доступ webview на геолокацию
на каждый чих.

Фреймы внутри webview, теоретически, тоже могут дёргать эту геолокацию, без разрешения.
Поэтому нужно по умному снести плагин cordova-plugin-geolocation, голое API заблочить,
все обращения выдачу к HTML Geolocation API завернуть напрямую в background-geolocation.

https://stackoverflow.com/a/40891337
> If cordova.js file is missing, the navigator.geolocation will call the
> HTML5 object which is browser based and hence the weird alert.
