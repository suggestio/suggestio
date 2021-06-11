package io.suggest.cordova.intent

object CdvIntentParseUtil {
  /** NDEF_DISCOVERED intent example data:
    * {
    *   "extras": {
    *      "android.nfc.extra.NDEF_MESSAGES": [
    *        "NdefMessage [NdefRecord tnf=1 type=55 payload=04737567676573742E696F2F, NdefRecord tnf=4 type=616E64726F69642E636F6D3A706B67 payload=696F2E737567676573742E61707073756767657374]"
    *      ],
    *      "android.nfc.extra.ID": ["4","-96","-11","10","70","112","-128"],
    *      "android.nfc.extra.TAG": "TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.Ndef]"
    *    },
    *    "action": "android.nfc.action.NDEF_DISCOVERED",
    *    "flags": 272629760,
    *    "component": "ComponentInfo{io.suggest.appsuggest/io.suggest.appsuggest.MainActivity}",
    *    "data":"https://suggest.io/",
    *    "package":"io.suggest.appsuggest"
    *  }
    */

}
