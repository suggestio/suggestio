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

  }

  import Words._


  object Intent {
    object Action {
      final def MAIN = Words( ANDROID, INTENT, ACTION, "MAIN" )
      final def NDEF_DISCOVERED = Words( ANDROID, NFC, ACTION, "NDEF_DISCOVERED" )
    }
  }


  object Nfc {
    /** NFC Android Application Record: NDEF external type name. */
    final def AAR_EXTERNAL_TYPE = "android.com:pkg"
  }

}
