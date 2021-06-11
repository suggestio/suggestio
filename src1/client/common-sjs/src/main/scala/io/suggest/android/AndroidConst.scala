package io.suggest.android

object AndroidConst {

  final def ANDROID_ = "android."

  object Intent {
    object Action {
      final def NFC_ACTION_ = "nfc.action."
      final def NFC_NDEF_DISCOVERED = ANDROID_ + NFC_ACTION_ + "NDEF_DISCOVERED"
    }
  }

}
