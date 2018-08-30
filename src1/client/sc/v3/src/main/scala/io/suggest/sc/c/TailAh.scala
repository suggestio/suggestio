package io.suggest.sc.c

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MGeoPoint
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sc.GetRouterCtlF
import io.suggest.sc.m._
import io.suggest.sc.m.grid._
import io.suggest.sc.m.hdr.{MenuOpenClose, SearchOpenClose}
import io.suggest.sc.m.inx.{GetIndex, MScIndex, WcTimeOut}
import io.suggest.sc.m.search.NodeRowClick
import io.suggest.sc.sc3.Sc3Pages.MainScreen
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.log.Log
import io.suggest.ueq.UnivEqUtil._
import io.suggest.common.coll.Lists.Implicits.OptionListExtOps
import io.suggest.spa.DoNothing
import japgolly.univeq._
import io.suggest.spa.DiodeUtil.Implicits._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.07.17 12:53
  * Description: Бывает, что надо заглушить сообщения, т.к. обработка этих сообщений стала
  * неактуальной, а сообщения всё ещё идут.
  */
object TailAh {

  /** Сборка снимка данных состояния.
    * @param v0 Инстанс состояния MScRoot.
    * @return Данные в MainScreen.
    */
  def getMainScreenSnapShot(v0: MScRoot): MainScreen = {
    val inxState = v0.index.state
    val searchOpened = v0.index.search.isShown
    val currRcvrId = inxState.currRcvrId

    // TODO Поддержка нескольких тегов в URL.
    val selTagIdOpt = v0.index.search.geo.data.selTagIds.headOption

    MainScreen(
      nodeId        = currRcvrId,
      // Не рендерить координаты в URL, если находишься в контексте узла, закрыта панель поиска и нет выбранного тега.
      // Это улучшит кэширование, возможно улучшит приватность при обмене ссылками.
      locEnv        = OptionUtil.maybe {
        currRcvrId.isEmpty || v0.index.search.isShown || selTagIdOpt.nonEmpty
      }(v0.index.search.geo.mapInit.state.center),
      generation    = Some( inxState.generation ),
      searchOpened  = searchOpened,
      tagNodeId     = selTagIdOpt,
      menuOpened    = v0.index.menu.opened,
      focusedAdId   = for {
        scAdData <- v0.grid.core.focusedAdOpt
        focData  <- scAdData.focused.toOption
        if focData.userFoc
        adNodeId <- scAdData.nodeId
      } yield {
        adNodeId
      }
    )
  }

}


/** Непосредственный контроллер "последних" сообщений. */
class TailAh[M](
                 modelRW                  : ModelRW[M, MScRoot],
                 respWithActionHandlers   : Seq[IRespWithActionHandler],
                 routerCtlF               : GetRouterCtlF
               )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Заставить роутер собрать новую ссылку.
    case ResetUrlRoute =>
      val m = TailAh.getMainScreenSnapShot( value )
      // Уведомить в фоне роутер, заодно разблокировав интерфейс.
      Future {
        routerCtlF()
          .set( m )
          .runNow()
      }

      noChange


    // js-роутер заливает в состояние данные из URL.
    case m: RouteTo =>
      val v0 = value
      val currMainScreen = TailAh.getMainScreenSnapShot(v0)

      // Считаем, что js-роутер уже готов. Если нет, то это сообщение должно было быть перехвачено в JsRouterInitAh.

      var gridNeedsReload = false
      val indexRespTotallyEmpty = v0.index.resp.isTotallyEmpty
      var nodeIndexNeedsReload = indexRespTotallyEmpty
      var needUpdateUi = false

      var inx = v0.index

      var fxsAcc = List.empty[Effect]

      // Проверка id узла. Если отличается, то надо перезаписать.
      if (m.mainScreen.nodeId !=* currMainScreen.nodeId) {
        nodeIndexNeedsReload = true
        inx = inx.withState {
          inx.state.withRcvrNodeId( m.mainScreen.nodeId.toList )
        }
      }

      // Проверка поля generation
      for {
        generation2 <- m.mainScreen.generation
        if !currMainScreen.generation.contains( generation2 )
      } {
        // generation не совпадает. Надо будет перезагрузить плитку.
        gridNeedsReload = true
        inx = inx.withState(
          inx.state.withGeneration( generation2 )
        )
      }

      // Проверка поля searchOpened
      if (m.mainScreen.searchOpened !=* currMainScreen.searchOpened) {
        // Вместо патчинга состояния имитируем клик: это чтобы возможные сайд-эффекты обычного клика тоже отработали.
        fxsAcc ::= Effect.action( SearchOpenClose(m.mainScreen.searchOpened) )
      }
      // TODO Возможно, SwitchTab надо запихнуть в else-ветвь тут?

      // Смотрим координаты текущей точки.
      for {
        currGeoPoint <- m.mainScreen.locEnv
        if !currMainScreen.locEnv.contains(currGeoPoint)
      } {
        needUpdateUi = true
        inx = withMapCenterInitReal(currGeoPoint, inx)
      }

      // Смотрим текущий выделенный тег
      for {
        tagNodeId <- m.mainScreen.tagNodeId
        if !currMainScreen.tagNodeId.contains( tagNodeId )
      } {
        // Имитируем клик по тегу, да и всё.
        fxsAcc ::= Effect.action {
          NodeRowClick( tagNodeId )
        }
      }

      // Сверить панель меню открыта или закрыта
      if (m.mainScreen.menuOpened !=* currMainScreen.menuOpened)
        fxsAcc ::= Effect.action( MenuOpenClose(m.mainScreen.menuOpened) )

      // Сверить focused ad id:
      if (
        m.mainScreen.focusedAdId !=* currMainScreen.focusedAdId &&
        !nodeIndexNeedsReload
      ) {
        for {
          focusedAdId <- m.mainScreen.focusedAdId
            .orElse( currMainScreen.focusedAdId )
        } {
          fxsAcc ::= Effect.action( GridBlockClick(nodeId = focusedAdId) )
        }
      }

      // Обновлённое состояние, которое может быть и не обновлялось:
      lazy val v2 = v0.withIndex( inx )

      // Принять решение о перезагрузке выдачи, если необходимо.
      if ( fxsAcc.isEmpty && !needUpdateUi && !gridNeedsReload && inx ===* v0.index) {
        // Роутер шлёт RouteTo на каждый чих, даже просто в ответ на выставление этой самой роуты.
        // TODO надо как-то отучить его от этой привычки.
        noChange

      } else if (v0.internals.geoLockTimer.nonEmpty) {
        // Блокирование загрузки.
        val fxOpt = fxsAcc.mergeEffects
        ah.updatedSilentMaybeEffect(v2, fxOpt)

        // TODO Если новое состояние условно "пустое" (без rcvrId или координат), то запустить геолокацию, можно даже без таймера (поддерживается?), а просто запустить.
        // TODO Иначе - остановить геолокацию, если она запущена сейчас -- т.к. состояние "непустое".
      } else if (nodeIndexNeedsReload) {
        // Целиковая перезагрузка выдачи.
        fxsAcc ::= getIndexFx(
          geoIntoRcvr = false,
          focusedAdId = m.mainScreen.focusedAdId,
          retUserLoc  = v2.index.search.geo.mapInit.userLoc.isEmpty
        )
        val fx = fxsAcc.mergeEffects.get
        ah.updateMaybeSilentFx(needUpdateUi)(v2, fx)

      } else if (gridNeedsReload) {
        // Узел не требует обновления, но требуется перезагрузка плитки.
        fxsAcc ::= Effect.action {
          GridLoadAds( clean = true, ignorePending = true )
        }
        val fx = fxsAcc.mergeEffects.get
        ah.updateMaybeSilentFx(needUpdateUi)(v2, fx)

      } else if (needUpdateUi) {
        // Изменения на уровне интерфейса.
        val fxOpt = fxsAcc.mergeEffects
        ah.updatedMaybeEffect( v2, fxOpt )

      } else {
        // Ничего не изменилось или только эффекты.
        val fxOpt = fxsAcc.mergeEffects
        ah.maybeEffectOnly( fxOpt )
      }


    // Сигнал наступления геолокации (или ошибки геолокации).
    case m: GlPubSignal =>
      val v0 = value

      // Сейчас ожидаем максимально точных координат?
      v0.internals.geoLockTimer.fold {
        // Сейчас не ожидаются координаты. Просто сохранить координаты в состояние карты.
        m.orig
          .locationOpt
          // TODO Opt: iphone шлёт кучу одинаковых или похожих координат, раз в 1-2 секунды. Надо это фильтровать?
          .fold( noChange ) { geoLoc =>
            // Не двигать карту, сохранять координаты только в .userLoc
            val v2 = v0.withIndex(
              v0.index.withSearch(
                v0.index.search.withGeo(
                  v0.index.search.geo.withMapInit(
                    v0.index.search.geo.mapInit
                      .withUserLoc( Some(geoLoc) )
                  )
                )
              )
            )
            ah.updateMaybeSilent( !v0.index.search.isShown )(v2)
          }

      } { geoLockTimerId =>
        // Прямо сейчас этот контроллер ожидает координаты.
        // Функция общего кода завершения ожидания координат: запустить выдачу, выключить geo loc, грохнуть таймер.
        def __finished(v00: MScRoot, isSuccess: Boolean) = {
          val fxs = getIndexFx(geoIntoRcvr = true, retUserLoc = !isSuccess) + geoOffFx
          DomQuick.clearTimeout(geoLockTimerId)
          val v22 = _removeTimer(v00)
          updatedSilent(v22, fxs)
        }

        // Ожидаются координаты геолокации прямо сейчас.
        m.orig.either.fold(
          // Ожидаются координаты, но пришла ошибка. Можно ещё подождать, но пока считаем, что это конец.
          // Скорее всего, юзер отменил геолокацию или что-то ещё хуже.
          {_ =>
            __finished(v0, isSuccess = false)
          },
          // Есть какие-то координаты, но не факт, что ожидаемо точные.
          {geoLoc =>
            // Т.к. работает suppressor, то координаты можно всегда записывать в состояние, не боясь постороннего "шума".
            val v1 = v0.withIndex(
              v0.index.withSearch(
                v0.index.search.withGeo(
                  v0.index.search.geo.withMapInit(
                    v0.index.search.geo.mapInit
                      // Переместить карту в текущую точку.
                      .withState(
                        v0.index.search.geo.mapInit.state
                          .withCenterInitReal( geoLoc.point )
                      )
                      // Текущая позиция юзера - тут же.
                      .withUserLoc( Some(geoLoc) )
                  )
                )
              )
            )

            if (m.orig.glType.highAccuracy) {
              // Пришли точные координаты. Завершаем ожидание.
              __finished(v1, isSuccess = true)
            } else {
              // Пока получены не точные координаты. Надо ещё подождать координат по-точнее...
              updatedSilent(v1)
            }
          }
        )
      }


    // Наступил таймаут ожидания геолокации. Нужно активировать инициализацию в имеющемся состоянии
    case GeoLocTimeOut =>
      val v0 = value
      v0.internals.geoLockTimer.fold {
        noChange
      } { _ =>
        // Удалить из состояния таймер геолокации, запустить выдачу.
        val v2 = _removeTimer(v0)
        val fxs = getIndexFx(geoIntoRcvr = true, retUserLoc = true) + geoOffFx
        updatedSilent(v2, fxs)
      }


    // Объединённая обработка результатов API-запросов к серверу.
    // Результаты всех index, grid, focused, tags запросов - попадают только сюда.
    case m: HandleScApiResp =>
      val value0 = value

      val rhCtx0 = MRhCtx(value0, m)
      val respHandler = respWithActionHandlers
        .find { rh =>
          rh.isMyReqReason(rhCtx0)
        }
        .get

      // Класс сложного аккамулятора при свёрстке resp-экшенов:
      case class RaFoldAcc( v1         : MScRoot        = value0,
                            fxAccRev   : List[Effect]   = Nil
                          )

      // Надо сначала проверить timestamp, если он задан.
      val isActualResp = m.reqTimeStamp.fold(true) { reqTimeStamp =>
        // Найти среди pot'ов состояния соответствие timestamp'у.
        respHandler
          .getPot(rhCtx0)
          .exists(_ isPendingWithStartTime reqTimeStamp)
      }

      // Сборка сложной логики внутри Either: по левому борту ошибки, по правому - нормальный ход действий.
      Either
        .cond(
          isActualResp,
          left = {
            LOG.log(WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m)
            noChange
          },
          right = None
        )
        // Раскрыть содержимое tryResp.
        .flatMap { _ =>
          // Если ошибка запроса, то залить её в состояние
          for (ex <- m.tryResp.toEither.left) yield {
            val v2 = respHandler.handleReqError(ex, rhCtx0)
            updated(v2)
          }
        }
        // Если запрос ок, то значит пора выполнить свёрстку respActions на состояние и эффекты
        .map { scResp =>
          val acc9 = scResp.respActions.foldLeft( RaFoldAcc() ) { (acc0, ra) =>
            val rhCtx1 = rhCtx0.copy(
              value0 = acc0.v1
            )
            respWithActionHandlers
              .find { rah =>
                rah.isMyRespAction( ra.acType, rhCtx0 )
              }
              .map { rah =>
                rah.applyRespAction( ra, rhCtx1 )
              }
              .fold {
                // Resp-экшен не поддерживается системой. Такое возможно, только когда есть тип экшена, для которого забыли накодить RespActionHandler.
                LOG.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = ra )
                acc0
              } { case (v2, fxOpt) =>
                acc0.copy(
                  v1 = v2,
                  fxAccRev = fxOpt.prependTo( acc0.fxAccRev )
                )
              }
          }

          val fxOpt9 = acc9.fxAccRev
            .reverse
            .mergeEffects
          ah.updatedMaybeEffect( acc9.v1, fxOpt9 )
        }
        // Вернуть Left или Right.
        .fold(identity, identity)


    // Если юзер активно тыкал пальцем по экрану, то таймер сокрытия мог сработать после окончания приветствия.
    case _: WcTimeOut =>
      noChange

    // Обработка всегда-игнорируемых событий, но которые зачем-то нужны.
    case DoNothing =>
      noChange

  }

  private def _removeTimer(v0: MScRoot = value): MScRoot = {
    v0.withInternals(
      v0.internals.withGeoLockTimer( None )
    )
  }

  private def getIndexFx(geoIntoRcvr: Boolean, focusedAdId: Option[String] = None, retUserLoc: Boolean = false): Effect = {
    Effect.action(
      GetIndex(withWelcome = true, geoIntoRcvr = geoIntoRcvr, focusedAdId = focusedAdId, retUserLoc = retUserLoc) )
  }

  private def geoOffFx: Effect =
    Effect.action( GeoLocOnOff(enabled = false) )

  private def withMapCenterInitReal(center: MGeoPoint, inx: MScIndex): MScIndex = {
    inx.withSearch(
      inx.search.withGeo(
        inx.search.geo.withMapInit(
          inx.search.geo.mapInit
            .withState(
              inx.search.geo.mapInit.state
                .withCenterInitReal( center )
            )
        )
      )
    )
  }

}
