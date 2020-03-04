package io.suggest.sc.c.grid

import com.github.fisshy.react.scroll.AnimateScroll
import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.ad.blk.BlockPaddings
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{MScreen, MSzMult}
import io.suggest.grid.build.{GridBuilderUtil, MGbBlock, MGridBuildArgs, MGridBuildResult}
import io.suggest.grid.{GridBuilderUtilJs, GridCalc, GridConst, GridScrollUtil, MGridCalcConf}
import io.suggest.jd.{MJdConf, MJdTagId}
import io.suggest.jd.render.m.{GridRebuild, MJdDataJs, MJdRuntime}
import io.suggest.jd.tags.JdTag
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs}
import io.suggest.sc.m.{HandleScApiResp, ResetUrlRoute}
import io.suggest.sc.m.grid._
import io.suggest.sc.sc3._
import io.suggest.sc.styl.ScCss
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.geom.d2.IWidth
import io.suggest.jd.render.u.JdUtil
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
          blockRenderData2GbPayload( brd.doc.template.rootLabel, brd, brd.doc.jdId )

        } { fullAdData =>
          // Открытая карточка. Вернуть MGbSubItems со списком фокус-блоков:
          Tree.Node(
            root = {
              val jdt = fullAdData.doc.template.rootLabel
              val jdId = fullAdData.doc.jdId

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
          gbRes.coordsById.get( scAd.jdId )
        }
        // Взять только самый верхний блок карточки. Он должен быть первым по порядку:
        .nextOption()
        .foreach { toXY =>
          AnimateScroll.scrollTo(
            // Сдвиг обязателен, т.к. карточки заезжают под заголовок.
            to = Math.max(0, toXY.y - ScCss.HEADER_HEIGHT_PX - BlockPaddings.default.value),
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
      .iterator
      .flatten
      .zipWithIndex
      .find { _._1.nodeId contains nodeId }
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
      //println(m)
      if (v0.core.ads.isPending && !m.ignorePending) {
        LOG.warn( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.core.ads) )
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
            common = (MScCommonQs.searchGridAds set OptionUtil.SomeBool.someFalse)(args0.common)
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
          if (ad0.isAlwaysOpened) {
            // Клик по всегда развёрнутой карточке должен приводить к скроллу к началу карточки без загрузки.
            val scrollFx = GridAh.scrollToAdFx( ad0, v0.core.gridBuild )
            effectOnly(scrollFx)

          } else {
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

      if (!m.force && szMultMatches && jdConf0.gridColumnsCount ==* gridColsCount2) {
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
