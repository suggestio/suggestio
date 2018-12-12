package io.suggest.ad.edit.c

import diode._
import io.suggest.ad.edit.m.{DeleteAdClick, DeleteAdResp, MAdEditFormConf}
import io.suggest.ad.edit.srv.ILkAdEditApi
import io.suggest.lk.m.{DeleteConfirmPopupCancel, DeleteConfirmPopupOk, MDeleteConfirmPopupS}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.17 18:17
  * Description: Контроллер удаления карточки.
  */
class DeleteAh[M](
                   lkAdEditApi  : ILkAdEditApi,
                   confRO       : ModelRO[MAdEditFormConf],
                   modelRW      : ModelRW[M, Option[MDeleteConfirmPopupS]]
                 )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке удаления текущей карточки.
    case DeleteAdClick =>
      val v0 = value
      v0.fold {
        val v2 = Some( MDeleteConfirmPopupS() )
        updated( v2 )
      } { _ =>
        // Дублирующийся клик, скорее всего.
        noChange
      }


    // Юзер подтвердил удаление рекламной карточки.
    case DeleteConfirmPopupOk =>
      val v0 = value.get
      val v2 = Some( v0.withRequest( v0.request.pending() ) )

      val adId = confRO.value.adId.get
      val fx = Effect {
        lkAdEditApi
          .deleteSubmit( adId )
          .transform { tryResp =>
            Success( DeleteAdResp(tryResp) )
          }
      }

      updated(v2, fx)


    // Юзер передумал удалять рекламную карточку.
    case DeleteConfirmPopupCancel =>
      val v0 = value
      v0.fold {
        noChange
      } { _ =>
        updated( None )
      }


    // Ответ сервера по теме удаления текущей карточки.
    case m: DeleteAdResp =>
      // Отредиректить юзера прочь из формы.
      val v0 = value.get

      val v2 = v0.withRequest(
        m.tryResp.fold(
          v0.request.fail,
          v0.request.ready
        )
      )

      if (v2.request.isReady)
        DomQuick.goToLocation( m.tryResp.get )

      updated( Some(v2) )

  }

}
