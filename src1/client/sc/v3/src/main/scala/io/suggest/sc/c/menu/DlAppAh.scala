package io.suggest.sc.c.menu

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.dev.MOsFamily
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.app.{MScAppGetQs, MScAppGetResp}
import io.suggest.sc.m.{MScRoot, OnlineCheckConn, ResetUrlRoute, SetErrorState}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.inx.MScIndexState
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.m.menu.{DlAppOpen, DlInfoResp, ExpandDlApp, MDlAppDia, MkAppDlInfoReq, PlatformSetAppDl, QrCodeExpand, TechInfoDlAppShow}
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
class DlAppAh(
               scAppApi          : IScAppApi,
               modelRW           : ModelRW[MScRoot, MDlAppDia],
               indexStateRO      : ModelRO[MScIndexState],
             )
  extends ActionHandler( modelRW )
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[MScRoot]] = {

    // показать/скрыть диалог
    case m: DlAppOpen =>
      val v0 = value

      if (v0.opened ==* m.opened) {
        noChange

      } else {
        var updF = MDlAppDia.opened replace m.opened
        var finalFx: Effect = ResetUrlRoute().toEffectPure

        if (m.opened) {
          for (osFamily <- v0.platform if v0.getReq.isEmpty && !v0.getReq.isPending) {
            val (reqUpdF, fx) = _dlInfoReq( osFamily )
            updF = updF andThen reqUpdF
            finalFx += fx
          }
        } else {
          // Сокрытие диалога - сброс частей состояния, чтобы перезапросить ссылки с сервера.
          updF = updF andThen (MDlAppDia.getReq replace Pot.empty)
        }

        val v2 = updF(v0)
        updated( v2, finalFx )
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
            .replace( Some(m.osPlatform) ) andThen
          infoReqUpdF
        )(v0)

        updated( v2, fx )
      }


    // Реакция на ответ инфы по загрузкам с сервера.
    case m: DlInfoResp =>
      val v0 = value
      if ( !(v0.getReq isPendingWithStartTime m.timeStampMs) ) {
        noChange

      } else {
        val v2 = MDlAppDia.getReq
          .modify( _.withTry(m.tryResp) )(v0)

        // Эффект, если требуется:
        val fxOpt = m.tryResp.fold[Option[Effect]](
          {ex =>
            // Если ошибка, отрендерить всплывающее сообщение:
            var fx: Effect = Effect.action {
              val errDiaS = MScErrorDia(
                messageCode     = ErrorMsgs.XHR_UNEXPECTED_RESP,
                potRO           = Some( modelRW.zoom[Pot[Any]](_.getReq) ),
                exceptionOpt    = Some( ex ),
                retryAction     = Some( MkAppDlInfoReq ),
              )
              SetErrorState( errDiaS )
            }

            if (OnlineCheckConn.maybeNeedCheckConnectivity(ex))
              fx += OnlineCheckConn.toEffectPure

            Some(fx)
          },
          {resp =>
            // Если только один ответ, то сразу раскрыть его.
            for {
              (_, i) <- resp.dlInfos
                .iterator
                .zipWithIndex
                .nextOption()
              // Если всего лишь один ответ.
              if resp.dlInfos.lengthIs == 1
            } yield {
              // Эффект, чтобы анимацией сглаживать резкий скачок высоты диалога.
              Effect.action( ExpandDlApp(i, isExpanded = true) )
            }
          }
        )

        ah.updatedMaybeEffect( v2, fxOpt )
      }


    // Команда к запуску запроса списка закачек на сервер.
    case MkAppDlInfoReq =>
      val v0 = value

      (for {
        osPlatform <- v0.platform
        if !v0.getReq.isPending
      } yield {
        val (infoReqUpdF, fx) = _dlInfoReq( osPlatform )
        val v2 = infoReqUpdF(v0)
        updated(v2, fx)
      })
        .getOrElse( noChange )


    // Раскрытие/сокрытие панели.
    case m: ExpandDlApp =>
      val v0 = value

      if (m.isExpanded) {
        // Раскрытие плашки
        if (v0.expanded contains m.index) {
          // Плашка уже раскрыта
          noChange
        } else {
          val v2 = (MDlAppDia.expanded replace Some(m.index))(v0)
          updated(v2)
        }
      } else {
        // Сокрытие плашки
        if (v0.expanded contains m.index) {
          val v2 = (MDlAppDia.expanded replace None)(v0)
          updated(v2)
        } else {
          // Эта плашка НЕ раскрыта, и скрывать её не надо.
          noChange
        }
      }


    // Управление отображением тех.информации.
    case m: TechInfoDlAppShow =>
      val v0 = value

      if (v0.showTechInfo ==* m.isShow) {
        noChange
      } else {
        val v2 = (MDlAppDia.showTechInfo replace m.isShow)(v0)
        updated(v2)
      }


    // Раскрыть-свернуть QR-код скачки.
    case m: QrCodeExpand =>
      val v0 = value

      if (v0.qrCodeExpanded ==* m.expanded) {
        noChange
      } else {
        val v2 = (MDlAppDia.qrCodeExpanded replace m.expanded)(v0)
        updated(v2)
      }

  }


  private def _dlInfoReq(osFamily: MOsFamily) = {
    val timeStampMs = System.currentTimeMillis()

    val fx: Effect = Effect {
      val inxState = indexStateRO.value
      val qs = MScAppGetQs(
        osFamily  = osFamily,
        onNodeId  = inxState.rcvrId,
        rdr       = false,
      )

      scAppApi
        .appDownloadInfo( qs )
        .transform { tryResp =>
          val r = DlInfoResp( timeStampMs, tryResp )
          Success(r)
        }
    }

    val updF = MDlAppDia.getReq.replace {
      Pot.empty[MScAppGetResp].pending(timeStampMs)
    }

    updF -> fx
  }

}

