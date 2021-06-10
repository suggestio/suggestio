package io.suggest.sc.c

import cordova.plugins.appminimize.CdvAppMinimize
import diode._
import diode.data.Pot
import io.suggest.ble.api.IBleBeaconsApi
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
import io.suggest.lk.m.CsrfTokenEnsure
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.perm.BoolOptPermissionState
import io.suggest.react.r.ComponentCatch
import io.suggest.sc.c.dia.WzFirstDiaAh
import io.suggest.sc.index.{MIndexesRecentJs, MSc3IndexResp, MScIndexArgs, MScIndexInfo, MScIndexes}
import io.suggest.sc.m.boot.{Boot, BootAfter, MBootServiceIds}
import io.suggest.sc.m.dia.first.{InitFirstRunWz, MWzFrames, MWzPhases, WzPhasePermRes}
import io.suggest.sc.m.in.{MInternalInfo, MJsRouterS, MScInternals}
import io.suggest.sc.m.inx.save.MIndexesRecentOuter
import io.suggest.sc.m.menu.DlAppOpen
import io.suggest.sc.u.api.IScStuffApi
import io.suggest.sc.v.search.SearchCss
import io.suggest.sjs.dom2.DomQuick
import japgolly.univeq._
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.{DAction, DoNothing, HwBackBtn, SioPages}
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.scalajs.react.extra.router.RouterCtl
import org.scalajs.dom

import java.net.URI
import scala.util.{Success, Try}

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
    val route0 = v0.internals.info.currRoute
    val bootingRoute = route0
      .filter( _ => inxState.viewCurrent.isEmpty && v0.index.resp.isEmpty )
    val currRcvrId = bootingRoute.fold(inxState.rcvrId)(_.nodeId)

    // TODO Поддержка нескольких тегов в URL.
    val selTagIdOpt = v0.index.search.geo.data.selTagIds.headOption

    val locEnv2 = OptionUtil.maybeOpt {
      currRcvrId.isEmpty
      //(currRcvrId.isEmpty || searchOpened || selTagIdOpt.nonEmpty) &&
      //  (v0.internals.boot.wzFirstDone contains[Boolean] true) &&
      //  v0.internals.info.geoLockTimer.isEmpty
    } {
      bootingRoute
        .flatMap(_.locEnv)
        .orElse {
          Option.when( v0.internals.boot.wzFirstDone contains[Boolean] true )(
            v0.index.search.geo.mapInit.state.center
          )
        }
        .orElse {
          v0.dev.geoLoc
            .currentLocation
            .map(_._2.point)
        }
    }

    SioPages.Sc3(
      nodeId        = currRcvrId,
      // Не рендерить координаты в URL, если находишься в контексте узла, закрыта панель поиска и нет выбранного тега.
      // Это улучшит кэширование, возможно улучшит приватность при обмене ссылками.
      locEnv        = locEnv2,
      generation    = Some( inxState.generation ),
      searchOpened  = searchOpened,
      tagNodeId     = selTagIdOpt,
      menuOpened    = v0.index.menu.opened,
      focusedAdId   = for {
        scAdLoc   <- v0.grid.core.ads.interactAdOpt
        scAd      = scAdLoc.getLabel
        adData    <- scAd.data.toOption
        if adData.isOpened
        nodeId    <- adData.doc.tagId.nodeId
      } yield {
        nodeId
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

  private def recents2searchCssModF(inxRecents: MScIndexes) = {
    MIndexesRecentOuter.searchCss
      .composeLens( SearchCss.args )
      .composeLens( MSearchCssProps.nodesFound )
      .composeLens( MNodesFoundS.req )
      .modify( _.ready(
        MSearchRespInfo(
          resp = MGeoNodesResp(
            nodes = for (inxInfo <- inxRecents.indexes) yield {
              MGeoNodePropsShapes(
                props = inxInfo.indexResp,
              )
            }
          ),
        )
      ))
  }


  private def root_index_search_geo_init_state_LENS = MScRoot.index
    .composeLens( _inxSearchGeoMapInitLens )
    .composeLens( MMapInitState.state )

}


/** Непосредственный контроллер "последних" сообщений. */
class TailAh(
              routerCtl                : RouterCtl[SioPages.Sc3],
              modelRW                  : ModelRW[MScRoot, MScRoot],
              scRespHandlers           : Seq[IRespHandler],
              scRespActionHandlers     : Seq[IRespActionHandler],
              scStuffApi               : IScStuffApi,
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
            .set( nextRoute2, m.via )
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

      (for {
        indexes2 <- m.pot.toOption
      } yield {
        // Если !clean, и пришли с сервера прочищенные результаты, то надо восстановить исходную сортировку: сервер возвращает несортированный chunked-ответ.
        // TODO Надо учитывать, что за время запроса могут появиться новые элементы в списке на клиенте. И нужно отрабатывать все три списка (текущий, запрошенный и полученный).
        val pot2 = (for {
          indexesOuter0 <- outerLens.get( v0 ).saved
          indexes0 = indexesOuter0.indexes
          if !m.clean && !m.pot.isFailed &&
             indexes0.nonEmpty && indexes2.nonEmpty
        } yield {
          val nodeId2i = indexes0
            .iterator
            .flatMap(_.state.nodeId)
            .zipWithIndex
            .toMap

          MScIndexes.indexes.modify { inxs =>
            inxs.sortBy { inx =>
              inx.state.nodeId
                .flatMap( nodeId2i.get )
                .getOrElse( Int.MaxValue )
            }
          }(indexes2)
        })
          .orElse( m.pot )

        // Успешно прочитан результат.
        val modF = (
          (MIndexesRecentOuter.saved set pot2) andThen
          // Обновить CSS для списка узлов, раз уж всё впорядке.
          (TailAh.recents2searchCssModF( indexes2 ))
        )

        // Если clean, то запустить запрос на сервер по сверке списка узлов: вдруг, что-то изменилось.
        val fxOpt = Option.when( m.clean && indexes2.indexes.nonEmpty ) {
          Effect.action {
            CsrfTokenEnsure(
              onComplete = Some {
                Effect {
                  scStuffApi
                    .fillNodesList( indexes2 )
                    .transform { tryRes =>
                      // Сконвертировать набор resp'ов к необходимому представлению рантаймового списка. Заменить обновлённые узлы:
                      val respRes = for (resps2 <- tryRes) yield {
                        val resps2Map = (for {
                          r <- resps2.iterator
                          nodeId <- r.nodeId
                        } yield {
                          nodeId -> r
                        })
                          .toMap

                        MScIndexes.indexes.modify { inxs0 =>
                          for {
                            inx0 <- inxs0
                            nodeId <- inx0.indexResp.nodeId
                            resp2 <- resps2Map.get( nodeId )
                          } yield {
                            MScIndexInfo.indexResp.set( resp2 )(inx0)
                          }
                        }(indexes2)
                      }
                      val a = m.copy(
                        clean = false,
                        pot = m.pot.withTry(respRes),
                      )
                      Success(a)
                    }
                }
              }
            )
          }
        }
        val v2 = outerLens.modify( modF )(v0)
        ah.updatedMaybeEffect( v2, fxOpt )
      })
        .orElse {
          for (ex <- m.pot.exceptionOption) yield {
            logger.error( ErrorMsgs.SRV_REQUEST_FAILED, ex, m )
            // Отработать ошибку чтения.
            val v2 = outerLens
              .composeLens( MIndexesRecentOuter.saved )
              .modify( _.fail(ex) )(v0)
            updated(v2)
          }
        }
        .getOrElse {
          if (m.clean) {
            // Запустить эффект чтения из хранилища:
            val fx = Effect.action {
              val tryRes = Try {
                MIndexesRecentJs
                  .get()
                  .getOrElse( MScIndexes.empty )
              }
              LoadIndexRecents.pot.modify(_ withTry tryRes)(m)
            }
            val potLens = outerLens composeLens MIndexesRecentOuter.saved
            val v2 = if (potLens.exist(_.isPending)(v0)) {
              v0
            } else {
              potLens.modify(_.pending())(v0)
            }
            updatedSilent( v2, fx )

          } else {
            // Просто пересборка инстанса, чтобы освежить какие-то lazy-val'ы.
            val v2 = outerLens.modify(identity)(v0)
            updated(v2)
          }
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
                MIndexesRecentJs.get()
              }
              for (ex <- tryRes.failed)
                logger.warn( ErrorMsgs.KV_STORAGE_ACTION_FAILED, ex )
              tryRes
                .toOption
                .flatten
            }

            // Сборка сохраняемого инстанса MIndexInfo.
            val inxInfo2 = MScIndexInfo(
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
            val recentOpt2 = recentOpt0.fold [Option[MScIndexes]] {
              val ir = MScIndexes( inxInfo2 :: Nil )
              Some(ir)
            } { recents0 =>
              val isDuplicate = recents0.indexes.exists { m =>
                (m.state isSamePlaceAs currRoute)
              }

              Option.when(!isDuplicate) {
                val maxItems = MScIndexes.MAX_RECENT_ITEMS
                MScIndexes.indexes.modify { r0 =>
                  // Удалять из списка старые значения, которые выглядят похоже (без учёта isLoggedIn и прочих глубинных флагов).
                  val r1 = r0.filterNot(_.indexResp isLogoTitleBgSame inxInfo2.indexResp)
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
                ex <- Try( MIndexesRecentJs.save( recents2 ) ).failed
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
        .flatMap(_.indexes)
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
    case m @ HwBackBtn =>
      val v0 = value

      // TODO dia.nodes.circuit: Надо оптимизировать этот костыль, чтобы через js-роутер отрабатывалась ситуация:
      //      Запилить LkNodes js-роутер по аналогии с login-form, но более сложный.
      v0.dialogs.nodes.circuit
        .map { nodesCircuit =>
          val fx = Effect.action {
            nodesCircuit.dispatch( m.asInstanceOf[HwBackBtn.type] )
            DoNothing
          }
          effectOnly(fx)
        }
        .getOrElse {
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
        }


    // SPA-роутер заливает в состояние данные из URL.
    case m: RouteTo =>
      val v0 = value

      // Аккамулятор функций, обновляющих список модов, которые будут наложены на v0.
      var modsAcc = List.empty[MScRoot => MScRoot]

      // Возможно js-роутер или cordova-fetch ещё не готовы, и нужно отложить полную обработку состояния.
      // Ожидание cordova-fetch требуется, т.к. cordova либо AFNetworking на iOS 12 как-то долговато инициализируются.
      val isFullyReady = v0.dev.platform.isReady &&
        v0.internals.jsRouter.jsRouter.isReady &&
        v0.internals.boot.wzFirstDone.getOrElseTrue

      // Надо ли повторно отрабатывать m после того, как js-роутер станет готов?
      var jsRouterAwaitRoute = false

      // Текущее значение MainScreen:
      val currMainScreen = TailAh.getMainScreenSnapShot( v0 )

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
          val adsPot = v0.grid.core.ads.adsTreePot
          if (adsPot.isPending || adsPot.exists(!_.subForest.isEmpty))
            isGridNeedsReload = true
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Проверка поля searchOpened
      if (m.mainScreen.searchOpened !=* currMainScreen.searchOpened) {
        // Первое раскрытие панели может приводить к запросам в jsRouter, который на раннем этапе может быть ещё не готов.
        // Поэтому проверяем загруженность роутера в память, прежде чем запускать.
        var sideBarFx: Effect = SideBarOpenClose(
          bar = MScSideBars.Search,
          open = OptionUtil.SomeBool(m.mainScreen.searchOpened),
        ).toEffectPure

        if (v0.internals.boot.wzFirstDone.isEmpty) {
          val jsRouterSvcId = MBootServiceIds.JsRouter
          sideBarFx = Boot( jsRouterSvcId :: Nil ).toEffectPure >>
            BootAfter( jsRouterSvcId, sideBarFx ).toEffectPure
        }
        fxsAcc ::= sideBarFx
      }

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
        for (nextGeoPoint <- m.mainScreen.locEnv) {
          modsAcc ::= TailAh.root_index_search_geo_init_state_LENS
            .modify {
              _.withCenterInitReal( nextGeoPoint )
            }
        }
        if (isFullyReady) {
          needUpdateUi = true
          isToReloadIndex = true
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
          val focusedAdIdOpt = m.mainScreen.focusedAdId
            .orElse( currMainScreen.focusedAdId )
          if (focusedAdIdOpt.nonEmpty) {
            // Для GridBlockClick надо вычислить gridKey, поискав карточку с таким nodeId в текущей плитке:
            fxsAcc ::= GridBlockClick(
              adIds = focusedAdIdOpt.toList,
              gridPath = None,
              gridKey = None,
            ).toEffectPure
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
      if (
        m.mainScreen.needGeoLoc &&
        v0.internals.boot.wzFirstDone.nonEmpty &&
        v0.dialogs.first.view.isEmpty &&
        !WzFirstDiaAh.isNeedWizardFlowVal
      ) {
        // Если геолокация ещё не запущена, то запустить:
        if (
          !isGeoLocRunning &&
          !(v0.dev.geoLoc.switch.onOff contains[Boolean] true)
        ) {
          fxsAcc ::= GeoLocOnOff(enabled = true, isHard = false).toEffectPure
          isGeoLocRunning = true
        }

        // Если bluetooth не запущен - запустить в добавок к геолокации:
        if (
          !(v0.dev.beaconer.isEnabled contains[Boolean] true) &&
          (v0.dev.beaconer.hasBle contains[Boolean] true)
        ) {
          fxsAcc ::= Effect.action {
            BtOnOff(
              isEnabled = OptionUtil.SomeBool.someTrue,
              opts = MBeaconerOpts(
                askEnableBt = false,
                oneShot     = false,
                scanMode    = IBleBeaconsApi.ScanMode.BALANCED,
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
      val root_internals_jsRouter_delayedRouteTo_LENS = MScRoot.internals
        .composeLens( MScInternals.jsRouter )
        .composeLens( MJsRouterS.delayedRouteTo )
      val delayedRouteTo0 = root_internals_jsRouter_delayedRouteTo_LENS get v0
      for {
        awaitRouteOpt <- {
          // Выставить роуту
          if (jsRouterAwaitRoute)
            Some(Some(m))
          // Сбросить ранее выставленную роуту
          else if (delayedRouteTo0.nonEmpty)
            Some(None)
          // Ничего менять в состоянии delayed-роуты не надо
          else None
        }
      } {
        modsAcc ::= root_internals_jsRouter_delayedRouteTo_LENS set awaitRouteOpt

        // Если delayed-роутера выставлена впервые, то надо подписаться на BootAfter(js-роутера), дождавшись platform ready.
        if (delayedRouteTo0.isEmpty) {
          val afterRouterReadyFx = Effect.action {
            // Надо по наступлению готовности платформы и роутера запускать delayed-роуту в обработку.
            // В cordova на iOS-12 роутер готов до platformReady, на остальных - наоборот.
            val runRouteFx = Effect.action {
              root_internals_jsRouter_delayedRouteTo_LENS
                .get( modelRW.value )
                .getOrElse( DoNothing )
            }
            // Сохранённая delayed-роута будет зачищена в ходе обработки RouteTo (см.выше delayedRouteTo.nonEmpty).
            BootAfter(
              MBootServiceIds.JsRouter,
              fx = runRouteFx,
              ifMissing = Some( runRouteFx ),
            )
          }
          fxsAcc ::= BootAfter(
            MBootServiceIds.Platform,
            fx = afterRouterReadyFx,
            ifMissing = Some( afterRouterReadyFx ),
          )
            .toEffectPure
        }
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
            geoIntoRcvr = m.mainScreen.nodeId.isEmpty && (v0.index.resp ==* Pot.empty),
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

      var modsAcc = List.empty[MScRoot => MScRoot]
      var fxAcc = List.empty[Effect]
      var nonSilentUpdate = false

      for {
        geoLockTimerId <- v0.internals.info.geoLockTimer
        glSignal <- m.origOpt
        if glSignal.glType.isHighAccuracy
      } {
        val mapAlreadySet = glSignal.isSuccess && v0.index.resp.isEmpty

        if (mapAlreadySet)
          for (geoLoc <- glSignal.locationOpt)
            modsAcc ::= TailAh.root_index_search_geo_init_state_LENS
              .modify( _.withCenterInitReal( geoLoc.point ) )

        val indexMapReset2 = !mapAlreadySet
        val switchCtx = m.scSwitch.fold {
          MScSwitchCtx(
            indexQsArgs = MScIndexArgs(
              geoIntoRcvr = true,
              retUserLoc  = m.origOpt
                .fold(true)(_.either.isLeft),
            ),
            indexMapReset = indexMapReset2,
          )
        } (MScSwitchCtx.indexMapReset set indexMapReset2)

        fxAcc ::= TailAh.getIndexFx( switchCtx )
        fxAcc ::= Effect.action {
          DomQuick.clearTimeout(geoLockTimerId)
          DoNothing
        }

        TailAh
          ._removeTimer( v0 )
          .foreach { case (alterF, ctFx) =>
            modsAcc ::= alterF
            fxAcc ::= ctFx
          }
      }

      // Обновить локацию на карте.
      for {
        glSignal <- m.origOpt
        geoLoc <- glSignal.locationOpt
      } {
        // Всегда сохранять координаты в .userLoc
        var modF = MMapInitState.userLoc set Some(geoLoc)

        if (
          // Нельзя менять местоположение на карте, если это просто фоновое тестирование индекса.
          !m.scSwitch.exists(_.demandLocTest) &&
          // Если очень ожидается текущая геолокация, то задвинуть карту.
          (
            !(v0.internals.boot.wzFirstDone contains[Boolean] true) ||
            (v0.index.resp ==* Pot.empty)
          ) &&
          (v0.internals.info.currRoute.exists { currRoute =>
            currRoute.locEnv.isEmpty && currRoute.nodeId.isEmpty
          })
        ) {
          modF = modF andThen MMapInitState
            .state.modify( _.withCenterInitReal(geoLoc.point) )
          nonSilentUpdate = true
        } else {
          nonSilentUpdate = nonSilentUpdate || v0.index.search.panel.opened
        }

        modsAcc ::= MScRoot.index
          .composeLens( TailAh._inxSearchGeoMapInitLens )
          .modify( modF )
      }

      // Если wz1 ждёт пермишшена на геолокацию, то надо обрадовать его:
      if (
        (v0.internals.boot.wzFirstDone contains[Boolean] false) &&
        (v0.dialogs.first.view.exists { wz1 =>
          (wz1.phase ==* MWzPhases.GeoLocPerm) &&
          (wz1.frame ==* MWzFrames.InProgress)
        })
      ) {
        fxAcc ::= WzPhasePermRes(
          phase = MWzPhases.GeoLocPerm,
          res = Success( BoolOptPermissionState(OptionUtil.SomeBool.someTrue) ),
        )
          .toEffectPure
      }

      ah.optionalResult(
        v2Opt = modsAcc
          .reduceOption(_ andThen _)
          .map(_(v0)),
        fxOpt = fxAcc.mergeEffects,
        silent = !nonSilentUpdate,
      )


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


    // Ошибка от компонента.
    case m: ComponentCatch =>
      logger.error( ErrorMsgs.CATCHED_CONSTRUCTOR_EXCEPTION, msg = m )
      // TODO Отрендерить ошибку на экран.
      noChange

  }

}
