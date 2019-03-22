package io.suggest.id.login.m.reg

import diode.FastEq
import diode.data.Pot
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.03.19 13:28
  * Description: Состояние одного accept-чекбокса и контента этого чекбокса.
  */
object MAcceptCheckBoxS {

  implicit object MAcceptCheckBoxSFastEq extends FastEq[MAcceptCheckBoxS] {
    override def eqv(a: MAcceptCheckBoxS, b: MAcceptCheckBoxS): Boolean = {
      (a.isChecked ==* b.isChecked) &&
      (a.content ===* b.content) &&
      (a.isContentShowing ==* b.isContentShowing)
    }
  }

  @inline implicit def univEq: UnivEq[MAcceptCheckBoxS] = UnivEq.derive


  val isChecked         = GenLens[MAcceptCheckBoxS](_.isChecked)
  val content           = GenLens[MAcceptCheckBoxS](_.content)
  val isContentShowing  = GenLens[MAcceptCheckBoxS](_.isContentShowing)

  def default = apply()

}


case class MAcceptCheckBoxS(
                             isChecked            : Boolean             = false,
                             content              : Pot[None.type]      = Pot.empty,
                             isContentShowing     : Boolean             = false,
                           )
