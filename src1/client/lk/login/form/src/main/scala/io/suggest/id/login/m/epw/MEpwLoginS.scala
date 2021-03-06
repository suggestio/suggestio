package io.suggest.id.login.m.epw

import diode.FastEq
import diode.data.Pot
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
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
      (a.name             ===* b.name) &&
      (a.password         ===* b.password) &&
      (a.isForeignPc       ==* b.isForeignPc) &&
      (a.loginReq         ===* b.loginReq) &&
      (a.loginBtnEnabled   ==* b.loginBtnEnabled)
    }
  }

  @inline implicit def univEq: UnivEq[MEpwLoginS] = UnivEq.derive

  def name              = GenLens[MEpwLoginS](_.name)
  def password          = GenLens[MEpwLoginS](_.password)
  def isForeignPc       = GenLens[MEpwLoginS](_.isForeignPc)
  def loginReq          = GenLens[MEpwLoginS](_.loginReq)
  def loginBtnEnabled   = GenLens[MEpwLoginS](_.loginBtnEnabled)
  def passwordVisible   = GenLens[MEpwLoginS](_.passwordVisible)

}


/** Контейнер данных состояния имени и пароля.
  *
  * @param name Имя пользователя (email).
  * @param password Пароль пользователя.
  * @param loginReq Состояние запроса логина.
  * @param loginBtnEnabled Активна ли кнопка логина?
  */
case class MEpwLoginS(
                       name                 : MTextFieldS           = MTextFieldS.empty,
                       password             : MTextFieldS           = MTextFieldS.empty,
                       isForeignPc          : Boolean               = false,
                       loginReq             : Pot[String]           = Pot.empty,
                       loginBtnEnabled      : Boolean               = false,
                       passwordVisible      : Boolean               = false,
                     )
