package io.suggest.lk.nodes.form.m

import diode.data.Pot
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

object MNfcDiaS {

  @inline implicit def univEq: UnivEq[MNfcDiaS] = UnivEq.force

  def writing = GenLens[MNfcDiaS](_.writing)
  def cancelF = GenLens[MNfcDiaS](_.cancelF)

}


/** Model of NFC dialog state. */
case class MNfcDiaS(
                     writing        : Pot[_]            = Pot.empty,
                     cancelF        : Option[() => _]   = None,
                   )
