package io.suggest.sc.c.grid

import com.github.fisshy.react.scroll.AnimateScroll
import diode._
import diode.data.{PendingBase, Pot, Ready}
import io.suggest.ad.blk.BlockPaddings
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{MScreen, MSzMult}
import io.suggest.grid.build.{GridBuilderUtil, MGbBlock, MGridBuildArgs, MGridBuildResult}
import io.suggest.grid.{GridBuilderUtilJs, GridCalc, GridConst, GridScrollUtil, MGridCalcConf}
import io.suggest.jd.{MJdConf, MJdDoc}
import io.suggest.jd.render.m.{MJdDataJs, MJdRuntime}
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs}
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.{HandleScApiResp, MScRoot, ResetUrlRoute}
import io.suggest.sc.m.grid._
import io.suggest.sc.sc3._
import io.suggest.sc.styl.ScCss
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.geom.d2.IWidth
import io.suggest.jd.render.u.JdUtil
import io.suggest.primo.id.OptId
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DoNothing
import japgolly.univeq._
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

  /** Кол-во карточек за один ответ. */
  // TODO Рассчитывать кол-во карточек за 1 реквест на основе экрана и прочих вещей.
  def adsPerReqLimit = 10


  /** Полное переконфигурирование плитки.
    * @return 1 - кол-во колонок плитки.
    *         2 - szMult плитки.
    */
  def fullGridConf(mscreen: IWidth): (Int, MSzMult) = {
    val gridConf = GRID_CONF

    val gridColsCount = GridCalc.getColumnsCount(
      contSz = mscreen,
      conf   = gridConf
    )
    val gridSzMult = GridCalc.getSzMult4tilesScr(gridColsCount, mscreen, gridConf)
    (gridColsCount, gridSzMult)
  }


  /** Выполнение ребилда плитки. */
  def rebuildGrid(ads: Pot[Seq[MScAdData]], jdConf: MJdConf, jdRuntime: MJdRuntime): MGridBuildResult = {

    /** Конвертация одной карточки в один блок для рендера в плитке. */
    def blockRenderData2GbPayload(nodeId: Option[String], stripTpl: Tree[JdTag], brd: MJdDataJs): Tree[MGbBlock] = {
      // Несфокусированная карточка. Вернуть blockMeta с единственного стрипа.
      Tree.Leaf {
        val stripJdt = stripTpl.rootLabel

        val bmOpt = stripJdt.props1.bm
        val wideBgBlk = for {
          bm      <- bmOpt
          if bm.expandMode.nonEmpty
          //_       <- OptionUtil.maybeTrue( bm.wide )
          bg      <- stripJdt.props1.bgImg
          // 2018-01-23: Для wide-фона нужен отдельный блок, т.к. фон позиционируется отдельно от wide-block-контента.
          // TODO Нужна поддержка wide-фона без картинки.
          bgEdge  <- brd.edges.get( bg.edgeUid )
          imgWh   <- bgEdge.origWh
        } yield {
          imgWh
        }

        MGbBlock(
          size   = GridBuilderUtilJs.gbSizeFromJdt(stripJdt, jdRuntime, jdConf),
          nodeId = nodeId,
          jdtOpt = Some(stripJdt),
          wideBgSz = wideBgBlk
        )
      }
    }

    // Приведение списка карточек к grid-блокам и подблоков обсчёта плитки.
    val itmDatas = ads
      .toIterator
      .flatten
      .map { scAdData =>
        scAdData.focused.fold [Tree[MGbBlock]] {
          // Несфокусированная карточка. Вернуть bm единственного стрипа.
          val brd = scAdData.main
          blockRenderData2GbPayload( scAdData.nodeId, brd.doc.template, brd )
        } { foc =>
          // Открытая карточка. Вернуть MGbSubItems со списком фокус-блоков:
          Tree.Node(
            root = MGbBlock(
              size   = GridBuilderUtilJs.gbSizeFromJdt(
                jdt = foc.blkData
                  .doc.template
                  .rootLabel,
                jdRuntime = jdRuntime,
                jdConf    = jdConf,
              ),
              nodeId = scAdData.nodeId,
              jdtOpt = None,
            ),
            forest = foc.blkData
              .doc.template
              .subForest
              .iterator
              .map { subTpl =>
                blockRenderData2GbPayload( scAdData.nodeId, subTpl, foc.blkData )
              }
              .toStream
          )
        }
      }
      .toStream

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
  def mkJdRuntime(ads: Pot[Seq[MScAdData]], jdConf: MJdConf): MJdRuntime = {
    JdUtil.mkRuntime(
      docs = ads
        .iterator
        .flatten
        .flatMap(_.flatGridTemplates)
        .toStream,
      jdConf = jdConf,
    )
  }


  /** Эффект скроллинга к указанной карточке. */
  def scrollToAdFx(toAd    : MScAdData,
                   ads     : Pot[Seq[MScAdData]],
                   gbRes   : MGridBuildResult
                  ): Effect = {
    // Карточка уже открыта, её надо свернуть назад в main-блок.
    // Нужно узнать координату в плитке карточке
    Effect.action {
      val nodeIdOpt = toAd.nodeId
      val yCoordsIter = gbRes.coords
        .iterator
        .zip( ads.iterator.flatten )
        .filter { case (_, scAdData) =>
          scAdData.nodeId ==* nodeIdOpt
        }

      if (yCoordsIter.nonEmpty) {
        // Выбрать самый верхний блок карточки, он не обязательно первый по порядку идёт.
        val toY = yCoordsIter
          .map(_._1.y)
          .min
        AnimateScroll.scrollTo(
          // Сдвиг обязателен, т.к. карточки заезжают под заголовок.
          to = Math.max(0, toY - ScCss.HEADER_HEIGHT_PX - BlockPaddings.default.value),
          options = GridScrollUtil.scrollOptions
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
      .toOption
      .flatMap { ads =>
        ads
          .iterator
          .zipWithIndex
          .find { _._1.nodeId contains nodeId }
      }
  }

}


/** Поддержка resp-handler'а для карточек плитки без фокусировки. */
class GridRespHandler
  extends IRespWithActionHandler
  with Log
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[GridLoadAds]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( ctx.value0.grid.core.ads )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): MScRoot = {
    MScRoot.grid
      .composeLens(MGridS.core)
      .composeLens( MGridCoreS.ads )
      .modify( _.fail(ex) )( ctx.value0 )
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.AdsTile
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    val gridResp = ra.ads.get
    val g0 = ctx.value0.grid

    val isSilentOpt = ctx.m.reason match {
      case gla: GridLoadAds => gla.silent
      case _ => None
    }

    // Нельзя тут использовать ctx.m.reason: причина относится только к начальному resp-экшену (и то необязательно).
    val isCleanLoad = ctx.m.qs.search.offset
      .fold(true)(_ ==* 0)

    // Если silent, то надо попытаться повторно пере-использовать уже имеющиеся карточки.
    val reusableAdsMap: Map[String, MScAdData] = {
      if (
        isCleanLoad &&
        (isSilentOpt contains true) &&
        gridResp.ads.nonEmpty &&
        g0.core.ads.nonEmpty
      ) {
        // Есть условия для сборки карты текущих карточек:
        OptId.els2idMap(
          g0.core.ads
            .iterator
            .flatten
        )
      } else {
        // Сборка карты текущих карточек не требуется в данной ситуации.
        Map.empty
      }
    }

    // Подготовить полученные с сервера карточки:
    val newScAds = gridResp.ads
      .iterator
      .map { sc3AdData =>
        // Если есть id и карта переиспользуемых карточек не пуста, то поискать там текущую карточку:
        sc3AdData.jd.doc.jdId.nodeId
          .filter( _ => reusableAdsMap.nonEmpty )
          .flatMap( reusableAdsMap.get )
          // Если карточка не найдена среди reusable-карточек, то перейки к сброке состояния новой карточки:
          .getOrElse {
            // Собрать начальное состояние карточки.
            // Сервер может присылать уже открытые карточи - это нормально.
            // Главное - их сразу пропихивать и в focused, и в обычные блоки.
            val jdDoc0 = sc3AdData.jd.doc
            val isFocused = jdDoc0.template.rootLabel.name ==* MJdTagNames.DOCUMENT
            val jsEdgesMap = MEdgeDataJs.jdEdges2EdgesDataMap( sc3AdData.jd.edges )

            MScAdData(
              main = MJdDataJs(
                doc = {
                  if (isFocused) {
                    // Найти главный блок в шаблоне focused-карточки документа.
                    MJdDoc.template
                      .set(jdDoc0.template.getMainBlockOrFirst)( jdDoc0 )
                  } else jdDoc0
                },
                edges   = jsEdgesMap,
              ),
              focused = if (isFocused) {
                // Сервер прислал focused-карточку.
                val v = MScFocAdData(
                  MJdDataJs(
                    doc   = jdDoc0,
                    edges = jsEdgesMap,
                  ),
                  canEdit = sc3AdData.canEdit.getOrElseFalse,
                  userFoc = false
                )
                Ready(v)
              } else {
                // Обычный grid-block.
                Pot.empty
              }
            )
          }
      }
      .toVector

    // Самоконтроль для отладки: Проверить, совпадает ли SzMult между сервером и клиентом?
    //if (gridResp.szMult !=* g0.core.jdConf.szMult)
    //  LOG.warn(WarnMsgs.SERVER_CLIENT_SZ_MULT_MISMATCH, msg = (gridResp.szMult, g0.core.jdConf.szMult))


    // Опциональный эффект скролла вверх.
    val scrollFxOpt = {
      // Возможно, требование скролла задано принудительно в исходном запросе перезагрузки плитки?
      val isScrollUp = isSilentOpt
        .map(!_)
        .getOrElse( isCleanLoad )
      // А если вручную не задано, то определить нужность скроллинга автоматически:
      OptionUtil.maybe(isScrollUp) {
        Effect.action {
          AnimateScroll.scrollToTop( GridScrollUtil.scrollOptions )
          DoNothing
        }
      }
    }

    val ads2 = if (isCleanLoad) {
      g0.core.ads.ready( newScAds )

    } else {
      val scAds2 = g0.core.ads.toOption
        .fold(newScAds)(_ ++ newScAds)
      // ready - обязателен, иначе останется pending и висячий без дела GridLoaderR.
      g0.core.ads.ready( scAds2 )
    }

    val jdRuntime2 = GridAh.mkJdRuntime(ads2, g0.core.jdConf)
    val g2 = g0.copy(
      core = g0.core.copy(
        jdRuntime   = jdRuntime2,
        ads         = ads2,
        // Отребилдить плитку:
        gridBuild   = GridAh.rebuildGrid(ads2, g0.core.jdConf, jdRuntime2)
      ),
      hasMoreAds  = ctx.m.qs.search.limit.fold(true) { limit =>
        gridResp.ads.lengthCompare(limit) >= 0
      }
    )

    // И вернуть новый акк:
    val v2 = ctx.value0.withGrid(g2)
    ActionResult(Some(v2), scrollFxOpt)
  }

}


/** Resp-handler для обработки ответа по фокусировке одной карточки. */
class GridFocusRespHandler
  extends IRespWithActionHandler
  with Log
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[GridBlockClick]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    ctx.value0
      .grid.core
      .focusedAdOpt
      .map(_.focused)
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): MScRoot = {
    LOG.error(ErrorMsgs.XHR_UNEXPECTED_RESP, ex, msg = ctx.m)
    val g0 = ctx.value0.grid
    val reason = ctx.m.reason.asInstanceOf[GridBlockClick]
    GridAh
      .findAd(reason.nodeId, g0.core)
      .fold( ctx.value0 ) { case (ad0, index) =>
        val ad1 = MScAdData.focused
          .modify(_.fail(ex))(ad0)
        val g2 = GridAh.saveAdIntoValue(index, ad1, g0)
        ctx.value0.withGrid( g2 )
      }
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.AdsFoc
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    val focQs = ctx.m.qs.foc.get
    val nodeId = focQs.lookupAdId
    val g0 = ctx.value0.grid
    GridAh
      .findAd(nodeId, g0.core)
      .fold [ActionResult[MScRoot]] {
        LOG.warn(ErrorMsgs.FOC_LOOKUP_MISSING_AD, msg = focQs)
        ActionResult.NoChange //ModelUpdate( ctx.value0 )

      } { case (ad0, index) =>
        val focResp = ra.ads.get
        val focAd = focResp.ads.head
        val ad1 = MScAdData.focused
          .modify(
            _.ready(
              MScFocAdData(
                blkData = MJdDataJs( focAd.jd ),
                canEdit = focAd.canEdit contains true,
                userFoc = true
              )
            )
          )(ad0)

        val adsPot2 = for (ads0 <- g0.core.ads) yield {
          ads0
            .iterator
            .zipWithIndex
            .map { case (xad0, i) =>
              if (i ==* index) {
                // Раскрыть выбранную карточку.
                ad1
              } else if (xad0.focused.nonEmpty) {
                // Скрыть все уже открытык карточки.
                MScAdData.focused
                  .set( Pot.empty )(xad0)
              } else {
                // Нераскрытые карточки - пропустить без изменений.
                xad0
              }
            }
            .toVector
        }

        val jdRuntime2 = GridAh.mkJdRuntime( adsPot2, g0.core.jdConf )
        val gridBuild2 = GridAh.rebuildGrid( adsPot2, g0.core.jdConf, jdRuntime2 )
        val g2 = MGridS.core.modify(
          _.copy(
            jdRuntime = jdRuntime2,
            ads       = adsPot2,
            gridBuild = gridBuild2
          )
        )(g0)
        // Надо проскроллить выдачу на начало открытой карточки:
        val scrollFx = GridAh.scrollToAdFx( ad1, adsPot2, gridBuild2 )
        val resetRouteFx = ResetUrlRoute.toEffectPure

        val v2 = MScRoot.grid.set(g2)( ctx.value0 )
        val fxOpt = Some(scrollFx + resetRouteFx)
        ActionResult(Some(v2), fxOpt)
      }
  }

}


/** Контроллер плитки карточек.
  * @param scQsRO Доступ к текущим аргументам поиска карточек.
  */
class GridAh[M](
                 api             : IScUniApi,
                 scQsRO          : ModelRO[MScQs],
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
          val screenHeight = screenRO.value.height
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
      //println(m)
      if (v0.core.ads.isPending && !m.ignorePending) {
        LOG.warn( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.core.ads) )
        noChange

      } else {
        val args0 = scQsRO.value
        val nextReqPot2 = v0.core.ads.pending()

        val fx = Effect {
          // Если clean, то нужно обнулять offset.
          val offset = v0.core.ads
            .filter(_ => !m.clean)
            .fold(0)(_.size)

          val args2 = args0.copy(
            search = args0.search
              .withLimitOffset(
                limit  = Some( GridAh.adsPerReqLimit ),
                offset = Some(offset)
              ),
            common = MScCommonQs.searchGridAds
              .set( Some(false) )(args0.common)
          )

          // Запустить запрос с почищенными аргументами...
          val fut = api.pubApi( args2 )

          // Завернуть ответ сервера в экшен:
          val startTime = nextReqPot2.asInstanceOf[PendingBase].startTime
          fut.transform { tryRes =>
            val r = HandleScApiResp(
              reqTimeStamp  = Some(startTime),
              qs            = args2,
              tryResp       = tryRes,
              reason        = m,
            )
            Success(r)
          }
        }

        val v2 = MGridS.core
          .composeLens( MGridCoreS.ads )
          .set( nextReqPot2 )(v0)
        updated(v2, fx)
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
          LOG.error( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange

        } { case (ad0, index) =>
          ad0.focused.fold {
            // Карточка сейчас скрыта, её нужно раскрыть.
            // Собрать запрос фокусировки на ровно одной рекламной карточке.
            val fx = Effect {
              val args0 = scQsRO.value
              val args1 = args0.copy(
                search = MAdsSearchReq(
                  rcvrId = args0.search.rcvrId
                ),
                // TODO common: надо выставлять подгрузку grid-карточек при перескоке foc->index, чтобы плитка приходила сразу?
                foc = Some(
                  MScFocusArgs(
                    focIndexAllowed  = true,
                    lookupMode       = None,
                    lookupAdId       = m.nodeId
                  )
                )
              )
              api.pubApi( args1 )
                .transform { tryResp =>
                  val r = HandleScApiResp(
                    reqTimeStamp  = None,
                    qs            = args1,
                    tryResp       = tryResp,
                    reason        = m,
                  )
                  Success(r)
                }
            }

            // выставить текущей карточке, что она pending
            val ad1 = MScAdData.focused
              .modify(_.pending())(ad0)

            val v2 = GridAh.saveAdIntoValue(index, ad1, v0)
            updated(v2, fx)

          } { _ =>
            val ad1         = MScAdData.focused.set( Pot.empty )(ad0)
            val ads2        = GridAh.saveAdIntoAds(index, ad1, v0)
            val jdRuntime2  = GridAh.mkJdRuntime(ads2, v0.core.jdConf)
            val gridBuild2  = GridAh.rebuildGrid(ads2, v0.core.jdConf, jdRuntime2)
            val v2          = MGridS.core.modify { core0 =>
              core0.copy(
                ads       = ads2,
                jdRuntime = jdRuntime2,
                gridBuild = gridBuild2
              )
            }(v0)
            // В фоне - запустить скроллинг к началу карточки.
            val scrollFx      = GridAh.scrollToAdFx( ad0, ads2, gridBuild2 )
            val resetRouteFx  = ResetUrlRoute.toEffectPure
            val fxs           = scrollFx + resetRouteFx
            updated(v2, fxs)
          }
        }


    // Экшен запуска пересчёта конфигурации плитки.
    case GridReConf =>
      val v0 = value
      val mscreen = screenRO.value
      val (gridColsCount2, szMult2) = GridAh.fullGridConf( mscreen )

      val jdConf0 = v0.core.jdConf
      val szMultMatches = jdConf0.szMult ==* szMult2
      //println( s"gridColumnCount=$gridColsCount2 $szMultMatches ${jdConf0.szMult} => $szMult2" )

      if (szMultMatches && jdConf0.gridColumnsCount ==* gridColsCount2) {
        noChange

      } else {
        val jdConf1 = {
          var jdConfLens = MJdConf.gridColumnsCount.set( gridColsCount2 )
          if (!szMultMatches)
            jdConfLens = jdConfLens andThen MJdConf.szMult.set(szMult2)
          jdConfLens( jdConf0 )
        }

        val jdRuntime2 =
          if (szMultMatches) v0.core.jdRuntime
          else GridAh.mkJdRuntime(v0.core.ads, jdConf1)

        var coreLens = (
          MGridCoreS.jdConf.set( jdConf1 ) andThen
          MGridCoreS.gridBuild.set( GridAh.rebuildGrid(v0.core.ads, jdConf1, jdRuntime2) )
        )

        // Если изменился szMult, то перепилить css-стили карточек:
        if (!szMultMatches)
          coreLens = (
            coreLens andThen
            MGridCoreS.jdRuntime.set( jdRuntime2 )
          )

        val v2 = MGridS.core.modify(coreLens)(v0)

        // TODO Возможно, что надо перекачать содержимое плитки с сервера, если всё слишком сильно переменилось. Нужен отложенный таймер для этого.
        updated(v2)
      }

  }

}
