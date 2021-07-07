package io.suggest.lk.nodes.form.m

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq


/** Model abstracts different NFC operations, implemented in lk-nodes form. */
object MNfcOperations extends Enum[MNfcOperation] {

  /** Write NDEF records for showcase app/url. */
  case object WriteShowcase extends MNfcOperation

  /** Enforce tag to be read-only. */
  case object MakeReadOnly extends MNfcOperation


  override def values = findValues

}


sealed abstract class MNfcOperation extends EnumEntry

object MNfcOperation {
  @inline implicit def univEq: UnivEq[MNfcOperation] = UnivEq.derive
}
