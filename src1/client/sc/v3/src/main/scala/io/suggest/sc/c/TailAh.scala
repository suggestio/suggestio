package io.suggest.sc.c

import diode._
import io.suggest.ble.beaconer.m.BtOnOff
import io.suggest.common.empty.OptionUtil
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sc.ScConstants
import io.suggest.sc.m._
import io.suggest.sc.m.grid._
import io.suggest.sc.m.inx._
import io.suggest.sc.m.search.{MGeoTabS, MMapInitState, MScSearch, NodeRowClick}
import io.suggest.sc.sc3.Sc3Pages.MainScreen
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.ueq.UnivEqUtil._
import io.suggest.common.coll.Lists.Implicits.OptionListExtOps
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.dia.InitFirstRunWz
import io.suggest.sc.m.in.{MInternalInfo, MJsRouterS, MScInternals}
import io.suggest.sc.sc3.Sc3Pages
import io.suggest.sjs.dom.DomQuick
import japgolly.univeq._
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import japgolly.scalajs.react.extra.router.RouterCtl

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
    val searchOpened = v0.index.search.panel.opened
    val currRcvrId = inxState.rcvrId

    // TODO Поддержка нескольких тегов в URL.
    val selTagIdOpt = v0.index.search.geo.data.selTagIds.headOption

    MainScreen(
      nodeId        = currRcvrId,
      // Не рендерить координаты в URL, если находишься в контексте узла, закрыта панель поиска и нет выбранного тега.
      // Это улучшит кэширование, возможно улучшит приватность при обмене ссылками.
      locEnv        = OptionUtil.maybe {
        currRcvrId.isEmpty || searchOpened || selTagIdOpt.nonEmpty
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
      },
      firstRunOpen = v0.dialogs.first.view.nonEmpty,
    )
  }


  private def _removeTimer(v0: MScRoot) = {
    for (gltId <- v0.internals.info.geoLockTimer) yield {
      val alterF = MScRoot.internals
        .composeLens( MScInternals.info )
        .composeLens( MInternalInfo.geoLockTimer )
        .set(None)
      val fx = _clearTimerFx( gltId )
      (alterF, fx)
    }
  }

  private def _clearTimerFx(timerId: Int): Effect = {
    Effect.action {
      DomQuick.clearTimeout( timerId )
      DoNothing
    }
  }

  /** Сборка таймера геолокации.
    *
    * @param v0 Состояние MScInternals.
    * @return Обновлённое состояние + эффект ожидания срабатывания таймера.
    *         Но таймер - уже запущен к этому моменту.
    */
  def mkGeoLocTimer(switchCtx: MScSwitchCtx, currTimerIdOpt: Option[Int]): (MScInternals => MScInternals, Effect) = {
    val tp = DomQuick.timeoutPromiseT( ScConstants.ScGeo.INIT_GEO_LOC_TIMEOUT_MS )( GeoLocTimeOut(switchCtx) )
    val modifier = MScInternals.info
      .composeLens(MInternalInfo.geoLockTimer)
      .set( Some( tp.timerId ) )

    var timeoutFx: Effect = Effect( tp.fut )
    for (currTimerId <- currTimerIdOpt) {
      timeoutFx += _clearTimerFx(currTimerId)
    }

    (modifier, timeoutFx)
  }

  private def _inxSearchGeoMapInitLens = {
    MScIndex.search
      .composeLens( MScSearch.geo)
      .composeLens( MGeoTabS.mapInit )
  }

  /* Было: (geoIntoRcvr: Boolean, focusedAdId: Option[String] = None, retUserLoc: Boolean = false) */
  private def getIndexFx( switchCtx: MScSwitchCtx ): Effect = {
    GetIndex( switchCtx )
      .toEffectPure
  }

  private def _currRoute = {
    MScRoot.internals
      .composeLens( MScInternals.info )
      .composeLens( MInternalInfo.currRoute )
  }

}


/** Непосредственный контроллер "последних" сообщений. */
class TailAh[M](
                 routerCtl                : RouterCtl[Sc3Pages],
                 modelRW                  : ModelRW[M, MScRoot],
                 scRespHandlers           : Seq[IRespHandler],
                 scRespActionHandlers     : Seq[IRespActionHandler],
               )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Заставить роутер собрать новую ссылку.
    case ResetUrlRoute =>
      val v0 = value
      val m = TailAh.getMainScreenSnapShot( v0 )
      // Уведомить в фоне роутер, заодно разблокировав интерфейс.
      val fx = Effect {
        routerCtl
          .set( m )
          .toFuture
          .map(_ => DoNothing)
      }
      val currRouteLens = TailAh._currRoute
      val m0 = currRouteLens.get( v0 )
      if ( m0 contains m ) {
        // TODO Может вообще тут делать ничего не надо?
        effectOnly(fx)
      } else {
        val v2 = currRouteLens.set( Some(m) )( v0 )
        updatedSilent(v2, fx)
      }


    // SPA-роутер заливает в состояние данные из URL.
    case m: RouteTo =>
      //println(m)
      // TODO Возможно, этот код управления должен жить прямо в роутере?
      val v0 = value

      // Аккамулятор функций, обновляющих список модов, которые будут наложены на v0.
      var modsAcc = List.empty[MScRoot => MScRoot]

      // Возможно js-роутер ещё не готов, и нужно отложить полную обработку состояния:
      val isJsRouterReady = v0.internals.jsRouter.jsRouter.isReady

      // Надо ли повторно отрабатывать m после того, как js-роутер станет готов?
      var jsRouterAwaitRoute = false

      // Текущее значение MainScreen:
      val currMainScreen = TailAh.getMainScreenSnapShot(v0)

      // Считаем, что js-роутер уже готов. Если нет, то это сообщение должно было быть перехвачено в JsRouterInitAh.
      var isToReloadIndex = isJsRouterReady && v0.index.resp.isTotallyEmpty && v0.internals.boot.wzFirstDone.contains(true)
      var needUpdateUi = false

      var isGeoLocRunning = v0.internals.info.geoLockTimer.nonEmpty

      var isGridNeedsReload = false

      // Аккамулятор результирующих эффектов:
      var fxsAcc = List.empty[Effect]

      // Сохранить присланную роуту в состояние, если роута изменилась:
      val currRouteLens = TailAh._currRoute
      if ( !(currRouteLens.get(v0) contains m) )
        modsAcc ::= currRouteLens.set( Some(m.mainScreen) )

      // Проверка поля generation
      for {
        generation2 <- m.mainScreen.generation
        if !(currMainScreen.generation contains generation2)
      } {
        modsAcc ::= MScRoot.index
          .composeLens(MScIndex.state)
          .composeLens(MScIndexState.generation)
          .set(generation2)
        // generation не совпадает. Надо будет перезагрузить плитку.
        if (isJsRouterReady) {
          val adsPot = v0.grid.core.ads
          if (adsPot.isPending || adsPot.exists(_.nonEmpty))
            isGridNeedsReload = true
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Проверка поля searchOpened
      if (m.mainScreen.searchOpened !=* currMainScreen.searchOpened)
        fxsAcc ::= SideBarOpenClose(MScSideBars.Search, m.mainScreen.searchOpened).toEffectPure

      // Проверка id нового узла.
      if (m.mainScreen.nodeId !=* currMainScreen.nodeId) {
        if (isJsRouterReady) {
          isToReloadIndex = true
          needUpdateUi = true
          // nodeId будет передан через экшен.
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Смотрим координаты текущей точки.
      if (
        // Если состояние гео-точки хоть немного изменилось:
        !(
          (m.mainScreen.locEnv.isEmpty && currMainScreen.locEnv.isEmpty) ||
          m.mainScreen.locEnv.exists { mgp2 =>
            currMainScreen.locEnv.exists { mgp0 =>
              mgp0 ~= mgp2
            }
          }
        )
      ) {
        if (isJsRouterReady) {
          needUpdateUi = true
          for (nextGeoPoint <- m.mainScreen.locEnv) {
            modsAcc ::= MScRoot.index
              .composeLens( TailAh._inxSearchGeoMapInitLens )
              .composeLens( MMapInitState.state )
              .modify {
                _.withCenterInitReal( nextGeoPoint )
              }
          }
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Смотрим текущий выделенный тег
      for {
        tagNodeId <- m.mainScreen.tagNodeId
        if !(currMainScreen.tagNodeId contains tagNodeId)
      } {
        // Имитируем клик по тегу, да и всё.
        if (isJsRouterReady)
          fxsAcc ::= NodeRowClick( tagNodeId ).toEffectPure
        else
          jsRouterAwaitRoute = true
      }

      // Сверить панель меню открыта или закрыта
      if (m.mainScreen.menuOpened !=* currMainScreen.menuOpened)
        fxsAcc ::= SideBarOpenClose(MScSideBars.Menu, m.mainScreen.menuOpened).toEffectPure

      // Сверить focused ad id:
      if (
        m.mainScreen.focusedAdId !=* currMainScreen.focusedAdId &&
        !isToReloadIndex
      ) {
        if (isJsRouterReady) {
          for {
            focusedAdId <- m.mainScreen.focusedAdId orElse currMainScreen.focusedAdId
          } {
            fxsAcc ::= GridBlockClick(nodeId = focusedAdId).toEffectPure
          }
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Если нет гео-точки и нет nodeId, то требуется активировать геолокацию:
      if (m.mainScreen.needGeoLoc && !v0.index.isFirstRun) {
        //println("TailAh.RouteTo: need GEO LOC")
        // Если геолокация ещё не запущена, то запустить:
        if (v0.dev.platform.hasGeoLoc && !(v0.dev.geoLoc.switch.onOff contains true) && !isGeoLocRunning) {
          fxsAcc ::= GeoLocOnOff(enabled = true, isHard = false).toEffectPure
          isGeoLocRunning = true
        }

        // Если bluetooth не запущен - запустить в добавок к геолокации:
        if (v0.dev.platform.hasBle && !(v0.dev.beaconer.isEnabled contains true))
          fxsAcc ::= BtOnOff(isEnabled = true, hard = false).toEffectPure

        // Если таймер геолокации не запущен, то запустить:
        if (v0.internals.info.geoLockTimer.isEmpty) {
          val switchCtx = MScSwitchCtx(
            indexQsArgs = MScIndexArgs(
              withWelcome = true,
              geoIntoRcvr = true,
              retUserLoc  = false,
            )
          )
          val (viMod, fx) = TailAh.mkGeoLocTimer( switchCtx, v0.internals.info.geoLockTimer )
          modsAcc ::= MScRoot.internals.modify(viMod)
          fxsAcc ::= fx
        }
      }

      // Запихнуть в состояние текущий экшен для запуска позднее, если требуется:
      for {
        awaitRouteOpt <- {
          // Выставить роуту
          if (jsRouterAwaitRoute)
            Some(Some(m))
          // Сбросить ранее выставленную роуту
          else if (v0.internals.jsRouter.delayedRouteTo.nonEmpty)
            Some(None)
          // Ничего менять в состоянии delayed-роуты не надо
          else None
        }
      } {
        modsAcc ::= MScRoot.internals
          .composeLens( MScInternals.jsRouter )
          .composeLens( MJsRouterS.delayedRouteTo )
          .set( awaitRouteOpt )
      }

      // Диалог first-run открыт?
      if (currMainScreen.firstRunOpen !=* m.mainScreen.firstRunOpen) {
        // Запустить экшен управления диалогом.
        fxsAcc ::= Effect.action(
          InitFirstRunWz( currMainScreen.firstRunOpen )
        )
      }

      // Обновлённое состояние, которое может быть и не обновлялось:
      val v2Opt = modsAcc.reduceOption(_ andThen _).map(_(v0))

      // Принять решение о перезагрузке выдачи, если необходимо.
      if (isToReloadIndex && !isGeoLocRunning) {
        //println( "reload index" )
        // Целиковая перезагрузка выдачи.
        val switchCtx = MScSwitchCtx(
          indexQsArgs = MScIndexArgs(
            geoIntoRcvr = false,
            retUserLoc  = v0.index.search.geo.mapInit.userLoc.isEmpty,
            withWelcome = true,
            nodeId      = m.mainScreen.nodeId,
          ),
          focusedAdId = m.mainScreen.focusedAdId,
        )
        fxsAcc ::= TailAh.getIndexFx( switchCtx )
      } else if (isGridNeedsReload)  {
        // Узел не требует обновления, но требуется перезагрузка плитки.
        fxsAcc ::= GridLoadAds( clean = true, ignorePending = true ).toEffectPure
      }

      ah.optionalResult(v2Opt, fxsAcc.mergeEffects, silent = needUpdateUi)


    // Сигнал наступления геолокации (или ошибки геолокации).
    case m: GlPubSignal =>
      val v0 = value

      // Сейчас ожидаем максимально точных координат?
      v0.internals.info.geoLockTimer.fold {
        // Сейчас не ожидаются координаты. Просто сохранить координаты в состояние карты.
        m.origOpt
          .flatMap(_.locationOpt)
          // TODO Opt: iphone шлёт кучу одинаковых или похожих координат, раз в 1-2 секунды. Надо это фильтровать?
          .fold( noChange ) { geoLoc =>
            // Не двигать карту, сохранять координаты только в .userLoc
            val v2 = MScRoot.index
              .composeLens( TailAh._inxSearchGeoMapInitLens )
              .composeLens( MMapInitState.userLoc )
              .set( Some(geoLoc) )(v0)
            ah.updateMaybeSilent( !v0.index.search.panel.opened )(v2)
          }

      } { geoLockTimerId =>
        // Прямо сейчас этот контроллер ожидает координаты.
        // Функция общего кода завершения ожидания координат: запустить выдачу, выключить geo loc, грохнуть таймер.
        def __finished(v00: MScRoot, isSuccess: Boolean) = {
          val switchCtx = m.scSwitch.getOrElse {
            MScSwitchCtx(
              indexQsArgs = MScIndexArgs(
                geoIntoRcvr = true,
                retUserLoc  = !isSuccess,
                withWelcome = true,
              )
            )
          }
          val fxs = TailAh.getIndexFx( switchCtx )
          DomQuick.clearTimeout(geoLockTimerId)
          val (v22, fxs2) = TailAh._removeTimer(v00)
            .fold( (v00, fxs) ) { case (alterF, ctFx) =>
              alterF(v00) -> (fxs + ctFx)
            }
          updatedSilent(v22, fxs2)
        }

        m.origOpt
          .flatMap(_.either.right.toOption)
          .fold {
            // Ожидаются координаты, но пришла ошибка. Можно ещё подождать, но пока считаем, что это конец.
            // Скорее всего, юзер отменил геолокацию или что-то ещё хуже.
            __finished(v0, isSuccess = false)
          } { geoLoc =>
            // Есть какие-то координаты, но не факт, что ожидаемо точные.
            // Т.к. работает suppressor, то координаты можно всегда записывать в состояние, не боясь постороннего "шума".
            val v1 = MScRoot.index
              .composeLens( TailAh._inxSearchGeoMapInitLens )
              .modify { mi0 =>
                // Текущая позиция юзера - всегда обновляется.
                var mi2 = MMapInitState.userLoc
                  .set( Some(geoLoc) )( mi0 )
                // Нельзя менять местоположение на карте, если это просто фоновое тестирование индекса.
                if (!m.scSwitch.exists(_.demandLocTest)) {
                  // Нормальное ожидаемое определение местоположения. Переместить карту в текущую точку.
                  mi2 = MMapInitState.state
                    .modify(_.withCenterInitReal( geoLoc.point ))(mi2)
                }
                mi2
              }(v0)

            if (m.origOpt.exists(_.glType.isHighAccuracy)) {
              // Пришли точные координаты. Завершаем ожидание.
              __finished(v1, isSuccess = true)
            } else {
              // Пока получены не точные координаты. Надо ещё подождать координат по-точнее...
              updatedSilent(v1)
            }
          }
      }


    // Наступил таймаут ожидания геолокации. Нужно активировать инициализацию в имеющемся состоянии
    case m: GeoLocTimeOut =>
      val v0 = value
      v0.internals.info.geoLockTimer.fold(noChange) { _ =>
        // Удалить из состояния таймер геолокации, запустить выдачу.
        var fxsAcc = List.empty[Effect]
        val v2 = TailAh._removeTimer(v0)
          .fold(v0) { case (alterF, ctFx) =>
            fxsAcc ::= ctFx
            alterF( v0 )
          }

        // Если demandLocTest, то и остановиться на этой ошибке:
        if (!m.switchCtx.demandLocTest)
          fxsAcc ::= TailAh.getIndexFx( m.switchCtx )

        ah.updatedSilentMaybeEffect(v2, fxsAcc.mergeEffects)
      }


    // Рукопашный запуск таймера геолокации.
    case m: GeoLocTimerStart =>
      val v0 = value
      val (viMod, fx) = TailAh.mkGeoLocTimer( m.switchCtx, v0.internals.info.geoLockTimer )
      val v2 = MScRoot.internals.modify(viMod)(v0)
      updated(v2, fx)


    // Объединённая обработка результатов API-запросов к серверу.
    // Результаты всех index, grid, focused, tags запросов - попадают только сюда.
    case m: HandleScApiResp =>
      val value0 = value

      val rhCtx0 = MRhCtx(value0, m)
      val respHandler = scRespHandlers
        .find { rh =>
          rh.isMyReqReason(rhCtx0)
        }
        .get

      // Класс сложного аккамулятора при свёрстке resp-экшенов:
      case class RaFoldAcc( v1         : MScRoot        = value0,
                            fxAccRev   : List[Effect]   = Nil
                          )

      // Надо сначала проверить timestamp, если он задан.
      val rhPotOpt = respHandler.getPot(rhCtx0)
      val isActualResp = m.reqTimeStamp.fold(true) { reqTimeStamp =>
        // Найти среди pot'ов состояния соответствие timestamp'у.
        rhPotOpt
          .exists(_ isPendingWithStartTime reqTimeStamp)
      }

      // Сборка сложной логики внутри Either: по левому борту ошибки, по правому - нормальный ход действий.
      Either
        .cond(
          isActualResp,
          left = {
            LOG.log(WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (respHandler.getClass.getSimpleName, rhPotOpt.flatMap(_.pendingOpt).map(_.startTime), m.reqTimeStamp) )
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
            scRespActionHandlers
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
              } { actRes =>
                acc0.copy(
                  v1        = actRes.newModelOpt getOrElse acc0.v1,
                  fxAccRev  = actRes.effectOpt prependTo acc0.fxAccRev
                )
              }
          }

          // Если value изменилась, то надо её обновлять:
          val v9Opt = OptionUtil.maybe(acc9.v1 !===* value0)( acc9.v1 )

          // Эффекты - объеденить:
          val fxOpt9 = acc9.fxAccRev
            .reverse
            .mergeEffects

          ah.optionalResult( v9Opt, fxOpt9 )
        }
        // Вернуть Left или Right.
        .fold(identity, identity)


    // Если юзер активно тыкал пальцем по экрану, то таймер сокрытия мог сработать после окончания приветствия.
    case _: WcTimeOut =>
      noChange

  }

}
