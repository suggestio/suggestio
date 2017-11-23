package io.suggest.sc.grid.c

import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.dev.MScreen
import io.suggest.err.ErrorConstants
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.grid.m._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.react.ReactDiodeUtil.PotOpsExt
import io.suggest.sc.resp.MScRespActionTypes
import io.suggest.sc.root.m.HandleIndexResp
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
        !v0.ads.isPending &&
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
        val v2 = v0.withAds( v0.ads.pending() )
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
      if (v0.ads.isPending && !m.ignorePending) {
        LOG.warn( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.ads) )
        noChange

      } else {
        val searchArgs = searchArgsRO.value
        val nextReqPot2 = v0.ads.pending()
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

        val v2 = v0.withAds( nextReqPot2 )
        updated(v2, fx)
      }


    // Пришёл результат запроса карточек с сервера.
    case m: GridLoadAdsResp =>
      // Сверить timestamp с тем, что внутри Pot'а.
      val v0 = value
      if ( v0.ads.isPendingWithStartTime(m.startTime) ) {
        // Это и есть ожидаемый ответ сервера. Разобраться, что там внутри...
        val v2 = m.resp.fold(
          {ex =>
            // Записать ошибку в состояние.
            v0.withAds( v0.ads.fail(ex) )
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
                .withAds( v0.ads.ready(newScAds) )
                .withSzMult( findAdsResp.szMult )
            } else {
              // Проверить, совпадает ли SzMult?
              ErrorConstants.assertArg( findAdsResp.szMult ==* v0.szMult )
              v0.withAds(
                v0.ads.map { _ ++ newScAds }
              )
            }
            v1.withHasMoreAds(
              findAdsResp.ads.size >= m.limit
            )
          }
        )
        updated(v2)

      } else {
        // Почему-то пришёл неактуальный ответ на запрос.
        LOG.warn( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }


    // Реакция на клик по карточке в плитке.
    // Нужно отправить запрос на сервер, чтобы понять, что делать дальше.
    // Возможны разные варианты: фокусировка в карточку, переход в выдачу другого узла, и т.д. Всё это расскажет сервер.
    case m: GridBlockClick =>
      val v0 = value

      // Поискать запрошенную карточку в состоянии.
      _findAd(m.nodeId, v0)
        .fold {
          LOG.error( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange

        } { case (ad0, index) =>
          // Есть карточка в плитке. Посмотреть, сфокусирована она или нет?
          def __saveAdIntoValue(newAd: MScAdData) = _saveAdIntoValue(index, newAd, v0)

          ad0.focused.fold {
            // Карточка сейчас скрыта, её нужно раскрыть.
            // Собрать запрос фокусировки на ровно одной рекламной карточке.
            val fx = Effect {
              val args0 = searchArgsRO.value
              val args = MFindAdsReq(
                allowReturnJump = Some( true ),
                adIdLookup      = Some( m.nodeId ),
                adsLookupMode   = None,
                screenInfo      = args0.screenInfo,
                receiverId      = args0.receiverId
              )
              api.focusedAds(args)
                .transform { tryResp =>
                  Success( FocusedResp(m.nodeId, tryResp) )
                }
            }

            // выставить текущей карточке, что она pending
            val ad1 = ad0.withFocused( ad0.focused.pending() )

            val v2 = __saveAdIntoValue(ad1)
            updated(v2, fx)

          } { _ =>
            // Карточка уже открыта, её надо свернуть назад в main-блок.
            val ad1 = ad0.withFocused( Pot.empty )
            val v2 = __saveAdIntoValue(ad1)
            updated(v2)
          }
        }


    // Сигнал завершения запроса фокусировки на какой-либо карточке
    case m: FocusedResp =>
      val v0 = value

      // Найти фокусируемую рекламную карточку
      _findAd(m.nodeId)
        .fold {
          // Не найдена искомая карточка.
          LOG.warn( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange

        } { case (ad0, index) =>
          def __saveAdIntoValue(newAd: MScAdData) = _saveAdIntoValue(index, newAd, v0)

          // Есть искомая карточка. Перейти к обработке результата запроса.
          m.tryResp.fold(
            // Какая-то ошибка запроса этой рекламной карточки.
            {ex =>
              LOG.error(ErrorMsgs.XHR_UNEXPECTED_RESP, ex, msg = m)
              val v2 = __saveAdIntoValue(
                ad0.withFocused(
                  ad0.focused.fail(ex)
                )
              )
              updated(v2)
            },
            {sc3Resp =>
              // Может прийти редирект в другую выдачу, а может -- раскрытие карточки.
              val ra = sc3Resp.respActions.head
              ra.acType match {
                // Редирект в другую выдачу. Форсируем переключение в новую плитку.
                case MScRespActionTypes.Index =>
                  val fx = Effect.action( HandleIndexResp(m.tryResp, reqTimestamp = None) )
                  effectOnly( fx )

                // Фокусировка: раскрыть текущую карточку с помощью принятого контента.
                case MScRespActionTypes.AdsFoc =>
                  val adsResp = ra.ads.get
                  val v2 = __saveAdIntoValue(
                    ad0.withFocused(
                      ad0.focused.ready(
                        MBlkRenderData( adsResp.ads.head )
                      )
                    )
                  )
                  updated(v2)

                case other =>
                  LOG.error( ErrorMsgs.UNSUPPORTED_VALUE_OF_ARGUMENT, msg = (m, other) )
                  throw new UnsupportedOperationException(other.toString)
              }
            }
          )
        }

  }


  /** Найти карточку с указанным id в состоянии, вернув её и её индекс. */
  private def _findAd(nodeId: String, v0: MGridS = value): Option[(MScAdData, Int)] = {
    v0.ads
      .toOption
      .flatMap { ads =>
        ads
          .iterator
          .zipWithIndex
          .find { _._1.nodeId contains nodeId }
      }
  }

  /** Залить в состояние обновлённый инстанс карточки. */
  def _saveAdIntoValue(index: Int, newAd: MScAdData, v0: MGridS = value): MGridS = {
    v0.withAds(
      for (ads0 <- v0.ads) yield {
        ads0.updated(index, newAd)
      }
    )
  }

}
