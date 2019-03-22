package io.suggest.id.login.m.epw

import diode.FastEq
import diode.data.Pot
import io.suggest.lk.m.MTextFieldS
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
      (a.name ===* b.name) &&
      (a.password ===* b.password) &&
      (a.isForeignPc ==* b.isForeignPc) &&
      (a.loginReq ===* b.loginReq) &&
      (a.loginBtnEnabled ==* b.loginBtnEnabled)
    }
  }

  @inline implicit def univEq: UnivEq[MEpwLoginS] = UnivEq.derive

  val name              = GenLens[MEpwLoginS](_.name)
  val password          = GenLens[MEpwLoginS](_.password)
  val isForeignPc       = GenLens[MEpwLoginS](_.isForeignPc)
  val loginReq          = GenLens[MEpwLoginS](_.loginReq)
  val loginBtnEnabled   = GenLens[MEpwLoginS](_.loginBtnEnabled)

}


/** Контейнер данных состояния имени и пароля.
  *
  * @param name Имя пользователя (email).
  * @param password Пароль пользователя.
  * @param loginReq Состояние запроса логина.
  * @param loginBtnEnabled Активна ли кнопка логина?
  */
case class MEpwLoginS(
                       name                 : MTextFieldS        = MTextFieldS.empty,
                       password             : MTextFieldS        = MTextFieldS.empty,
                       isForeignPc          : Boolean               = false,
                       loginReq             : Pot[String]           = Pot.empty,
                       loginBtnEnabled      : Boolean               = false,
                     ) {

  /** Когда надо отображать на экране прогресс-бар ожидания? */
  lazy val isShowPendingSome = Some( loginReq.isPending )

  /** Для React-duode connection требуется AnyRef. */
  lazy val isForeignPcSome = Some( isForeignPc )

}
