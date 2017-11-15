package io.suggest.sc.grid.c

import diode._
import diode.data.PendingBase
import io.suggest.err.ErrorConstants
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.grid.m.{GridLoadAds, GridLoadAdsResp, MGridS, MScAdData}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.react.ReactDiodeUtil.PotOpsExt
import io.suggest.sc.resp.MScRespActionTypes
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:59
  * Description: Контроллер плитки карточек.
  *
  * @param searchArgsRO Доступ к текущим аргументам поиска карточек.
  */
class GridAdsAh[M](
                    api             : IFindAdsApi,
                    searchArgsRO    : ModelRO[MFindAdsReq],
                    modelRW         : ModelRW[M, MGridS]
                  )
  extends ActionHandler(modelRW)
  with Log
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал к загрузке карточек с сервера согласно текущему состоянию выдачи.
    case m: GridLoadAds =>
      val v0 = value
      if (v0.nextReq.isPending) {
        LOG.warn( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.nextReq) )
        noChange

      } else {
        val searchArgs = searchArgsRO.value
        val nextReqPot2 = v0.nextReq.pending()
        val fx = Effect {
          // Если clean, то нужно обнулять offset.
          val offset = if (m.clean) {
            0
          } else {
            v0.ads.size
          }

          // TODO Вычислять на основе данных параметров MScreen.
          val limit = 10

          val searchArgs2 = searchArgs
            .withLimitOffset( limit = Some(limit), offset = Some(offset) )

          // Запустить запрос с почищенными аргументами...
          val fut = api.findAds( searchArgs2 )

          // Завернуть ответ сервера в экшен:
          val startTime = nextReqPot2.asInstanceOf[PendingBase].startTime
          fut.transform { tryRes =>
            Success( GridLoadAdsResp(m, startTime, tryRes) )
          }
        }

        val v2 = v0.withNextReq( nextReqPot2 )
        updated(v2, fx)
      }


    // Пришёл результат запроса карточек с сервера.
    case m: GridLoadAdsResp =>
      // Сверить timestamp с тем, что внутри Pot'а.
      val v0 = value
      if ( v0.nextReq.isPendingWithStartTime(m.startTime) ) {
        // Это и есть ожидаемый ответ сервера. Разобраться, что там внутри...
        val v2 = m.resp.fold(
          {ex =>
            // Записать ошибку в состояние.
            v0.withNextReq( v0.nextReq.fail(ex) )
          },
          {scResp =>
            // Сервер ответил что-то вразумительное.
            // Пока поддерживается только чистый findAds-ответ, поэтому остальные варианты игнорим.
            val scAction = scResp.respActions.head
            ErrorConstants.assertArg( scAction.acType ==* MScRespActionTypes.AdsTile )
            val findAdsResp = scAction.ads.get
            val newScAds = findAdsResp.ads.map(MScAdData.apply)
            if (m.evidence.clean) {
              v0
                .withAds( newScAds )
                .withSzMult( findAdsResp.szMult )
            } else {
              // Проверить, совпадает ли SzMult?
              ErrorConstants.assertArg( findAdsResp.szMult ==* v0.szMult )
              v0.withAds(
                v0.ads ++ newScAds
              )
            }
          }
        )
        updated(v2)

      } else {
        // Почему-то пришёл неактуальный ответ на запрос.
        LOG.warn( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }


  }

}
