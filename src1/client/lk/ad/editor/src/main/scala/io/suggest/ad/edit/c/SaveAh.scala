package io.suggest.ad.edit.c

import diode._
import diode.data.PendingBase
import io.suggest.ad.edit.m._
import io.suggest.ad.edit.srv.ILkAdEditApi
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.10.17 15:54
  * Description: Контроллер управления сохранением карточки.
  */
class SaveAh[M](
                 lkAdEditApi  : ILkAdEditApi,
                 confRO       : ModelRO[MAdEditFormConf],
                 modelRW      : ModelRW[M, MAeRoot]
               )
  extends ActionHandler(modelRW)
  with Log
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке сохранения карточки.
    case SaveAd =>
      val v0 = modelRW.value
      if (v0.save.saveReq.isPending) {
        // Уже сейчас есть какой-то запущенный реквест сохранения.
        LOG.log( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = v0.save.saveReq )
        noChange

      } else {
      // TODO Проверить текущие upload-запросы.
        // Реквест сохранения можно отправить на сервер прямо сейчас. Подготовить данные для сохранения:
        val form = v0.toForm
        val conf = confRO.value

        val pot2 = v0.save.saveReq
          .pending()
        val pot2p = pot2.asInstanceOf[PendingBase]
        val ts = pot2p.startTime

        val fx = Effect {
          val saveFut = lkAdEditApi.saveAdSubmit( conf.producerId, form )
          saveFut.transform { tryRes =>
            Success( SaveAdResp(ts, tryRes) )
          }
        }

        val v2 = v0
          .withSave(
            v0.save
              .withSaveReq( pot2 )
          )

        updated(v2, fx)
      }


    // Сигнал завершения запроса сохранения.
    case m: SaveAdResp =>
      val v0 = value
      val saveReq0 = v0.save.saveReq

      if (saveReq0.isPending && saveReq0.asInstanceOf[PendingBase].startTime ==* m.timestamp) {
        // Это ожидаемый запрос. Анализируем ответ:
        m.tryResp.fold(
          // Ошибка выполнения запроса.
          {ex =>
            val v2 = v0.withSave(
              v0.save
                .withSaveReq(
                  saveReq0.fail(ex)
                )
            )
            updated(v2)
          },
          // Запрос сохранения исполнен. Залить новые данные в текущую форму.
          {formReInit =>
            val v2 = v0
              .withConf( formReInit.conf )
              .withSave(
                v0.save
                  .withSaveReq( v0.save.saveReq.ready(formReInit) )
              )
            // TODO Запилить withDoc(), пере-заливающий данные эджей и шаблона в jdArgs.

            updated(v2)
          }
        )

      } else {
        // Неожидаемый ответ, неизвестно что с ним делать.
        // Возможно, запрос был выкинут по таймауту или что-то ещё пошло не так. Игнорим.
        LOG.warn( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
