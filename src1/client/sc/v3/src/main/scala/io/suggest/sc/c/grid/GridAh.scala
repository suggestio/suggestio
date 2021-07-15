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
import io.suggest.sc.m.{HandleScApiResp, MScRoot, ResetUrlRoute}
import io.suggest.sc.m.grid._
import io.suggest.sc.sc3._
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.geom.d2.IWidth
import io.suggest.jd.render.u.JdUtil
import io.suggest.n2.node.MNodeTypes
import io.suggest.sc.ads.MScNodeMatchInfo
import io.suggest.sc.u.ScQsUtil
import io.suggest.log.Log
import io.suggest.n2.edge.MEdgeFlags
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.inx.MScSwitchCtx
import io.suggest.sc.v.styl.ScCss
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil.TreeLocOps
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import org.scalajs.dom
import scalaz.{NonEmptyList, Tree, TreeLoc}

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:59
  * Description: Утиль контроллера плитки карточек.
  */
object GridAh extends Log {

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
  def rebuildGrid(ads: MGridAds, jdConf: MJdConf, jdRuntime: MJdRuntime): MGridBuildResult = {
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
      adsTree <- ads.adsTreePot.iterator
      scAdDataLoc <- adsTree
        .loc.onlyLowest
        .iterator
      scAdData = scAdDataLoc.getLabel
      adData <- scAdData.data.iterator
    } yield {
      if (!adData.isOpened) {
        // Несфокусированная или не раскрытая карточка. Только один блок:
        val brd = scAdData.data.get
        blockRenderData2GbPayload( brd.doc.template.rootLabel, brd, brd.doc.tagId )

      } else {
        // Открытая карточка. Вернуть MGbSubItems со списком фокус-блоков:
        Tree.Node(
          root = {
            val jdt = adData.doc.template.rootLabel
            val jdId = adData.doc.tagId

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
              .mkTreeIndexed( adData.doc )
              .subForest
          } yield {
            val (subJdId, subJdt) = tplIndexedTree.rootLabel
            blockRenderData2GbPayload( subJdt, adData, subJdId )
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
  def mkJdRuntime(gridAds: MGridAds, g: MGridCoreS): MJdRuntime =
    mkJdRuntime(gridAds, g.jdConf, g.jdRuntime)
  def mkJdRuntime(gridAds: MGridAds, jdConf: MJdConf, jdRuntime: MJdRuntime): MJdRuntime = {
    JdUtil
      .prepareJdRuntime( jdConf )
      .docs {
        (for {
          // adPtrsSet: не нужно рендерить стили для карточек, которых нет на экране или которые дублируются в плитке.
          scAds <- gridAds.adsTreePot.iterator
          scAdLoc <- scAds
            .loc.onlyLowest
            .iterator
          scAd = scAdLoc.getLabel
          gridItem <- scAd.gridItems
        } yield {
          gridItem.jdDoc
        })
          .to( LazyList )
      }
      .prev( jdRuntime )
      .make
  }


  /** Эффект скроллинга к указанной карточке. */
  // TODO Унести на уровень view (в GridR), законнектится на поле MGridS().interactWith для инфы по скроллу.
  def scrollToAdFx(toAd    : MScAdData,
                   gbRes   : MGridBuildResult
                  ): Effect = {
    // Карточка уже открыта, её надо свернуть назад в main-блок.
    // Нужно узнать координату в плитке карточке
    Effect.action {
      toAd
        .gridItems
        .iterator
        .flatMap { gridItem =>
          gbRes.coordsById.get( gridItem.jdDoc.tagId )
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


  /** Найти карточку с указанным id в состоянии, вернув её и её индекс. */
  def findAd(m: GridBlockClick, v0: MGridAds): Option[TreeLoc[MScAdData]] = {
    v0.adsTreePot
      .toOption
      .flatMap { adsTree =>
        m.gridPath
          // map+flatten вместо flatMap, чтобы быстрее обнаруживать ошибки неправильных gridPath.
          .map { gridPath =>
            adsTree.loc.findByGridKeyPath( gridPath )
          }
          .orElse {
            m.gridKey
              .map ( adsTree.loc.findByGridKey )
          }
          .flatten
      }
  }


  /** Нечистый метод чтения текущего скролла через ковыряния внутри view'а,
    * чтобы не усложнять модели и всю логику обработки скролла.
    * Следует дёргать внутри Effect().
    */
  def getGridScrollTop(): Option[Double] = {
    Option( dom.document.getElementById( GridScrollUtil.SCROLL_CONTAINER_ID ) )
      .map( _.scrollTop )
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
        if ( getGridScrollTop().exists(_ > 1) ) {
          AnimateScroll.scrollMore( gridHeightDeltaPx, GridScrollUtil.scrollOptions(isSmooth = false) )
        }

        DoNothing
      }
    }
  }

  def _isMatches(onlyMatching: MScNodeMatchInfo, scAnm: MScNodeMatchInfo): Boolean = {
    onlyMatching.ntype.fold(true) { onlyMatchingType =>
      scAnm.ntype.exists { scAdMatchType =>
        scAdMatchType eqOrHasParent onlyMatchingType
      }
    } &&
    onlyMatching.nodeId.fold(true)( scAnm.nodeId.contains[String] )
  }


  /** Сброс фокуса у всех карточек, кроме указанной.
    * Указанная карточка - перезаписывается указанным инстансом.
    *
    * @param gridKeyPathOpt Порядковый номер обновляемой карточки в плитке.
    * @param gridCore0 Исходное состояние плитки.
    * @return Обновлённое состояние плитки.
    */
  def resetFocus(gridKeyPathOpt: Option[List[GridAdKey_t]], gridCore0: MGridCoreS): MGridCoreS = {
    // Т.к. фокусировка может быть вложенная, а дерево надо проходить по всем под-уровням, то сначала надо поискать
    // локацию указанной карточки в дереве, вычислить node path и сравнивать все проходимые элементы дерева
    // с целевым nodePath, чтобы не ломать линию фокуса.
    val gridAds2 = MGridAds.adsTreePot.modify {
      _.map { adsPtrs0 =>
        (for {
          // Не трогать фокусировку элемента дерева по указанному пути.
          // Должен начинаться с корневого 0-элемента, т.к. нижнее дерево
          keepNodePath <- {
            val r = gridKeyPathOpt
              // Nil подразумевает, что надо свернуть карточки вплость до root.subForest.
              .orElse( Some(Nil) )

            if (r.isEmpty)
              logger.warn( ErrorMsgs.NODE_NOT_FOUND, msg = gridKeyPathOpt )

            r
          }

        } yield {
          adsPtrs0
            .loc
            .dropChildrenUntilPath( keepNodePath )
            .toTree
        })
          .getOrElse {
            // should never happen: узел с указанными gridKey не найден в дереве.
            logger.warn( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = (ErrorMsgs.NODE_NOT_FOUND, gridKeyPathOpt) )
            adsPtrs0
          }
      }
    }( gridCore0.ads )

    val jdRuntime2 = GridAh.mkJdRuntime( gridAds2, gridCore0 )
    val gridBuild2 = GridAh.rebuildGrid(
      ads = gridAds2,
      jdConf = gridCore0.jdConf,
      jdRuntime = jdRuntime2,
    )

    gridCore0.copy(
      jdRuntime = jdRuntime2,
      ads       = gridAds2,
      gridBuild = gridBuild2,
    )
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
    // Логика проверок события живёт в шаблоне (компоненте), чтобы существенно снизить нагрузку на circuit, поэтому GridScroll - событие нечастое.
    case GridScroll =>
      val v0 = value

      val lens = MGridS.core
        .composeLens( MGridCoreS.ads )
        .composeLens( MGridAds.adsTreePot )

      if (
        !lens.get(v0).isPending &&
        v0.hasMoreAds
      ) {
        // В фоне надо будет запустить подгрузку новых карточек.
        val fx = GridLoadAds(clean = false, ignorePending = true)
          .toEffectPure

        // Выставить pending в состояние, чтобы повторные события скролла игнорились.
        val v2 = lens.modify(_.pending())(v0)

        updatedSilent(v2, fx)

      } else {
        // По идее, noChange должны были быть отсеяны уже на уровне GridR component, но всякое возможно...
        noChange
      }


    // Сигнал к загрузке карточек с сервера согласно текущему состоянию выдачи.
    case m: GridLoadAds =>
      val v0 = value

      if (v0.core.ads.adsTreePot.isPending && !m.ignorePending) {
        logger.warn( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.core.ads) )
        noChange

      } else {
        val nextReqPot2 = v0.core.ads.adsTreePot.pending()

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
            case radioSourceType @ MNodeTypes.RadioSource.BleBeacon => // TODO "case radioSourceType if radioSourceType eqOrHasParent MNodeTypes.RadioSource =>" -- Replace code, after app v5.0.3 installed (including Sc3Circuit & ScAdsTile)
              // Требуется запросить и отработать только карточки, которые относятся к bluetooth-маячкам.
              // Для этого, надо вычистить locEnv до маячков, и убрать остальные критерии запроса.
              // А есть ли маячки в qs?
              val mroot = scRootRO.value
              val bcns0 = mroot.locEnvBleBeacons

              val gridAds0 = v0.core.ads
              val gridAds2 = MGridAds.adsTreePot.modify {
                _.map { adsPtrs0 =>
                  Tree.Node(
                    root = adsPtrs0.rootLabel,
                    forest = (for {
                      childSubTree0 <- adsPtrs0.subForest.iterator
                      scAdData = childSubTree0.rootLabel
                      adData <- scAdData.data
                        .toOption
                        .iterator
                      if {
                        // isDrop=true для тех карточек, которые надо удалить.
                        val matchInfos = adData.info.matchInfos
                        val isDrop = matchInfos.nonEmpty && matchInfos.forall { matchInfo =>
                          matchInfo
                            .nodeMatchings
                            .exists { nodeMatchInfo =>
                              nodeMatchInfo.ntype.exists { scAdMatchNtype =>
                                scAdMatchNtype eqOrHasParent radioSourceType
                              }
                            }
                        }
                        !isDrop
                      }
                    } yield {
                      childSubTree0
                    })
                      .to( LazyList )
                      .toEphemeralStream,
                  )
                }
              }(gridAds0)

              // если карточек вообще не осталось, то надо отрендерить 404-карточку.
              val isReturn404 = gridAds2.adsTreePot.exists(_.subForest.isEmpty)

              // ads2.exists(_.isEmpty): Если после зачистки BLE-карточек, не осталось карточек, то запросить 404-карточку с сервера.
              Either.cond(
                test = bcns0.nonEmpty || isReturn404,

                right = {
                  // Есть видимые маячки. И наверное надо cделать запрос на сервер.
                  // TODO А может просто перетасовать карточки, если порядок маячков просто немного изменился? Или это BleBeaconer уже отрабатывает?
                  // allow404 обычно false, т.к. обычно есть карточки помимо маячковых.
                  val qs4Ble = ScQsUtil.gridAdsOnlyBleBeaconed( scRootRO.value, allow404 = isReturn404 )
                  Some(qs4Ble)
                },

                left = {
                  // Нет маячков в qs, но видимо ранее они были.
                  // Это значит, нужно просто удалить Bluetooth-only карточки (если они есть), без запросов на сервер.
                  val jdRuntime2 = GridAh.mkJdRuntime( gridAds2, v0.core )
                  val gbRes2 = MGridBuildResult.nextRender
                    .composeLens( MGridRenderInfo.animate )
                    .set( false )(
                      GridAh.rebuildGrid( gridAds2, v0.core.jdConf, jdRuntime2 )
                    )
                  val v2 = MGridS.core.modify(
                    _.copy(
                      jdRuntime = jdRuntime2,
                      ads       = gridAds2,
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
                  val offset = v0.core.ads.adsTreePot
                    // Если clean, то нужно обнулять offset.
                    .filter(_ => !m.clean)
                    .fold(0)(_.subForest.length)
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
              }

              val v2 = MGridS.core
                .composeLens( MGridCoreS.ads )
                .composeLens( MGridAds.adsTreePot )
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
      val gridAds0 = v0.core.ads

      // Поискать запрошенную карточку в состоянии.
      (for {
        // Отработать открытие кастомной карточки по adId и пути в дереве:
        _ <- Option.when( m.adIds.isEmpty )(())
        scAdLoc <- GridAh.findAd(m, gridAds0)
        scAdData = scAdLoc.getLabel
        adData <- scAdData.data.toOption
        gridKey <- m.gridKey
        gridKeyPath <- m.gridPath
      } yield {
        if (adData.info.flags.exists(_.flag ==* MEdgeFlags.AlwaysOpened)) {
          // Клик по всегда развёрнутой карточке должен приводить к скроллу к началу карточки без загрузки.
          val scrollFx = GridAh.scrollToAdFx( scAdData, v0.core.gridBuild )
          val v2 = (
            MGridS.core.modify { gridCore0 =>
              // Состояние обновляем на данную карточку, чтобы sc-nodes-форма могла корректно определить текущую выбранную карточку.
              // Для индикации фокуса на карточке, используем Pot.focused.unavailable(), чтобы scAd.focused отличался от Pot.empty.
              val gridCore1 = MGridCoreS.ads
                .composeLens( MGridAds.interactWith )
                .set( Some((gridKeyPath, gridKey)) )(gridCore0)
              GridAh.resetFocus( m.gridPath, gridCore1)
            }
          )(v0)
          updatedSilent(v2, scrollFx)

        } else if (adData.isOpened) {
          // Карточка уже раскрыта. Синхронное сокрытие карточки.
          val adPtrs2 = scAdLoc
            .delete
            .get
            .toTree

          val gridAds2 = (
            MGridAds.adsTreePot
              .modify(_.map(_ => adPtrs2)) andThen
            // Текущее взаимодействие выставить на родительский элемент:
            MGridAds.interactWith.set {
              for {
                parentLoc  <- scAdLoc.parent
                firstBlock <- parentLoc.getLabel.gridItems.headOption
              } yield {
                parentLoc.gridKeyPath -> firstBlock.gridKey
              }
            }
          )(gridAds0)

          val jdRuntime2  = GridAh.mkJdRuntime(gridAds2, v0.core)
          val gridBuild2  = GridAh.rebuildGrid(gridAds2, v0.core.jdConf, jdRuntime2)
          val v2          = (
            MGridS.core.modify { core0 =>
              core0.copy(
                ads       = gridAds2,
                jdRuntime = jdRuntime2,
                gridBuild = gridBuild2
              )
            }
          )(v0)

          // В фоне - запустить скроллинг к началу карточки.
          val scrollFx      = GridAh.scrollToAdFx( scAdData, gridBuild2 )
          val resetRouteFx  = ResetUrlRoute().toEffectPure
          val fxs           = scrollFx + resetRouteFx
          updated(v2, fxs)

        } else if ( m.noOpen ) {
          // Ничего загружать не требуется, только прокрутить к указанной карточке.
          // Возможно, это переход "назад" из другой выдачи.
          val fx = GridAh.scrollToAdFx( scAdData, v0.core.gridBuild )
          effectOnly(fx)

        } else {
          // Запуск запроса за данными карточки на сервер.
          val fx = gridFocusReqFx(
            adIds = NonEmptyList( adData.doc.tagId.nodeId.get ),
            m = m,
          )

          var adPtrsModAcc = List.empty[Pot[Tree[MScAdData]] => Pot[Tree[MScAdData]]]
          val isPtrsPending = gridAds0.adsTreePot.isPending
          if (!isPtrsPending) {
            adPtrsModAcc ::= {
              ptrsTreePot: Pot[Tree[MScAdData]] =>
                ptrsTreePot.pending()
            }
          }

          // выставить текущей карточке, что она pending:
          if (!scAdData.data.isPending) {
            adPtrsModAcc ::= {
              ptrsTreePot: Pot[Tree[MScAdData]] =>
                for (_ <- ptrsTreePot) yield {
                  scAdLoc
                    .modifyLabel( MScAdData.data.modify(_.pending()) )
                    .toTree
                }
            }
          }

          // Не ясно, надо ли заменять значение interactWith, т.к. состояние плитки пока не изменилось...
          //if (!(v0.interactWith contains m.gridKey))
          //  modsAcc ::= MGridS.interactWith.set( Some(m.gridKey) )

          val v2Opt = adPtrsModAcc
            .reduceLeftOption(_ andThen _)
            .map { modF =>
              MGridS.core
                .composeLens( MGridCoreS.ads )
                .composeLens( MGridAds.adsTreePot )
                .modify( modF )(v0)
            }

          // silent = isPtrsPending: Т.к. пока это всё не влияет на рендер конкретной карточки, сверяем всё это лишь отчасти.
          ah.optionalResult( v2Opt, Some(fx), silent = isPtrsPending )
        }
      })
        .orElse {
          // Если задан adIds, надо поискать карточку с таким id или загрузить её с сервера.
          for {
            adIdFirst <- m.adIds.headOption
          } yield {
            // Поискать карточку среди имеющихся. Карточка может отсутствовать в состоянии, может быть в карте,
            // может быть в карте и дереве, может быть открытой и в виде main-блока.
            gridAds0
              .adsTreePot
              .iterator
              .flatMap { tree =>
                tree
                  .cobind(_.loc)
                  .flatten
                  .filter { loc =>
                    loc.getLabel.data.exists(
                      _.doc.tagId.nodeId.exists(
                        m.adIds.contains[String]))
                  }
                  .iterator
              }
              // Нужно разобраться, есть ли открытая карточка на руках?
              .find(_.getLabel.data.exists(_.isOpened))
              .fold {
                // Если нет в наличии сфокусированной карточки, то поискать хоть какую-нибудь, но запросить с сервера opened-вариант.
                var gridAdsMods = List.empty[MGridAds => MGridAds]

                // Дублирующийся main-блок в плитке не ищем: пусть он будет сгенерирован потом, иначе слишком много подводных камней и возможных ошибок.

                // Выставить pending для плитки, если ещё не выставлено:
                if (!gridAds0.adsTreePot.isPending)
                  gridAdsMods ::= MGridAds.adsTreePot.modify(_.pending())

                val v2Opt = gridAdsMods
                  .reduceLeftOption(_ andThen _)
                  .map { modF =>
                    MGridS.core
                      .composeLens( MGridCoreS.ads )
                      .modify(modF)( v0 )
                  }

                val focusReqFx = gridFocusReqFx(
                  adIds = NonEmptyList.fromSeq( adIdFirst, m.adIds.tail ),
                  m     = m,
                )

                ah.optionalResult( v2Opt, Some(focusReqFx), silent = false )

              } { openedAdLoc =>
                // Нужно просто прокрутить к уже раскрытой карточке.
                val fx = GridAh.scrollToAdFx( openedAdLoc.getLabel, v0.core.gridBuild )
                effectOnly(fx)
              }
          }
        }
        .getOrElse {
          // TODO Отработать LoadMore, когда m.noLoad (т.е. идёт возврат из другой выдачи)
          if (m.noOpen && v0.hasMoreAds) {
            val fx = Effect.action {
              GridLoadAds(
                clean         = false,
                ignorePending = false,
                afterLoadFx   = Some( m.toEffectPure ),
              )
            }
            effectOnly(fx)

          } else {
            logger.error( ErrorMsgs.NODE_NOT_FOUND, msg = m )
            noChange
          }
        }


    // Экшен запуска пересчёта конфигурации плитки.
    case m: GridRebuild =>
      val v0 = value
      val mscreen = screenRO.value
      val (gridColsCount2, szMult2) = GridAh.fullGridConf( mscreen.wh )

      val jdConf0 = v0.core.jdConf
      val szMultMatches = jdConf0.szMult ==* szMult2

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


  /** Эффект запроса к серверу на фокусировку карточки. */
  def gridFocusReqFx(adIds: NonEmptyList[String], m: GridBlockClick): Effect = {
    Effect {
      val qs = ScQsUtil.focAdsQs(
        scRootRO.value,
        adIds = adIds,
      )

      api
        .pubApi( qs )
        .transform { tryResp =>
          val r = HandleScApiResp(
            reqTimeStamp  = None,
            qs            = qs,
            tryResp       = tryResp,
            reason        = m,
            // Если сервер вернёт index ad open, то этот indexSwitch поможет потому вернутся юзеру назад.
            switchCtxOpt  = Some(
              MScSwitchCtx(
                // TODO indexQsArgs: Как тут None пропихнуть? Эти аргументы имеют мало смысла тут.
                indexQsArgs = MScIndexArgs(
                  geoIntoRcvr = false,
                  retUserLoc = false,
                ),
                afterBackGrid = Some( Effect.action {
                  m.copy(noOpen = true)
                } ),
                // В случае, если сервер пришлёт index ad open вместо плитки, новый индекс надо добавить в стопку поверх существующего.
                viewsAction = MScSwitchCtx.ViewsAction.PUSH,
              )
            ),
          )
          Success(r)
        }
    }
  }

}
