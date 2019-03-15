package io.suggest.id.login.m

import diode.FastEq
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
      (a.isForeignPc ==* b.isForeignPc)
    }
  }

  @inline implicit def univEq: UnivEq[MLoginFormOverallS] = UnivEq.derive

  val loginTab    = GenLens[MLoginFormOverallS](_.loginTab)
  val isVisible   = GenLens[MLoginFormOverallS](_.isVisible)
  val isForeignPc = GenLens[MLoginFormOverallS](_.isForeignPc)

}


case class MLoginFormOverallS(
                               loginTab         : MLoginTab         = MLoginTabs.default,
                               isVisible        : Boolean           = false,
                               isForeignPc      : Boolean           = false,
                             ) {

  /** Для React-duode connection требуется AnyRef. */
  lazy val isVisibleSome = Some( isVisible )

  /** Для React-duode connection требуется AnyRef. */
  lazy val isForeignPcSome = Some( isForeignPc )

}
