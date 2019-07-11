package io.suggest.id.login.m.pwch

import diode.FastEq
import io.suggest.id.login.v.LoginFormCss
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.07.19 15:57
  * Description: Состояние формы смены пароля.
  */
object MPwChangeRootS {

  def empty = apply()

  implicit object MPwChangeRootSFastEq extends FastEq[MPwChangeRootS] {
    override def eqv(a: MPwChangeRootS, b: MPwChangeRootS): Boolean = {
      (a.form ===* b.form) &&
      (a.formCss ===* b.formCss)
    }
  }

  @inline implicit def univEq: UnivEq[MPwChangeRootS] = UnivEq.derive

  val form    = GenLens[MPwChangeRootS](_.form)
  val formCss = GenLens[MPwChangeRootS](_.formCss)

}


/** Корневой контейнер состояния формы смены пароля.
  *
  * @param form Состояние полей формы.
  * @param formCss CSS-стили.
  */
case class MPwChangeRootS(
                           form           : MPwChangeS            = MPwChangeS.empty,
                           formCss        : LoginFormCss          = LoginFormCss(),
                         )
