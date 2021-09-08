package io.suggest.android

import io.suggest.common.html.HtmlConstants

object AndroidConst {

  object Words {

    def apply(words: String*): String =
      words.mkString( HtmlConstants.`.` )

    final def ANDROID = "android"
    final def NFC = "nfc"
    final def INTENT = "intent"
    final def ACTION = "action"

    final def NDEF = "NDEF"
    final def TECH = "TECH"
    final def _DISCOVERED = "_DISCOVERED"

  }

  import Words._


  object Intent {
    object Action {
      final def MAIN = Words( ANDROID, INTENT, ACTION, "MAIN" )
      final def NDEF_DISCOVERED = Words( ANDROID, NFC, ACTION, NDEF + _DISCOVERED )
      final def TECH_DISCOVERED = Words( ANDROID, NFC, ACTION, TECH + _DISCOVERED )
    }
  }


  object Nfc {
    /** NFC Android Application Record: NDEF external type name. */
    final def AAR_EXTERNAL_TYPE = "android.com:pkg"
  }

}
