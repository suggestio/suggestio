package io.suggest.sc.c.menu

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.dev.MOsFamily
import io.suggest.sc.app.{MScAppGetQs, MScAppGetResp}
import io.suggest.sc.m.inx.MScIndexState
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.m.menu.{DlInfoResp, MDlAppDia, OpenCloseAppDl, PlatformSetAppDl}
import io.suggest.sc.u.api.IScAppApi
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.2020 16:16
  * Description: Контроллер под-менюшки пункта скачивания приложения.
  */
class DlAppAh[M](
                  scAppApi          : IScAppApi,
                  modelRW           : ModelRW[M, MDlAppDia],
                  indexStateRO      : ModelRO[MScIndexState],
                )
  extends ActionHandler( modelRW )
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // показать/скрыть диалог
    case m: OpenCloseAppDl =>
      val v0 = value

      if (v0.opened ==* m.opened) {
        noChange

      } else {
        var updF = MDlAppDia.opened set m.opened
        var fxOpt = Option.empty[Effect]

        if (m.opened) {
          for (osFamily <- v0.platform if v0.getReq.isEmpty && !v0.getReq.isPending) {
            val (reqUpdF, fx) = _dlInfoReq( osFamily )
            updF = updF andThen reqUpdF
            fxOpt = Some(fx)
          }
        } else {
          // Сокрытие диалога - сброс частей состояния, чтобы перезапросить ссылки с сервера.
          updF = updF andThen (MDlAppDia.getReq set Pot.empty)
        }

        val v2 = updF(v0)
        ah.updatedMaybeEffect(v2, fxOpt)
      }


    // Выставить платформу для скачивания.
    case m: PlatformSetAppDl =>
      val v0 = value

      if (v0.platform contains[MOsFamily] m.osPlatform) {
        noChange

      } else {
        val (infoReqUpdF, fx) = _dlInfoReq(m.osPlatform)

        val v2 = (
          MDlAppDia.platform
            .set( Some(m.osPlatform) ) andThen
          infoReqUpdF
        )(v0)

        updated( v2, fx )
      }


    case m: DlInfoResp =>
      val v0 = value
      if ( !(v0.getReq isPendingWithStartTime m.timeStampMs) ) {
        noChange

      } else {
        val v2 = MDlAppDia.getReq
          .modify( _.withTry(m.tryResp) )(v0)
        updated(v2)
      }

  }


  private def _dlInfoReq(osFamily: MOsFamily) = {
    val timeStampMs = System.currentTimeMillis()

    val fx: Effect = Effect {
      val inxState = indexStateRO.value
      val qs = MScAppGetQs(
        osFamily  = osFamily,
        onNodeId    = inxState.rcvrId,
      )

      scAppApi
        .appDownloadInfo( qs )
        .transform { tryResp =>
          val r = DlInfoResp( timeStampMs, tryResp )
          Success(r)
        }
    }

    val updF = MDlAppDia.getReq.set {
      Pot.empty[MScAppGetResp].pending(timeStampMs)
    }

    updF -> fx
  }

}

