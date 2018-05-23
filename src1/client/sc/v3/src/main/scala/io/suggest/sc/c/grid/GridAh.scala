package io.suggest.sc.c.grid

import com.github.fisshy.react.scroll.{AnimateScroll, LinkProps}
import diode._
import diode.data.{PendingBase, Pot, Ready}
import io.suggest.ad.blk.BlockPaddings
import io.suggest.dev.{MScreen, MSzMults}
import io.suggest.err.ErrorConstants
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.render.v.{JdCss, JdCssFactory}
import io.suggest.jd.tags.MJdTagNames
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.react.ReactDiodeUtil.PotOpsExt
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sc.m.grid._
import io.suggest.sc.m.inx.HandleIndexResp
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.jd.tags.JdTag.Implicits._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.grid.{GridCalc, GridConst, GridScrollUtil, MGridCalcConf}
import io.suggest.grid.build.{GridBuilderUtil, MGridBuildArgs, MGridBuildResult}
import io.suggest.sc.styl.ScCss
import japgolly.univeq._
import io.suggest.react.ReactDiodeUtil.ActionHandlerExt
import io.suggest.sc.sc3.MScRespActionTypes

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:59
  * Description: Утиль контроллера плитки карточек.
  */
object GridAh {

  private def GRID_CONF = MGridCalcConf.EVEN_GRID

  /** Частичное переконфигурирование плитки. */
  private def reconfGridColumnsCount(mscreen: MScreen,
                                     gridConf: MGridCalcConf = GRID_CONF,
                                     minSzMult: Double = MSzMults.GRID_MIN_SZMULT_D): Int = {
    val evenGridColsCount = GridCalc.getColumnsCount(
      // TODO Надо учесть фактическую ширину, т.е. вычесть открытые боковые панели.
      contSz    = mscreen,
      conf      = gridConf,
      minSzMult = minSzMult
    )
    Math.max(2, evenGridColsCount * gridConf.cellWidth.relSz)
  }


  /** Полное переконфигурирование плитки. */
  def fullGridConf(mscreen: MScreen): MJdConf = {
    val gridConf = GRID_CONF

    val gridColsCount = reconfGridColumnsCount(mscreen, gridConf)
    MJdConf(
      isEdit = false,
      szMult = GridCalc.getSzMult4tilesScr(gridColsCount, mscreen, gridConf),
      gridColumnsCount = gridColsCount
    )
  }

}


/** Контроллер плитки карточек.
  * @param searchArgsRO Доступ к текущим аргументам поиска карточек.
  */
class GridAh[M](
                 api             : IFindAdsApi,
                 searchArgsRO    : ModelRO[MFindAdsReq],
                 screenRO        : ModelRO[MScreen],
                 jdCssFactory    : JdCssFactory,
                 modelRW         : ModelRW[M, MGridS]
               )
  extends ActionHandler(modelRW)
  with Log
{ ah =>


  /** Простая и ресурсоёмкая пересборка CSS карточек. */
  private def _mkJdCss(ads: Pot[Seq[MScAdData]], jdConf: MJdConf): JdCss = {
    jdCssFactory.mkJdCss(
      MJdCssArgs(
        templates = ads
          .iterator
          .flatten
          .flatMap(_.flatGridTemplates)
          .toSeq,
        conf = jdConf
      )
    )
  }


  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

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
        val fx = Effect.action {
          GridLoadAds(clean = false, ignorePending = true)
        }
        // Выставить pending в состояние, чтобы повторные события скролла игнорились.
        val v2 = v0.withCore(
          v0.core
            .withAds( v0.core.ads.pending() )
        )
        updatedSilent(v2, fx)

      } else {
        // Больше нет карточек, или запрос карточек уже в процессе, или скроллинг не требует подгрузки карточек.
        noChange
      }


    // Сигнал к загрузке карточек с сервера согласно текущему состоянию выдачи.
    case m: GridLoadAds =>
      val v0 = value
      if (v0.core.ads.isPending && !m.ignorePending) {
        LOG.warn( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.core.ads) )
        noChange

      } else {
        val searchArgs = searchArgsRO.value
        val nextReqPot2 = v0.core.ads.pending()
        val fx = Effect {
          // Если clean, то нужно обнулять offset.
          val offset = v0.core.ads
            .filter(_ => !m.clean)
            .fold(0)(_.size)

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

        val v2 = v0.withCore(
          v0.core
            .withAds( nextReqPot2 )
        )
        updated(v2, fx)
      }


    // Пришёл результат запроса карточек с сервера.
    case m: GridLoadAdsResp =>
      // Сверить timestamp с тем, что внутри Pot'а.
      val v0 = value
      if ( v0.core.ads.isPendingWithStartTime(m.startTime) ) {
        // Это и есть ожидаемый ответ сервера. Разобраться, что там внутри...
        m.resp.fold(
          {ex =>
            // Записать ошибку в состояние.
            val v2 = v0.withCore(
              v0.core
                .withAds( v0.core.ads.fail(ex) )
            )
            updated(v2)
          },
          {scResp =>
            // Сервер ответил что-то вразумительное.
            // Пока поддерживается только чистый findAds-ответ, поэтому остальные варианты игнорим.
            val scAction = scResp.respActions.head
            ErrorConstants.assertArg( scAction.acType ==* MScRespActionTypes.AdsTile )
            val findAdsResp = scAction.ads.get

            // Подготовить полученные с сервера карточки:
            val newScAds = findAdsResp.ads
              .iterator
              .map { sc3AdData =>
                // Собрать начальное состояние карточки.
                // Сервер может присылать уже открытые карточи - это нормально.
                // Главное - их сразу пропихивать и в focused, и в обычные блоки.
                val isFocused = sc3AdData.jd.template.rootLabel.name ==* MJdTagNames.DOCUMENT
                val jsEdgesMap = sc3AdData.jd
                  .edgesMap
                  .mapValues(MEdgeDataJs(_))

                MScAdData(
                  nodeId    = sc3AdData.jd.nodeId,
                  main      = MBlkRenderData(
                    template  = if (isFocused) {
                      // Найти главный блок в шаблоне focused-карточки документа.
                      sc3AdData.jd.template.getMainBlockOrFirst
                    } else {
                      sc3AdData.jd.template
                    },
                    edges     = jsEdgesMap
                  ),
                  focused = if (isFocused) {
                    // Сервер прислал focused-карточку.
                    val v = MScFocAdData(
                      MBlkRenderData(
                        template  = sc3AdData.jd.template,
                        edges     = jsEdgesMap
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
              .toVector

            val (ads2, jdConf2, fxOpt) = if (m.evidence.clean) {
              val jdConf1 = v0.core.jdConf.withSzMult(
                findAdsResp.szMult
              )
              val ads1 = v0.core.ads.ready(newScAds)
              val scrollFx: Effect = Effect.action {
                AnimateScroll.scrollToTop( _scrollOptions )
                GridScrollDone
              }
              (ads1, jdConf1, Some(scrollFx))

            } else {
              // Проверить, совпадает ли SzMult:
              ErrorConstants.assertArg( findAdsResp.szMult ==* v0.core.jdConf.szMult )
              val scAds2 = v0.core.ads.toOption.fold(newScAds)(_ ++ newScAds)
              // ready - обязателен, иначе останется pending и висячий без дела GridLoaderR.
              val ads1 = v0.core.ads.ready( scAds2 )
              (ads1, v0.core.jdConf, None)
            }

            val v2 = v0.copy(
              core = v0.core.copy(
                jdConf      = jdConf2,
                jdCss       = _mkJdCss(ads2, jdConf2),
                ads         = ads2,
                // Отребилдить плитку:
                gridBuild   = _rebuildGrid(ads2, jdConf2)
              ),
              hasMoreAds  = findAdsResp.ads.lengthCompare(m.limit) >= 0
            )

            ah.updatedMaybeEffect(v2, fxOpt)
          }
        )

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
      _findAd(m.nodeId, v0.core)
        .fold {
          LOG.error( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange

        } { case (ad0, index) =>
          ad0.focused.fold {
            // Карточка сейчас скрыта, её нужно раскрыть.
            // Собрать запрос фокусировки на ровно одной рекламной карточке.
            val fx = Effect {
              val args0 = searchArgsRO.value
              val args = MFindAdsReq(
                allowReturnJump = Some( true ),
                adIdLookup      = Some( m.nodeId ),
                adsLookupMode   = None,
                screen          = args0.screen,
                receiverId      = args0.receiverId
              )
              api.focusedAds(args)
                .transform { tryResp =>
                  Success( FocusedResp(m.nodeId, tryResp) )
                }
            }

            // выставить текущей карточке, что она pending
            val ad1 = ad0.withFocused( ad0.focused.pending() )

            val v2 = _saveAdIntoValue(index, ad1, v0)
            updated(v2, fx)

          } { _ =>
            val ad1 = ad0.withFocused( Pot.empty )
            val ads2 = _saveAdIntoAds(index, ad1, v0)
            val v2 = v0.withCore(
              v0.core.copy(
                ads       = ads2,
                jdCss     = _mkJdCss(ads2, v0.core.jdConf),
                gridBuild = _rebuildGrid(ads2, v0.core.jdConf)
              )
            )
            // В фоне - запустить скроллинг к началу карточки.
            val scrollFx = _scrollToAdFx( ad0, ads2, v2.core.gridBuild )
            updated(v2, scrollFx)
          }
        }


    // Сигнал завершения запроса фокусировки на какой-либо карточке
    case m: FocusedResp =>
      val v0 = value

      // Найти фокусируемую рекламную карточку
      _findAd(m.nodeId, v0.core)
        .fold {
          // Не найдена искомая карточка.
          LOG.warn( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange

        } { case (ad0, index) =>
          // Есть искомая карточка. Перейти к обработке результата запроса.
          m.tryResp.fold(
            // Какая-то ошибка запроса этой рекламной карточки.
            {ex =>
              LOG.error(ErrorMsgs.XHR_UNEXPECTED_RESP, ex, msg = m)
              val ad1 = ad0.withFocused(
                ad0.focused.fail(ex)
              )
              val v2 = _saveAdIntoValue(index, ad1, v0)
              updated(v2)
            },
            {sc3Resp =>
              // Может прийти редирект в другую выдачу, а может -- раскрытие карточки.
              val ra = sc3Resp.respActions.head
              ra.acType match {
                // Редирект в другую выдачу. Форсируем переключение в новую плитку.
                case MScRespActionTypes.Index =>
                  val fx = Effect.action {
                    HandleIndexResp(m.tryResp, reqTimestamp = None, reason = None)
                  }
                  effectOnly( fx )

                // Фокусировка: раскрыть текущую карточку с помощью принятого контента.
                case MScRespActionTypes.AdsFoc =>
                  val adsResp = ra.ads.get
                  val focAd = adsResp.ads.head
                  val ad1 = ad0.withFocused(
                    ad0.focused.ready(
                      MScFocAdData(
                        blkData = MBlkRenderData( focAd.jd ),
                        canEdit = focAd.canEdit.contains(true),
                        userFoc = true
                      )
                    )
                  )
                  val adsPot2 = for (ads0 <- v0.core.ads) yield {
                    ads0
                      .iterator
                      .zipWithIndex
                      .map { case (xad0, i) =>
                        if (i ==* index) {
                          // Раскрыть выбранную карточку.
                          ad1
                        } else if (xad0.focused.nonEmpty) {
                          // Скрыть все уже открытык карточки.
                          xad0.withFocused( Pot.empty )
                        } else {
                          // Нераскрытые карточки - пропустить без изменений.
                          xad0
                        }
                      }
                      .toVector
                  }
                  val v2 = v0.withCore(
                    v0.core.copy(
                      jdCss     = _mkJdCss(adsPot2, v0.core.jdConf),
                      ads       = adsPot2,
                      gridBuild = _rebuildGrid( adsPot2, v0.core.jdConf )
                    )
                  )
                  // Надо проскроллить выдачу на начало открытой карточки:
                  val scrollFx = _scrollToAdFx( ad1, adsPot2, v2.core.gridBuild )
                  updated(v2, scrollFx)

                case other =>
                  LOG.error( ErrorMsgs.UNSUPPORTED_VALUE_OF_ARGUMENT, msg = (m, other) )
                  throw new UnsupportedOperationException(other.toString)
              }
            }
          )
        }


    // Экшен запуска пересчёта конфигурации плитки.
    case GridReConf =>
      val v0 = value
      val gridColsCount2 = GridAh.reconfGridColumnsCount(
        mscreen   = screenRO.value,
        gridConf  = MGridCalcConf.PLAIN_GRID,
        minSzMult = v0.core.jdConf.szMult.toDouble
      )
      if (v0.core.jdConf.gridColumnsCount ==* gridColsCount2) {
        noChange

      } else {
        val jdConf2 = v0.core.jdConf
          .withGridColumnsCount( gridColsCount2 )
        val v2 = v0.withCore(
          v0.core
            .withJdConf( jdConf2 )
        )
        // TODO Возможно, что надо перекачать содержимое плитки с сервера, если всё слишком сильно переменилось. Нужен отложенный таймер для этого.
        updated(v2)
      }


    // Сообщение об окончании авто-скроллинга. TODO Это ненужный костыль. Он существует просто потому что Effect должен возвращать хоть какой-то экшен.
    case GridScrollDone =>
      noChange

  }


  /** Найти карточку с указанным id в состоянии, вернув её и её индекс. */
  private def _findAd(nodeId: String, v0: MGridCoreS): Option[(MScAdData, Int)] = {
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
  def _saveAdIntoAds(index: Int, newAd: MScAdData, v0: MGridS = value) = {
    for (ads0 <- v0.core.ads) yield {
      ads0.updated(index, newAd)
    }
  }
  def _saveAdIntoValue(index: Int, newAd: MScAdData, v0: MGridS = value): MGridS = {
    v0.withCore(
      v0.core.withAds(
        _saveAdIntoAds(index, newAd, v0)
      )
    )
  }


  /** Эффект скроллинга к указанной карточке. */
  private def _scrollToAdFx(toAd    : MScAdData,
                            ads     : Pot[Seq[MScAdData]],
                            gbRes   : MGridBuildResult
                           ): Effect = {
    // Карточка уже открыта, её надо свернуть назад в main-блок.
    // Нужно узнать координату в плитке карточке
    val nodeIdOpt = toAd.nodeId
    Effect.action {
      gbRes.coords
        .iterator
        .zip( ads.iterator.flatten )
        .find { case (_, scAdData) =>
          scAdData.nodeId ==* nodeIdOpt
        }
        .foreach { case (mcoordPx, _) =>
          AnimateScroll.scrollTo(
            // Сдвиг обязателен, т.к. карточки заезжают под заголовок.
            to = Math.max(0, mcoordPx.y - ScCss.HEADER_HEIGHT_PX - BlockPaddings.default.value),
            options = _scrollOptions
          )
        }
      GridScrollDone
    }
  }

  /** Константна с параметрами скроллинга. */
  private def _scrollOptions: LinkProps = {
    new LinkProps {
      override val smooth = true
      // Надо скроллить grid wrapper, а не document:
      override val containerId = GridScrollUtil.SCROLL_CONTAINER_ID
    }
  }


  /** Запуск ребилда плитки. */
  def _rebuildGrid(ads: Pot[Seq[MScAdData]], jdConf: MJdConf): MGridBuildResult = {
    GridBuilderUtil.buildGrid(
      MGridBuildArgs(
        columnsCount  = jdConf.gridColumnsCount,
        itemsExtDatas = MGridCoreS
          .ads2gridBlocks( ads.iterator.flatten )
          .toList,
        jdConf        = jdConf,
        offY          = GridConst.CONTAINER_OFFSET_TOP
      )
    )
  }

}
