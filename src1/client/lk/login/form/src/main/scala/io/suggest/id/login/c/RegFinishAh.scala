package io.suggest.id.login.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.login.m.{RegAccept, RegPdnSetAccepted, RegTosSetAccepted}
import io.suggest.id.login.m.reg.{MAcceptCheckBoxS, MRegFinishS}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 15:19
  * Description: Форма завершения регистрации.
  */
class RegFinishAh[M](
                      modelRW      : ModelRW[M, MRegFinishS]
                    )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: RegTosSetAccepted =>
      val v0 = value
      if (v0.tos.isChecked ==* m.isAccepted) {
        noChange

      } else {
        val v2 = MRegFinishS.tos
          .composeLens( MAcceptCheckBoxS.isChecked )
          .set(m.isAccepted)( v0 )
        updated( v2 )
      }


    case m: RegPdnSetAccepted =>
      val v0 = value
      if (v0.pdn.isChecked ==* m.isAccepted) {
        noChange

      } else {
        val v2 = MRegFinishS.pdn
          .composeLens( MAcceptCheckBoxS.isChecked )
          .set( m.isAccepted )(v0)
        updated( v2 )
      }


    case RegAccept =>
      val v0 = value
      if (v0.acceptReq.isPending) {
        noChange

      } else {
        val tstamp = System.currentTimeMillis()

        // TODO Организовать запрос на сервер.
        val fx: Effect = ???

        val updateF = MRegFinishS.acceptReq
          .modify( _.pending(tstamp) )
        val v2 = updateF(v0)
        updated(v2, fx)
      }

    // TODO Реакция на ответ сервера.

  }

}
