package io.suggest.id.login.m.reg

import diode.FastEq
import diode.data.Pot
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.19 17:42
  * Description: Состояние под-формы окончания регистрации.
  * Когда юзер возвращается из гос.услуг первый раз - надо задать пару вопросов.
  */
object MRegFinishS {

  implicit object MRegFinishSFastEq extends FastEq[MRegFinishS] {
    override def eqv(a: MRegFinishS, b: MRegFinishS): Boolean = {
      (a.tosAccepted ==* b.tosAccepted) &&
      (a.pdnAccepted ==* b.pdnAccepted) &&
      (a.acceptReq ===* b.acceptReq)
    }
  }

  implicit def univEq: UnivEq[MRegFinishS] = UnivEq.derive

  val tosAccepted = GenLens[MRegFinishS]( _.tosAccepted )
  val pdnAccepted = GenLens[MRegFinishS]( _.pdnAccepted )
  val acceptReq   = GenLens[MRegFinishS]( _.acceptReq )

}


/** Контейнер данных состояния окончания регистрации.
  *
  * @param tosAccepted Принял ли юзер условия соглашения сервиса?
  * @param pdnAccepted Принял ли юзер условия персональных данных?
  * @param acceptReq Реквест к серверу с подтверждением регистации.
  */
case class MRegFinishS(
                        tosAccepted         : Boolean         = false,
                        pdnAccepted         : Boolean         = false,
                        acceptReq           : Pot[String]     = Pot.empty,
                      )
