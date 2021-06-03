package cordova.plugins.nfc

import scala.scalajs.js

object NfcEvent {

  type Type_t <: String

  object Types {
    final def TAG = "tag".asInstanceOf[Type_t]
    final def NDEF_MIME = "ndef-mime".asInstanceOf[Type_t]
    final def NDEF = "ndef".asInstanceOf[Type_t]
    final def NDEF_FORMATABLE = "ndef-formatable".asInstanceOf[Type_t]
  }

}


trait NfcEvent extends js.Object {
  val `type`: NfcEvent.Type_t
  val tag: NfcEventTag
}

trait NfcEventTag extends js.Object {

  val ndefMessage: js.UndefOr[NdefMessage_t] = js.undefined

  // Android:
  val isWritable: js.UndefOr[Boolean] = js.undefined
  val id: js.UndefOr[ByteArray_t] = js.undefined
  val techTypes: js.UndefOr[js.Array[String]] = js.undefined
  val `type`: js.UndefOr[String] = js.undefined
  val canMakeReadOnly: js.UndefOr[Boolean] = js.undefined
  val maxSize: js.UndefOr[Int] = js.undefined

  // BlackBerry:
  val tagType: js.UndefOr[String] = js.undefined
  val isLocked: js.UndefOr[Boolean] = js.undefined
  val isLockable: js.UndefOr[Boolean] = js.undefined
  val freeSpaceSize: js.UndefOr[String] = js.undefined
  val serialNumberLength: js.UndefOr[String] = js.undefined
  val serialNumber: js.UndefOr[ByteArray_t] = js.undefined
  val name: js.UndefOr[String] = js.undefined

}