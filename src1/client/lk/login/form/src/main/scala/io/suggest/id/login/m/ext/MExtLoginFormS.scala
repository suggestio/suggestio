package io.suggest.id.login.m.ext

import diode.FastEq
import diode.data.Pot
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 17:21
  * Description: Контейнер данных состояния формы логина.
  */
object MExtLoginFormS {

  implicit object MExtLoginFormSFastEq extends FastEq[MExtLoginFormS] {
    override def eqv(a: MExtLoginFormS, b: MExtLoginFormS): Boolean = {
      (a.loginUrlReq ===* b.loginUrlReq)
    }
  }

  @inline implicit def univEq: UnivEq[MExtLoginFormS] = UnivEq.derive

  val loginUrlReq = GenLens[MExtLoginFormS](_.loginUrlReq)

}


/** Класс-контейнер данных состояния формы логина.
  *
  * @param loginUrlReq Состояние запроса на сервер за ссылкой для редиректа.
  */
case class MExtLoginFormS(
                           loginUrlReq          : Pot[String]           = Pot.empty,
                         ) {

  lazy val loginUrlReqPendingSome = Some( loginUrlReq.isPending )

}
