package io.suggest.lk.nodes.form.m

import japgolly.univeq.UnivEq

object MNfcDiaS {

  @inline implicit def univEq: UnivEq[MNfcDiaS] = UnivEq.derive

}


/** Model of NFC dialog state. */
case class MNfcDiaS(
                   )
