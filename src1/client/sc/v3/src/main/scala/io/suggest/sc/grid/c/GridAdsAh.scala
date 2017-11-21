package io.suggest.sc.grid.c

import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.dev.MScreen
import io.suggest.err.ErrorConstants
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.grid.m._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.react.ReactDiodeUtil.PotOpsExt
import io.suggest.sc.resp.MScRespActionTypes
import io.suggest.sc.tile.TileConstants
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
                    screenRO        : ModelRO[MScreen],
                    modelRW         : ModelRW[M, MGridS]
                  )
  extends ActionHandler(modelRW)
  with Log
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Реакция на событие скроллинга плитки: разобраться, стоит ли подгружать ещё карточки с сервера.
    case m: GridScroll =>
      val v0 = value
      if (
        !v0.nextReq.isPending &&
        v0.hasMoreAds &&
        v0.gridSz.exists { gridSz =>
          // Оценить уровень скролла. Возможно, уровень не требует подгрузки ещё карточек
          val contentHeight = gridSz.height + TileConstants.CONTAINER_OFFSET_TOP
          val screenHeight = screenRO.value.height
          val scrollPxToGo = contentHeight - screenHeight - m.scrollTop
          scrollPxToGo < TileConstants.LOAD_MORE_SCROLL_DELTA_PX
        }
      ) {
        // В фоне надо будет запустить подгрузку новых карточек.
        val fx = Effect.action {
          GridLoadAds(clean = false, ignorePending = true)
        }
        // Выставить pending в состояние, чтобы повторные события скролла игнорились.
        val v2 = v0.withNextReq( v0.nextReq.pending() )
        updated(v2, fx)

      } else {
        // Больше нет карточек, или запрос карточек уже в процессе, или скроллинг не требует подгрузки карточек.
        noChange
      }


    // Команда к обновлению фактических данных по плитке.
    case m: HandleGridBuildRes =>
      val v0 = value
      if (m.res.width <= 0 || m.res.height <= 0 || v0.gridSz.contains(m.res)) {
        // Размер плитки не изменился или невалиден. Такое надо игнорить.
        noChange
      } else {
        val v2 = v0.withGridSz( Some(m.res) )
        updated(v2)
      }


    // Сигнал к загрузке карточек с сервера согласно текущему состоянию выдачи.
    case m: GridLoadAds =>
      val v0 = value
      if (v0.nextReq.isPending && !m.ignorePending) {
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
            Success( GridLoadAdsResp(m, startTime, tryRes, limit) )
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
            val v1 = if (m.evidence.clean) {
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
            v1
              .withHasMoreAds(
                findAdsResp.ads.size >= m.limit
              )
              .withNextReq( Pot.empty )
          }
        )
        updated(v2)

      } else {
        // Почему-то пришёл неактуальный ответ на запрос.
        LOG.warn( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }


    // Реакция на клик по карточке в плитке.
    case m: GridBlockClick =>
      // Нужно отправить запрос на сервер, чтобы понять, что делать дальше.
      // Возможны разные варианты: фокусировка в карточку, переход в выдачу другого узла, и т.д. Всё это расскажет сервер.
      // TODO stub.
      println( "TODO block clicked: " + m.nodeId )
      noChange

  }

}
