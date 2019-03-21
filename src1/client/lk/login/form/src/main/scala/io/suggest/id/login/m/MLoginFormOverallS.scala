package io.suggest.id.login.m

import diode.FastEq
import io.suggest.id.login.{MLoginTab, MLoginTabs}
import io.suggest.id.login.v.LoginFormCss
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 17:30
  * Description: Общее состояние формы логина за пределами содержимого конкретных табов.
  */
object MLoginFormOverallS {

  implicit object MLoginFormOverallSFastEq extends FastEq[MLoginFormOverallS] {
    override def eqv(a: MLoginFormOverallS, b: MLoginFormOverallS): Boolean = {
      (a.loginTab ===* b.loginTab) &&
      (a.isVisible ==* b.isVisible) &&
      (a.formCss ===* b.formCss) &&
      (a.returnUrl ===* b.returnUrl)
    }
  }

  @inline implicit def univEq: UnivEq[MLoginFormOverallS] = UnivEq.derive

  val loginTab    = GenLens[MLoginFormOverallS](_.loginTab)
  val isVisible   = GenLens[MLoginFormOverallS](_.isVisible)
  val formCss     = GenLens[MLoginFormOverallS](_.formCss)
  val returnUrl   = GenLens[MLoginFormOverallS](_.returnUrl)

}


case class MLoginFormOverallS(
                               loginTab         : MLoginTab         = MLoginTabs.default,
                               isVisible        : Boolean           = false,
                               formCss          : LoginFormCss      = LoginFormCss(),
                               returnUrl        : Option[String]    = None,
                             ) {

  /** Для React-duode connection требуется AnyRef. */
  lazy val isVisibleSome = Some( isVisible )

}
