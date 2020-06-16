package io.suggest.id.login.m.reg.step3

import diode.FastEq
import diode.data.Pot
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.03.19 13:28
  * Description: Состояние одного accept-чекбокса и контента этого чекбокса.
  */
object MRegCheckBoxS {

  def empty = apply()

  implicit object MRegStep3CheckBoxesFastEq extends FastEq[MRegCheckBoxS] {
    override def eqv(a: MRegCheckBoxS, b: MRegCheckBoxS): Boolean = {
      (a.isChecked         ==* b.isChecked) &&
      (a.content          ===* b.content) &&
      (a.isContentShowing  ==* b.isContentShowing)
    }
  }

  @inline implicit def univEq: UnivEq[MRegCheckBoxS] = UnivEq.derive


  def isChecked         = GenLens[MRegCheckBoxS](_.isChecked)
  def content           = GenLens[MRegCheckBoxS](_.content)
  def isContentShowing  = GenLens[MRegCheckBoxS](_.isContentShowing)

}


/** Контейнер под-формы регистрации.
  *
  * @param isChecked Галочка поставлена.
  * @param content Содержимое соглашения.
  * @param isContentShowing Отображается содержимое соглашения.
  */
case class MRegCheckBoxS(
                          isChecked            : Boolean             = false,
                          content              : Pot[None.type]      = Pot.empty,
                          isContentShowing     : Boolean             = false,
                        )
