package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:33
  * Description: Модель состояния документа в редакторе.
  */
object MDocS {

  /** Реализация FastEq для инстансов [[MDocS]]. */
  implicit object MDocSFastEq extends FastEq[MDocS] {
    override def eqv(a: MDocS, b: MDocS): Boolean = {
      (a.jdDoc ===* b.jdDoc) &&
      (a.editors ===* b.editors)
    }
  }

  @inline implicit def univEq: UnivEq[MDocS] = UnivEq.derive

  val jdDoc = GenLens[MDocS](_.jdDoc)
  val editors = GenLens[MDocS](_.editors)

}


/** Класс модели состояния работы с документом.
  *
  * @param jdDoc Текущее состояние редактируемого jd-документа и смежных данных.
  */

case class MDocS(
                  jdDoc         : MJdDocEditS,
                  editors       : MEditorsS,
                )
