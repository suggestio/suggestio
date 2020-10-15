package io.suggest.sc.c

import cordova.plugins.appminimize.CdvAppMinimize
import diode._
import io.suggest.ble.beaconer.{BtOnOff, MBeaconerOpts}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.ScConstants
import io.suggest.sc.m._
import io.suggest.sc.m.grid._
import io.suggest.sc.m.inx._
import io.suggest.sc.m.search.{MGeoTabS, MMapInitState, MNodesFoundS, MScSearch, MSearchCssProps, MSearchRespInfo, NodeRowClick}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.ueq.UnivEqUtil._
import io.suggest.common.coll.Lists.Implicits._
import io.suggest.geo.GeoLocUtilJs
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.react.r.ComponentCatch
import io.suggest.sc.index.{MSc3IndexResp, MScIndexArgs}
import io.suggest.sc.m.dia.InitFirstRunWz
import io.suggest.sc.m.in.{MInternalInfo, MJsRouterS, MScInternals}
import io.suggest.sc.m.inx.save.{MIndexInfo, MIndexesRecent, MIndexesRecentOuter}
import io.suggest.sc.m.menu.DlAppOpen
import io.suggest.sc.v.search.SearchCss
import io.suggest.sjs.dom2.DomQuick
import japgolly.univeq._
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.{DAction, DoNothing, SioPages}
import japgolly.scalajs.react.extra.router.RouterCtl
import org.scalajs.dom

import scala.util.Try

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
  def getMainScreenSnapShot(v0: MScRoot): SioPages.Sc3 = {
    val inxState = v0.index.state
    val searchOpened = v0.index.search.panel.opened

    // Если ничего в view пока не загружено, то стараемся поддерживать исходные значения
    val bootingRoute = v0.internals.info.currRoute
      .filter( _ => inxState.viewCurrent.isEmpty )
    val currRcvrId = bootingRoute.fold(inxState.rcvrId)(_.nodeId)

    // TODO Поддержка нескольких тегов в URL.
    val selTagIdOpt = v0.index.search.geo.data.selTagIds.headOption

    SioPages.Sc3(
      nodeId        = currRcvrId,
      // Не рендерить координаты в URL, если находишься в контексте узла, закрыта панель поиска и нет выбранного тега.
      // Это улучшит кэширование, возможно улучшит приватность при обмене ссылками.
      locEnv        = OptionUtil.maybe {
        (currRcvrId.isEmpty || searchOpened || selTagIdOpt.nonEmpty) &&
        (v0.internals.boot.wzFirstDone contains[Boolean] true) &&
        v0.internals.info.geoLockTimer.isEmpty
      } {
        bootingRoute
          .flatMap(_.locEnv)
          .getOrElse( v0.index.search.geo.mapInit.state.center )
      },
      generation    = Some( inxState.generation ),
      searchOpened  = searchOpened,
      tagNodeId     = selTagIdOpt,
      menuOpened    = v0.index.menu.opened,
      focusedAdId   = for {
        scAdData <- v0.grid.core.focusedAdOpt
        if scAdData.focused.nonEmpty
        adNodeId <- scAdData.nodeId
      } yield {
        adNodeId
      },
      firstRunOpen = v0.dialogs.first.view.nonEmpty,
      dlAppOpen    = v0.index.menu.dlApp.opened,
      settingsOpen = v0.dialogs.settings.opened,
      login = for {
        loginCircuit <- v0.dialogs.login.ident
      } yield {
        loginCircuit.currentPage()
      },

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
      .composeLens( MScSearch.geo )
      .composeLens( MGeoTabS.mapInit )
  }

  private def getIndexFx( switchCtx: MScSwitchCtx ): Effect = {
    GetIndex( switchCtx )
      .toEffectPure
  }

  private def _currRoute = {
    MScRoot.internals
      .composeLens( MScInternals.info )
      .composeLens( MInternalInfo.currRoute )
  }


  private def root_internals_info_inxRecentsPrev_LENS = {
    MScRoot.internals
      .composeLens( MScInternals.info )
      .composeLens( MInternalInfo.inxRecents )
  }

  private def recents2searchCssModF(inxRecents: MIndexesRecent) = {
    MIndexesRecentOuter.searchCss
      .composeLens( SearchCss.args )
      .composeLens( MSearchCssProps.nodesFound )
      .composeLens( MNodesFoundS.req )
      .modify( _.ready(
        MSearchRespInfo(
          resp = MGeoNodesResp(
            nodes = for (inxInfo <- inxRecents.recents) yield {
              MGeoNodePropsShapes(
                props = inxInfo.indexResp,
              )
            }
          ),
        )
      ))
  }

}


/** Непосредственный контроллер "последних" сообщений. */
class TailAh(
              routerCtl                : RouterCtl[SioPages.Sc3],
              modelRW                  : ModelRW[MScRoot, MScRoot],
              scRespHandlers           : Seq[IRespHandler],
              scRespActionHandlers     : Seq[IRespActionHandler],
            )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[MScRoot]] = {

    // Заставить роутер собрать новую ссылку.
    case m: ResetUrlRoute =>
      val v0 = value
      def nextRoute0 = TailAh.getMainScreenSnapShot( v0 )
      var nextRoute2 = m.mods.fold(nextRoute0)( _(() => nextRoute0) )

      val currRouteLens = TailAh._currRoute
      val currRouteOpt = currRouteLens.get( v0 )

      if (!m.force && (currRouteOpt contains[SioPages.Sc3] nextRoute2)) {
        noChange
      } else {
        // Скопировать URL-поле virtBeacons, если в рамках одного местоположения:
        for {
          currRoute <- currRouteOpt
          if currRoute.virtBeacons.nonEmpty &&
            (currRoute isSamePlaceAs nextRoute2)
        }
          nextRoute2 = (SioPages.Sc3.virtBeacons set currRoute.virtBeacons)(nextRoute2)

        // Уведомить в фоне роутер, заодно разблокировав интерфейс.
        val fx = Effect.action {
          routerCtl
            .set( nextRoute2 )
            .runNow()
          DoNothing
        }

        val v2 = (currRouteLens set Some(nextRoute2))( v0 )
        updatedSilent(v2, fx)
      }


    // Запуск чтения сохранённых недавно-посещённых узлов.
    case m: LoadIndexRecents =>
      val v0 = value

      val outerLens = TailAh.root_internals_info_inxRecentsPrev_LENS

      if (m.clean) {
        // Завернуть в эффект?
        val readTryRes = Try( MIndexesRecent.get() )
          .map( _ getOrElse MIndexesRecent.empty )

        var infoModF = MIndexesRecentOuter.saved.modify(
          _.withTry(
            Try( MIndexesRecent.get() )
              .map( _ getOrElse MIndexesRecent.empty )
          )
        )

        for (recent2 <- readTryRes)
          infoModF = infoModF andThen TailAh.recents2searchCssModF( recent2 )

        val v2 = outerLens.modify( infoModF )(v0)
        updated(v2)

      } else {
        // Просто пересборка инстанса, чтобы освежить какие-то lazy-val'ы.
        val v2 = outerLens.modify( identity )(v0)
        updated(v2)
      }


    // Сохранить инфу по индексу.
    case m: SaveRecentIndex =>
      val v0 = value

      m.inxRecent2.fold {
        // Эффект, если есть роута и что-то изменилось.
        val fxOpt = for {
          currRoute       <- v0.internals.info.currRoute
          currIndexResp   <- v0.index.respOpt
        } yield {
          Effect.action {
            // Получить на руки текущее сохранённое состояние:
            val recentOpt0 = v0.internals.info.indexesRecents.saved.toOption orElse {
              val tryRes = Try {
                MIndexesRecent.get()
              }
              for (ex <- tryRes.failed)
                logger.warn( ErrorMsgs.KV_STORAGE_ACTION_FAILED, ex )
              tryRes
                .toOption
                .flatten
            }

            // Сборка сохраняемого инстанса MIndexInfo.
            val inxInfo2 = MIndexInfo(
              state         = currRoute,
              indexResp     = {
                if (currIndexResp.geoPoint.nonEmpty) {
                  currIndexResp
                } else {
                  // Если нет гео-точки узла, то передрать его с гео-карты.
                  (
                    MSc3IndexResp.geoPoint set Some(v0.index.search.geo.mapInit.state.center)
                  )(currIndexResp)
                }
              },
            )

            // Сохранить/обновить сохранённое состояние.
            val recentOpt2 = recentOpt0.fold [Option[MIndexesRecent]] {
              val ir = MIndexesRecent( inxInfo2 :: Nil )
              Some(ir)
            } { recents0 =>
              val isDuplicate = recents0.recents.exists { m =>
                (m.state isSamePlaceAs currRoute)
              }

              Option.when(!isDuplicate) {
                val maxItems = MIndexesRecent.MAX_RECENT_ITEMS
                MIndexesRecent.recents.modify { r0 =>
                  // Удалять из списка старые значения, которые isLookingSame.
                  val r1 = r0.filterNot(_.indexResp isLookingSame inxInfo2.indexResp)
                  // Укоротить список по максимальной длине.
                  val r2 =
                    if (r1.lengthIs > maxItems)  r1.take( maxItems - 1 )
                    else  r1
                  // И наконец добавить новый элемент.
                  inxInfo2 :: r2
                } (recents0)
              }
            }

            recentOpt2.fold[DAction] {
              DoNothing
            } { recents2 =>
              // Записать в постоянное хранилище
              for {
                ex <- Try( MIndexesRecent.save( recents2 ) ).failed
              }
                logger.warn( ErrorMsgs.KV_STORAGE_ACTION_FAILED, ex )

              SaveRecentIndex( recentOpt2 )
            }
          }
        }

        fxOpt.fold (noChange) { fx =>
          val v2 = TailAh.root_internals_info_inxRecentsPrev_LENS
            .composeLens( MIndexesRecentOuter.saved )
            .modify( _.pending() )( v0 )
          updatedSilent(v2, fx)
        }

      } { inxRecent2 =>
        val v2 = TailAh.root_internals_info_inxRecentsPrev_LENS.modify(
          MIndexesRecentOuter.saved
            .modify( _.ready(inxRecent2) ) andThen
          TailAh.recents2searchCssModF( inxRecent2 )
        )(v0)
        // не silent, т.к. подразумевается рендер некоторых частей списка в левой менюшке.
        updated( v2 )
      }


    // Сигнал выбора узла в списке ранее просмотренных.
    case m: IndexRecentNodeClick =>
      val v0 = value

      val lens = TailAh.root_internals_info_inxRecentsPrev_LENS
        .composeLens( MIndexesRecentOuter.saved )

      // Переход в ранее-посещённую локацию: найти в recent-списке предшествующее состояние выдачи.
      lens.get( v0 )
        .iterator
        .flatMap(_.recents)
        .find(_.indexResp ===* m.inxRecent)
        .fold {
          logger.warn( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange

        } { inxRecent =>
          // Клик по узлу - восстановить состояние выдачи.
          val routeFx = Effect.action {
            // Патчим текущую роуту данными из новой роуты.
            var nextRoute = TailAh._currRoute
              .get(v0)
              .fold( inxRecent.state )( _ silentSwitchingInto inxRecent.state )

            // Если мобильный экран, то надо сразу скрыть раскрытую панель меню:
            if (nextRoute.menuOpened && v0.grid.core.jdConf.gridColumnsCount <= 3)
              nextRoute = (SioPages.Sc3.menuOpened set false)(nextRoute)

            ResetUrlRoute( mods = Some(_ => nextRoute) )
          }

          effectOnly( routeFx )
        }


    // Сигнал нажатия хардварной кнопки back (приложение).
    case HwBack =>
      val v0 = value

      // cordova (android): при нажатии back надо скрыть открытые панели или диалоги, иначе свернуть приложение в фон.
      val plat = v0.dev.platform
      if (
        plat.isCordova &&
        // TODO m.RouteTo.isBack - нажата кнопка Back?
        TailAh._currRoute
          .get(v0)
          .exists { m => !m.isSomeThingOpened }
      ) {
        if (plat.isUsingNow && plat.isReady) {
          val minimizeFx = Effect.action {
            CdvAppMinimize.minimize()
            DoNothing
          }
          effectOnly( minimizeFx )

        } else {
          noChange
        }

      } else {
        val goBackFx = Effect.action {
          dom.window.history.back()
          DoNothing
        }
        effectOnly( goBackFx )
      }


    // SPA-роутер заливает в состояние данные из URL.
    case m: RouteTo =>
      val v0 = value

      // Аккамулятор функций, обновляющих список модов, которые будут наложены на v0.
      var modsAcc = List.empty[MScRoot => MScRoot]

      // Возможно js-роутер ещё не готов, и нужно отложить полную обработку состояния:
      val isFullyReady = v0.internals.jsRouter.jsRouter.isReady &&
        (v0.internals.boot.wzFirstDone.getOrElseTrue /*isEmpty || contains[Boolean] true*/)

      // Надо ли повторно отрабатывать m после того, как js-роутер станет готов?
      var jsRouterAwaitRoute = false

      // Текущее значение MainScreen:
      val currMainScreen = TailAh.getMainScreenSnapShot(v0)

      var isToReloadIndex = isFullyReady && v0.index.resp.isTotallyEmpty

      var needUpdateUi = false

      var isGeoLocRunning = v0.internals.info.geoLockTimer.nonEmpty

      var isGridNeedsReload = false

      // Аккамулятор результирующих эффектов:
      var fxsAcc = List.empty[Effect]

      // Сохранить присланную роуту в состояние, если роута изменилась:
      val currRouteLens = TailAh._currRoute
      if ( !(currRouteLens.get(v0) contains[SioPages.Sc3] m.mainScreen) )
        modsAcc ::= currRouteLens.set( Some(m.mainScreen) )

      // Проверка поля generation
      for {
        generation2 <- m.mainScreen.generation
        if !(currMainScreen.generation contains[Long] generation2)
      } {
        modsAcc ::= MScRoot.index
          .composeLens(MScIndex.state)
          .composeLens(MScIndexState.generation)
          .set(generation2)
        // generation не совпадает. Надо будет перезагрузить плитку.
        if (isFullyReady) {
          val adsPot = v0.grid.core.ads
          if (adsPot.isPending || adsPot.exists(_.nonEmpty))
            isGridNeedsReload = true
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Проверка поля searchOpened
      if (m.mainScreen.searchOpened !=* currMainScreen.searchOpened)
        fxsAcc ::= SideBarOpenClose( MScSideBars.Search, OptionUtil.SomeBool(m.mainScreen.searchOpened) ).toEffectPure

      // Проверка id нового узла.
      if (m.mainScreen.nodeId !=* currMainScreen.nodeId) {
        if (isFullyReady) {
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
        if (isFullyReady) {
          needUpdateUi = true
          isToReloadIndex = true
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
        if !(currMainScreen.tagNodeId contains[String] tagNodeId)
      } {
        // Имитируем клик по тегу, да и всё.
        if (isFullyReady)
          fxsAcc ::= NodeRowClick( tagNodeId ).toEffectPure
        else
          jsRouterAwaitRoute = true
      }

      // Сверить панель меню открыта или закрыта
      if (m.mainScreen.menuOpened !=* currMainScreen.menuOpened)
        fxsAcc ::= SideBarOpenClose( MScSideBars.Menu, OptionUtil.SomeBool(m.mainScreen.menuOpened) ).toEffectPure

      // Сверить focused ad id:
      if (
        (m.mainScreen.focusedAdId !=* currMainScreen.focusedAdId) &&
        !isToReloadIndex
      ) {
        if (isFullyReady) {
          for {
            focusedAdId <- m.mainScreen.focusedAdId orElse currMainScreen.focusedAdId
          } {
            fxsAcc ::= GridBlockClick(nodeId = focusedAdId).toEffectPure
          }
        } else {
          jsRouterAwaitRoute = true
        }
      }

      if (m.mainScreen.dlAppOpen !=* currMainScreen.dlAppOpen)
        fxsAcc ::= DlAppOpen( opened = m.mainScreen.dlAppOpen ).toEffectPure

      if (m.mainScreen.settingsOpen !=* currMainScreen.settingsOpen)
        fxsAcc ::= SettingsDiaOpen( opened = m.mainScreen.settingsOpen ).toEffectPure

      // Если в состоянии заданы виртуальные маячки, то нужно перезагрузить плитку.
      if (m.mainScreen.virtBeacons !=* currMainScreen.virtBeacons)
        isGridNeedsReload = true

      // Если нет гео-точки и нет nodeId, то требуется активировать геолокацию
      // (кроме случаев активности wzFirst-диалога: при запуске надо влезть до полного завершения boot-сервиса, но после закрытия диалога)
      if (m.mainScreen.needGeoLoc &&
        v0.internals.boot.wzFirstDone.nonEmpty &&
        v0.dialogs.first.view.isEmpty
      ) {
        // Если геолокация ещё не запущена, то запустить:
        if (
          GeoLocUtilJs.envHasGeoLoc() &&
          !(v0.dev.geoLoc.switch.onOff contains[Boolean] true) &&
          !isGeoLocRunning
        ) {
          fxsAcc ::= GeoLocOnOff(enabled = true, isHard = false).toEffectPure
          isGeoLocRunning = true
        }

        // Если bluetooth не запущен - запустить в добавок к геолокации:
        if (v0.dev.platform.hasBle && !(v0.dev.beaconer.isEnabled contains[Boolean] true)) {
          fxsAcc ::= Effect.action {
            BtOnOff(
              isEnabled = true,
              opts = MBeaconerOpts(
                hardOff     = false,
                askEnableBt = false,
                oneShot     = false,
              )
            )
          }
        }

        // Если таймер геолокации не запущен, то запустить:
        if (v0.internals.info.geoLockTimer.isEmpty) {
          val switchCtx = MScSwitchCtx(
            indexQsArgs = MScIndexArgs(
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
      if (currMainScreen.firstRunOpen !=* m.mainScreen.firstRunOpen)
        // Запустить экшен управления диалогом.
        fxsAcc ::= InitFirstRunWz( m.mainScreen.firstRunOpen ).toEffectPure

      // Диалог логина открыт? Открыть, если требуется.
      // Надо посмотреть, залогинен ли юзер сейчас или нет, и в контексте этого разбирать новое значение.
      val login2 = m.mainScreen.login
        .filter(_ => !v0.index.isLoggedIn)
      if (currMainScreen.login !=* login2)
        fxsAcc ::= ScLoginFormChange( login2 ).toEffectPure

      // Обновлённое состояние, которое может быть и не обновлялось:
      val v2Opt = modsAcc
        .reduceOption(_ andThen _)
        .map(_(v0))

      // Принять решение о перезагрузке выдачи, если необходимо.
      if (isToReloadIndex && !isGeoLocRunning) {
        // Целиковая перезагрузка выдачи.
        val switchCtx = MScSwitchCtx(
          showWelcome = m.mainScreen.showWelcome,
          indexQsArgs = MScIndexArgs(
            geoIntoRcvr = false,
            retUserLoc  = v0.index.search.geo.mapInit.userLoc.isEmpty,
            nodeId      = m.mainScreen.nodeId,
          ),
          focusedAdId = m.mainScreen.focusedAdId,
        )
        fxsAcc ::= TailAh.getIndexFx( switchCtx ) >> LoadIndexRecents(clean = false).toEffectPure

      } else if (isGridNeedsReload) {
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
              )
            )
          }
          val fxs = TailAh.getIndexFx( switchCtx ) + Effect.action {
            DomQuick.clearTimeout(geoLockTimerId)
            DoNothing
          }
          val (v22, fxs2) = TailAh._removeTimer(v00)
            .fold( (v00, fxs) ) { case (alterF, ctFx) =>
              alterF(v00) -> (fxs + ctFx)
            }
          updatedSilent(v22, fxs2)
        }

        m.origOpt
          .flatMap(_.either.toOption)
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
        if (!m.switchCtx.demandLocTest) {
          fxsAcc ::= TailAh.getIndexFx( m.switchCtx )
        }

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

      val rhCtx0 = MRhCtx(value0, m, modelRW)
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
      (for {
        _ <- Either.cond(
          test = isActualResp,
          left = {
            logger.log(ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (respHandler.getClass.getSimpleName, rhPotOpt.flatMap(_.pendingOpt).map(_.startTime), m.reqTimeStamp) )
            noChange
          },
          right = None,
        )

        // Раскрыть содержимое tryResp.
        scResp <- {
          // Если ошибка запроса, то залить её в состояние
          for (ex <- m.tryResp.toEither.left) yield {
            // Костыль, чтобы безопасно скастовать MScRoot и M. На деле происходит просто копипаст инстанса, т.к. всегда M == MScRoot.
            //ActionResult( ares.newModelOpt.map(modelRW.updated), ares.effectOpt )
            respHandler.handleReqError(ex, rhCtx0)
          }
        }

      } yield {
        val acc9 = scResp.respActions.foldLeft( RaFoldAcc() ) { (acc0, ra) =>
          val rhCtx1 = (MRhCtx.value0 set acc0.v1)(rhCtx0)
          scRespActionHandlers
            .find { rah =>
              rah.isMyRespAction( ra.acType, rhCtx0 )
            }
            .map { rah =>
              rah.applyRespAction( ra, rhCtx1 )
            }
            .fold {
              // Resp-экшен не поддерживается системой. Такое возможно, только когда есть тип экшена, для которого забыли накодить RespActionHandler.
              logger.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = ra )
              acc0
            } { actRes =>
              acc0.copy(
                v1        = actRes.newModelOpt getOrElse acc0.v1,
                fxAccRev  = actRes.effectOpt :?: acc0.fxAccRev,
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
      })
        // Вернуть содержимое Left или Right.
        .fold(identity, identity)


    // Если юзер активно тыкал пальцем по экрану, то таймер сокрытия мог сработать после окончания приветствия.
    case _: WcTimeOut =>
      noChange


    // Ошибка от компонента.
    case m: ComponentCatch =>
      logger.error( ErrorMsgs.CATCHED_CONSTRUCTOR_EXCEPTION, msg = m )
      // TODO Отрендерить ошибку на экран.
      noChange

  }

}
