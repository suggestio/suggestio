package io.suggest.id.login.m

import diode.FastEq
import diode.data.Pot
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 15:47
  * Description: Состояние формы логина по имени-паролю.
  */
object MEpwLoginS {

  implicit object MEpwLoginSFastEq extends FastEq[MEpwLoginS] {
    override def eqv(a: MEpwLoginS, b: MEpwLoginS): Boolean = {
      (a.name ===* b.name) &&
      (a.password ===* b.password) &&
      (a.loginReq ===* b.loginReq)
    }
  }

  @inline implicit def univEq: UnivEq[MEpwLoginS] = UnivEq.derive

  val name      = GenLens[MEpwLoginS](_.name)
  val password  = GenLens[MEpwLoginS](_.password)
  val loginReq  = GenLens[MEpwLoginS](_.loginReq)

}


/** Контейнер данных состояния имени и пароля.
  *
  * @param name Имя пользователя (email).
  * @param password Пароль пользователя.
  */
case class MEpwLoginS(
                       name          : String       = "",
                       password      : String       = "",
                       loginReq      : Pot[None.type]       = Pot.empty,
                     )
