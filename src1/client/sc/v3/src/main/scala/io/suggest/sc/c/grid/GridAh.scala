package io.suggest.sc.c.grid

import com.github.fisshy.react.scroll.AnimateScroll
import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.ad.blk.BlockPaddings
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{MScreen, MSzMult}
import io.suggest.grid.build.{GridBuilderUtil, MGbBlock, MGridBuildArgs, MGridBuildResult, MGridRenderInfo}
import io.suggest.grid.{GridBuilderUtilJs, GridCalc, GridConst, GridScrollUtil, MGridCalcConf}
import io.suggest.jd.{MJdConf, MJdTagId}
import io.suggest.jd.render.m.{GridRebuild, MJdDataJs, MJdRuntime}
import io.suggest.jd.tags.JdTag
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.{HandleScApiResp, MScRoot, OnlineCheckConn, ResetUrlRoute}
import io.suggest.sc.m.grid._
import io.suggest.sc.sc3._
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.geom.d2.IWidth
import io.suggest.jd.render.u.JdUtil
import io.suggest.n2.node.{MNodeType, MNodeTypes}
import io.suggest.sc.ads.MScNodeMatchInfo
import io.suggest.sc.u.ScQsUtil
import io.suggest.log.Log
import io.suggest.sc.v.styl.ScCss
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import japgolly.univeq._
import org.scalajs.dom
import scalaz.Tree

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:59
  * Description: Утиль контроллера плитки карточек.
  */
object GridAh {

  private def GRID_CONF = MGridCalcConf.EVEN_GRID


  /** Полное переконфигурирование плитки.
    * @return 1 - кол-во колонок плитки.
    *         2 - szMult плитки.
    */
  def fullGridConf(mscreen: IWidth): (Int, MSzMult) = {
    val gridConf = GRID_CONF

    val gridColsCount = GridCalc.getColumnsCount(
      contSz = mscreen,
      conf   = gridConf,
    )
    val gridSzMult = GridCalc.getSzMult4tilesScr(gridColsCount, mscreen, gridConf)
    (gridColsCount, gridSzMult)
  }


  /** Выполнение ребилда плитки. */
  def rebuildGrid(ads: Pot[Seq[MScAdData]], jdConf: MJdConf, jdRuntime: MJdRuntime): MGridBuildResult = {

    /** Конвертация одной карточки в один блок для рендера в плитке. */
    def blockRenderData2GbPayload(blk: JdTag, brd: MJdDataJs, jdId: MJdTagId): Tree[MGbBlock] = {
      // Несфокусированная карточка. Вернуть blockMeta с единственного стрипа.
      Tree.Leaf {
        MGbBlock(
          jdId      = jdId,
          size      = GridBuilderUtilJs.gbSizeFromJdt(jdId, blk, jdRuntime, jdConf),
          jdt       = blk,
          wideBgSz  = OptionUtil.maybeOpt(blk.props1.expandMode.nonEmpty) {
            for {
              bgImgEdgeId <- blk.props1.bgImg
              edge <- brd.edges.get( bgImgEdgeId.edgeUid )
              origWh <- edge.origWh
            } yield {
              origWh
            }
          },
        )
      }
    }

    // Приведение списка карточек к grid-блокам и подблоков обсчёта плитки.
    val itmDatas = (for {
      scAdData <- ads
        .iterator
        .flatten
    } yield {
      scAdData
        .focOrAlwaysOpened
        .fold [Tree[MGbBlock]] {
          // Несфокусированная карточка. Вернуть данные единственного блока.
          val brd = scAdData.main
          blockRenderData2GbPayload( brd.doc.template.rootLabel, brd, brd.doc.tagId )

        } { fullAdData =>
          // Открытая карточка. Вернуть MGbSubItems со списком фокус-блоков:
          Tree.Node(
            root = {
              val jdt = fullAdData.doc.template.rootLabel
              val jdId = fullAdData.doc.tagId

              MGbBlock(
                jdId = jdId,
                size = GridBuilderUtilJs.gbSizeFromJdt(
                  jdt       = jdt,
                  jdRuntime = jdRuntime,
                  jdConf    = jdConf,
                  jdId      = jdId,
                ),
                jdt = jdt,
              )
            },
            forest = for {
              tplIndexedTree <- JdUtil
                .mkTreeIndexed( fullAdData.doc )
                .subForest
            } yield {
              val (subJdId, subJdt) = tplIndexedTree.rootLabel
              blockRenderData2GbPayload( subJdt, fullAdData, subJdId )
            },
          )
        }
    })
      .to( LazyList )

    GridBuilderUtil.buildGrid(
      MGridBuildArgs(
        itemsExtDatas = itmDatas,
        jdConf        = jdConf,
        offY          = GridConst.CONTAINER_OFFSET_TOP,
        jdtWideSzMults = jdRuntime.data.jdtWideSzMults,
      )
    )
  }


  /** Сборка аргументов для рендера JdCss. */
  def mkJdRuntime(ads: Pot[Seq[MScAdData]], g: MGridCoreS): MJdRuntime =
    mkJdRuntime(ads, g.jdConf, g.jdRuntime)
  def mkJdRuntime(ads: Pot[Seq[MScAdData]], jdConf: MJdConf, jdRuntime: MJdRuntime): MJdRuntime = {
    JdUtil
      .mkRuntime( jdConf )
      .docs(
        ads
          .iterator
          .flatten
          .flatMap { adData =>
            JdUtil.flatGridTemplates( adData.focOrMain )
          }
          .to( LazyList )
      )
      .prev( jdRuntime )
      .result
  }


  /** Эффект скроллинга к указанной карточке. */
  def scrollToAdFx(toAd    : MScAdData,
                   gbRes   : MGridBuildResult
                  ): Effect = {
    // Карточка уже открыта, её надо свернуть назад в main-блок.
    // Нужно узнать координату в плитке карточке
    Effect.action {
      JdUtil.flatGridTemplates( toAd.focOrMain )
        .iterator
        .flatMap { scAd =>
          gbRes.coordsById.get( scAd.tagId )
        }
        // Взять только самый верхний блок карточки. Он должен быть первым по порядку:
        .nextOption()
        .foreach { toXY =>
          AnimateScroll.scrollTo(
            // Сдвиг обязателен, т.к. карточки заезжают под заголовок.
            to = Math.max(0, toXY.y - ScCss.HEADER_HEIGHT_PX - BlockPaddings.default.value),
            options = GridScrollUtil.scrollOptions(isSmooth = true)
          )
        }

      DoNothing
    }
  }


  def saveAdIntoValue(index: Int, newAd: MScAdData, v0: MGridS): MGridS = {
    MGridS.core
      .composeLens( MGridCoreS.ads )
      .set( saveAdIntoAds(index, newAd, v0) )(v0)
  }

  /** Залить в состояние обновлённый инстанс карточки. */
  def saveAdIntoAds(index: Int, newAd: MScAdData, v0: MGridS): Pot[Vector[MScAdData]] = {
    for (ads0 <- v0.core.ads) yield {
      ads0.updated(index, newAd)
    }
  }


  /** Найти карточку с указанным id в состоянии, вернув её и её индекс. */
  def findAd(nodeId: String, v0: MGridCoreS): Option[(MScAdData, Int)] = {
    v0.ads
      .iterator
      .flatten
      .zipWithIndex
      .find { _._1.nodeId contains nodeId }
  }


  /** Восстановление скролла после добавления
    *
    * @param g0 Начальное состояние плитки.
    * @param g2 Новое состояние плитки.
    * @return
    */
  def repairScrollPosFx(g0: MGridS, g2: MGridS): Option[Effect] = {
    // Нужно скроллить НЕанимированно, т.к. неявная коррекция выдачи должна проходить мгновенно и максимально незаметно.
    // Для этого надо вычислить разницу высоты между старой плиткой и новой плиткой, и скорректировать текущий скролл
    // на эту разницу без какой-либо анимации TODO (за искл. около-нулевого исходного скролла).
    val gridHeightPx0 = g0.core.gridBuild.gridWh.height
    val gridHeightPx2 = g2.core.gridBuild.gridWh.height
    val gridHeightDeltaPx = gridHeightPx2 - gridHeightPx0
    Option.when( Math.abs(gridHeightDeltaPx) > 2 ) {
      // Есть какой-то заметный глазу скачок высоты плитки. Запустить эффект сдвига скролла плитки.
      Effect.action {
        // Нужно понять, есть ли скролл прямо сейчас: чтобы не нагружать состояние лишним мусором, дёргаем элемент напрямую.
        if (
          Option( dom.document.getElementById( GridScrollUtil.SCROLL_CONTAINER_ID ) )
            .exists { el => el.scrollTop > 1 }
        ) {
          AnimateScroll.scrollMore( gridHeightDeltaPx, GridScrollUtil.scrollOptions(isSmooth = false) )
        }

        DoNothing
      }
    }
  }

  def _isMatches(onlyMatching: MScNodeMatchInfo, scAnm: MScNodeMatchInfo): Boolean = {
    onlyMatching.ntype.fold(true)( scAnm.ntype.contains[MNodeType] ) &&
    onlyMatching.nodeId.fold(true)( scAnm.nodeId.contains[String] )
  }

}


/** Контроллер плитки карточек.
  * @param scRootRO Доступ к текущим аргументам поиска карточек.
  */
class GridAh[M](
                 api             : IScUniApi,
                 scRootRO        : ModelRO[MScRoot],
                 screenRO        : ModelRO[MScreen],
                 modelRW         : ModelRW[M, MGridS]
               )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Реакция на событие скроллинга плитки: разобраться, стоит ли подгружать ещё карточки с сервера.
    case m: GridScroll =>
      val v0 = value

      if (
        !v0.core.ads.isPending &&
        v0.hasMoreAds && {
          // Оценить уровень скролла. Возможно, уровень не требует подгрузки ещё карточек
          val contentHeight = v0.core.gridBuild.gridWh.height + GridConst.CONTAINER_OFFSET_TOP
          val screenHeight = screenRO.value.wh.height
          val scrollPxToGo = contentHeight - screenHeight - m.scrollTop
          scrollPxToGo < GridConst.LOAD_MORE_SCROLL_DELTA_PX
        }
      ) {
        // В фоне надо будет запустить подгрузку новых карточек.
        val fx = GridLoadAds(clean = false, ignorePending = true)
          .toEffectPure
        // Выставить pending в состояние, чтобы повторные события скролла игнорились.
        val v2 = MGridS.core
          .composeLens( MGridCoreS.ads )
          .modify(_.pending())(v0)
        updatedSilent(v2, fx)

      } else {
        // Больше нет карточек, или запрос карточек уже в процессе, или скроллинг не требует подгрузки карточек.
        noChange
      }


    // Сигнал к загрузке карточек с сервера согласно текущему состоянию выдачи.
    case m: GridLoadAds =>
      val v0 = value

      if (v0.core.ads.isPending && !m.ignorePending) {
        logger.warn( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.core.ads) )
        noChange

      } else {
        val nextReqPot2 = v0.core.ads.pending()

        // Если обновление m.onlyMatching, то возможна ситуация просто удаления каких-то карточек из выдачи:
        // Например, все маячки исчезли, и маячковые карточки надо удалить БЕЗ каких-либо запросов на сервер.
        // Используем сюжетные линии Left-Right, чтобы разделить эти два пересекающихся сценария.

        // Есть какие-то данные для селективного обновления плитки. Разобраться, что там:
        // Если частичный патчинг, то возможно, что надо модифицировать args.common:
        m.onlyMatching
          .toLeft( Option.empty[MScQs] )
          .left.flatMap { nodeMatchInfo =>
            // Есть какие-то правила для сверки узла. Накатить изменения на функцию-модификатор:
            // TODO Фильтрация по id узла пока не реализована (за ненадобностью).
            if (nodeMatchInfo.nodeId.nonEmpty)
              throw new UnsupportedOperationException( nodeMatchInfo.nodeId.toString )
            nodeMatchInfo.ntype
              .toLeft( Option.empty[MScQs] )
          }
          .left.flatMap {
            case bleNtype @ MNodeTypes.BleBeacon =>
              // Требуется запросить и отработать только карточки, которые относятся к bluetooth-маячкам.
              // Для этого, надо вычистить locEnv до маячков, и убрать остальные критерии запроса.
              // А есть ли маячки в qs?
              val mroot = scRootRO.value
              val bcns0 = mroot.locEnvBleBeacons

              lazy val ads2 = v0.core.ads.map { _.filterNot { scAdData =>
                // true для тех карточек, которые надо удалить.
                val matchInfos = scAdData.main.info.matchInfos
                val isDrop = matchInfos.nonEmpty && matchInfos.forall { matchInfo =>
                  matchInfo
                    .nodeMatchings
                    .exists { nodeMatchInfo =>
                      nodeMatchInfo.ntype contains[MNodeType] bleNtype
                    }
                }
                //println(s"QS scAd#${scAdData.id.orNull} vs m[$matchInfos] => drop?$isDrop")
                isDrop
              }}

              // если карточек вообще не осталось, то надо отрендерить 404-карточку.
              val isReturn404 = ads2.exists(_.isEmpty)

              // ads2.exists(_.isEmpty): Если после зачистки BLE-карточек, не осталось карточек, то запросить 404-карточку с сервера.
              Either.cond(
                test = bcns0.nonEmpty || isReturn404,

                right = {
                  // Есть видимые маячки. И наверное надо cделать запрос на сервер.
                  // TODO А может просто перетасовать карточки, если порядок маячков просто немного изменился? Или это BleBeaconer уже отрабатывает?
                  // allow404 обычно false, т.к. обычно есть карточки помимо маячковых.
                  val qs4Ble = ScQsUtil.gridAdsOnlyBleBeaconed( scRootRO.value, allow404 = isReturn404 )
                  //println(s"QS only Beaconed ask, qs = $qs4Ble")
                  Some(qs4Ble)
                },

                left = {
                  // Нет маячков в qs, но видимо ранее они были.
                  // Это значит, нужно просто удалить Bluetooth-only карточки (если они есть), без запросов на сервер.
                  val jdRuntime2 = GridAh.mkJdRuntime(ads2, v0.core)
                  val gbRes2 = MGridBuildResult.nextRender
                    .composeLens( MGridRenderInfo.animate )
                    .set( false )(
                      GridAh.rebuildGrid(ads2, v0.core.jdConf, jdRuntime2)
                    )
                  val v2 = MGridS.core.modify(
                    _.copy(
                      jdRuntime = jdRuntime2,
                      ads       = ads2,
                      gridBuild = gbRes2,
                    )
                  )(v0)
                  // Эффект скролла: нужно подправить плитку, чтобы не было рывка.
                  val fxOpt = GridAh.repairScrollPosFx( v0, v2 )
                  ah.updatedMaybeEffect(v2, fxOpt)
                }
              )

            case other =>
              throw new UnsupportedOperationException( other.toString )
          }
          .fold [ActionResult[M]] (
            identity,
            {reqScQsOpt =>
              // Надо делать эффект запроса на сервер с указанными qs.
              val fx = Effect {
                val args2 = reqScQsOpt getOrElse {
                  val offset = v0.core.ads
                    // Если clean, то нужно обнулять offset.
                    .filter(_ => !m.clean)
                    .fold(0)(_.size)
                  ScQsUtil.gridAdsQs( scRootRO.value, offset )
                }

                // Завернуть ответ сервера в экшен:
                val startTime = nextReqPot2.asInstanceOf[PendingBase].startTime

                // Запустить запрос с почищенными аргументами...
                api
                  .pubApi( args2 )
                  .transform { tryRes =>
                    val r = HandleScApiResp(
                      reqTimeStamp  = Some( startTime ),
                      qs            = args2,
                      tryResp       = tryRes,
                      reason        = m,
                    )
                    Success(r)
                  }
              } + OnlineCheckConn.toEffectPure

              val v2 = MGridS.core
                .composeLens( MGridCoreS.ads )
                .set( nextReqPot2 )(v0)
              updated(v2, fx)
            }
          )
      }


    // Реакция на клик по карточке в плитке.
    // Нужно отправить запрос на сервер, чтобы понять, что делать дальше.
    // Возможны разные варианты: фокусировка в карточку, переход в выдачу другого узла, и т.д. Всё это расскажет сервер.
    case m: GridBlockClick =>
      val v0 = value

      // Поискать запрошенную карточку в состоянии.
      GridAh
        .findAd(m.nodeId, v0.core)
        .fold {
          logger.error( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange

        } { case (ad0, index) =>
          if (ad0.isAlwaysOpened) {
            // Клик по всегда развёрнутой карточке должен приводить к скроллу к началу карточки без загрузки.
            val scrollFx = GridAh.scrollToAdFx( ad0, v0.core.gridBuild )
            effectOnly(scrollFx)

          } else {
            ad0.focused.fold {
              // Карточка сейчас скрыта, её нужно раскрыть.
              // Собрать запрос фокусировки на ровно одной рекламной карточке.
              val fx = Effect {
                val qs = ScQsUtil.focAdsQs( scRootRO.value, m.nodeId )

                api.pubApi( qs )
                  .transform { tryResp =>
                    val r = HandleScApiResp(
                      reqTimeStamp  = None,
                      qs            = qs,
                      tryResp       = tryResp,
                      reason        = m,
                    )
                    Success(r)
                  }
              } + OnlineCheckConn.toEffectPure

              // выставить текущей карточке, что она pending
              val ad1 = MScAdData.focused
                .modify(_.pending())(ad0)

              val v2 = GridAh.saveAdIntoValue(index, ad1, v0)
              updated(v2, fx)

            } { _ =>
              // Карточка уже раскрыта. Синхронное сокрытие карточки.
              val ad1         = MScAdData.focused.set( Pot.empty )(ad0)
              val ads2        = GridAh.saveAdIntoAds(index, ad1, v0)
              val jdRuntime2  = GridAh.mkJdRuntime(ads2, v0.core)
              val gridBuild2  = GridAh.rebuildGrid(ads2, v0.core.jdConf, jdRuntime2)
              val v2          = MGridS.core.modify { core0 =>
                core0.copy(
                  ads       = ads2,
                  jdRuntime = jdRuntime2,
                  gridBuild = gridBuild2
                )
              }(v0)
              // В фоне - запустить скроллинг к началу карточки.
              val scrollFx      = GridAh.scrollToAdFx( ad1, gridBuild2 )
              val resetRouteFx  = ResetUrlRoute.toEffectPure
              val fxs           = scrollFx + resetRouteFx
              updated(v2, fxs)
            }
          }
        }


    // Экшен запуска пересчёта конфигурации плитки.
    case m: GridRebuild =>
      val v0 = value
      val mscreen = screenRO.value
      val (gridColsCount2, szMult2) = GridAh.fullGridConf( mscreen.wh )

      val jdConf0 = v0.core.jdConf
      val szMultMatches = jdConf0.szMult ==* szMult2
      //println( s"gridColumnCount=$gridColsCount2 $szMultMatches ${jdConf0.szMult} => $szMult2" )

      if (
        !m.force &&
        szMultMatches &&
        (jdConf0.gridColumnsCount ==* gridColsCount2)
      ) {
        noChange

      } else {
        val jdConf1 = {
          var jdConfLens = MJdConf.gridColumnsCount.set( gridColsCount2 )
          if (!szMultMatches)
            jdConfLens = jdConfLens andThen MJdConf.szMult.set(szMult2)
          jdConfLens( jdConf0 )
        }

        val jdRuntime2 =
          if (!m.force && szMultMatches) v0.core.jdRuntime
          else GridAh.mkJdRuntime(v0.core.ads, jdConf1, v0.core.jdRuntime)

        var coreModF = (
          (MGridCoreS.jdConf set jdConf1) andThen
          (MGridCoreS.gridBuild set GridAh.rebuildGrid(v0.core.ads, jdConf1, jdRuntime2))
        )

        // Если изменился szMult, то перепилить css-стили карточек:
        if (!szMultMatches)
          coreModF = coreModF andThen (MGridCoreS.jdRuntime set jdRuntime2)

        val v2 = (MGridS.core modify coreModF)(v0)

        // TODO Возможно, что надо перекачать содержимое плитки с сервера, если всё слишком сильно переменилось. Нужен отложенный таймер для этого.
        updated(v2)
      }


    // Добавить в акк инфу по исполению после обновления плитки.
    case m: GridAfterUpdate =>
      val v0 = value
      val v2 = MGridS.afterUpdate.modify( m.effect :: _ )(v0)
      updatedSilent(v2)

  }

}
