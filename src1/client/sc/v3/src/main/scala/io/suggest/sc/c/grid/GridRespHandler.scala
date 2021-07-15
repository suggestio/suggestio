package io.suggest.sc.c.grid

import com.github.fisshy.react.scroll.AnimateScroll
import diode.data.Pot
import diode.{ActionResult, Effect, ModelRO}
import io.suggest.grid.GridScrollUtil
import io.suggest.grid.build.{MGridBuildResult, MGridRenderInfo}
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.jd.render.u.JdUtil
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.grid._
import io.suggest.sc.m.{MScRoot, SetErrorState}
import io.suggest.sc.sc3.{MSc3RespAction, MScRespActionType, MScRespActionTypes}
import io.suggest.sc.v.toast.ScNotifications
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import japgolly.univeq._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.03.2020 16:44
  * Description: Обработчик блока данных с карточками плитки.
  */

/** Поддержка resp-handler'а для карточек плитки без фокусировки. */
final class GridRespHandler(
                             scNotifications    : ScNotifications,
                             isDoOsNotify       : ModelRO[Boolean],
                           )
  extends IRespWithActionHandler
  with Log
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[GridLoadAds]
  }

  private def _scRoot_grid_core_ads_adPtrs_LENS = MScRoot.grid
    .composeLens( MGridS.core )
    .composeLens( MGridCoreS.ads )
    .composeLens( MGridAds.adsTreePot )

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( _scRoot_grid_core_ads_adPtrs_LENS.get( ctx.value0 ) )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): ActionResult[MScRoot] = {
    val lens = _scRoot_grid_core_ads_adPtrs_LENS

    val v2 = (lens modify (_.fail(ex)) )( ctx.value0 )

    val errFx = Effect.action {
      val m = MScErrorDia(
        messageCode = ErrorMsgs.XHR_UNEXPECTED_RESP,
        potRO       = Some( ctx.modelRW.zoom(lens.get) ),
        retryAction = Some( ctx.m.reason ),
      )
      SetErrorState(m)
    }

    ActionResult.ModelUpdateEffect(v2, errFx)
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.AdsTile
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    val gridResp = ra.ads.get
    val g0 = ctx.value0.grid

    // Нужен ли тут Some(), если в isMyReqReason() запрещает иные варианты?
    val loadAdsAction = Some( ctx.m.reason ).collect {
      case gla: GridLoadAds => gla
    }

    val isSilentOpt = loadAdsAction.flatMap(_.silent)
    val qs = ctx.m.qs

    val isGridPatching = loadAdsAction.exists(_.onlyMatching.nonEmpty)

    // Если происходит частичный патчинг выдачи, то это не clean-заливка:
    val isCleanLoad = !isGridPatching &&
      qs.search.offset.fold(true)(_ ==* 0)

    // Если silent, то надо попытаться повторно пере-использовать уже имеющиеся карточки.
    val reusableAdsMap: Map[String, MScAdData] = {
      if (
        // When grid patching or silent update, or totally clean load, it is allowed to re-use ads. Possibly, this ugly conditions need rethink.
        (
          isCleanLoad ||
          (isSilentOpt contains[Boolean] true)
        ) &&
        gridResp.ads.nonEmpty &&
        g0.core.ads.adsTreePot.nonEmpty
      ) {
        // Есть условия для сборки карты текущих карточек:
        (for {
          adsTree   <- g0.core.ads.adsTreePot.iterator
          scAd      <- adsTree.flatten.iterator
          jdData    <- scAd.data.iterator
          nodeId    <- jdData.doc.tagId.nodeId.iterator
        } yield {
          nodeId -> scAd
        })
          .to( Map )
      } else {
        // Сборка карты текущих карточек не требуется в данной ситуации.
        Map.empty
      }
    }

    // Счётчик ключей элементов плитки.
    var idCounterNext = g0.core.ads.idCounter

    // Подготовить полученные с сервера карточки:
    val newScAds = (for {
      sc3AdData <- gridResp.ads.iterator
    } yield {
      // Если есть id и карта переиспользуемых карточек не пуста, то поискать там текущую карточку:
      val scAdData: MScAdData = (for {
        nodeId <- sc3AdData.jd.doc.tagId.nodeId
        scAd0  <- reusableAdsMap.get( nodeId )
        if scAd0.data.nonEmpty
      } yield {
        // При focused index ad open, возможна ситуация с focused.pending. Нужно сбросить pending:
        if (scAd0.data.isPending)
          MScAdData.data.modify( _.unPending )(scAd0)
        else
          scAd0
      })
        // Если карточка не найдена среди reusable-карточек, то перейки к сброке состояния новой карточки:
        .getOrElse {
          // Собрать начальное состояние карточки.
          // Сервер может присылать уже открытые карточи - это нормально.
          // Главное - их сразу пропихивать и в focused, и в обычные блоки.
          val jdJs = MJdDataJs.fromJdData( sc3AdData.jd, sc3AdData.info )
          val jdDoc0 = sc3AdData.jd.doc

          MScAdData(
            data = Pot.empty.ready( jdJs ),
            gridItems = {
              (for {
                itemSubTree <- JdUtil.mkTreeIndexed( jdDoc0 ).gridItemsIter
                (jdTagId, _) = itemSubTree.rootLabel
              } yield {
                val gridKey = idCounterNext
                idCounterNext += 1
                MGridItem(
                  gridKey,
                  jdDoc = jdDoc0.copy(
                    template = itemSubTree.map(_._2),
                    tagId    = jdTagId,
                  ),
                )
              })
                .toSeq
            },
            partialItems = false,
          )
        }

      // Ленивость не имеет смысла, т.к. список будет многократно использоваться ниже по тексту.
      Tree.Leaf( scAdData )
    })
      .to( List )

    // Самоконтроль для отладки: Проверить, совпадает ли SzMult между сервером и клиентом?
    //if (gridResp.szMult !=* g0.core.jdConf.szMult)
    //  LOG.warn(WarnMsgs.SERVER_CLIENT_SZ_MULT_MISMATCH, msg = (gridResp.szMult, g0.core.jdConf.szMult))

    // Новые (добавляемые) карточки по id.
    lazy val newScAdsById = newScAds
      .iterator
      .map(_.rootLabel)
      .zipWithIdIter[String]
      .to( Map )

    // Собрать обновлённый Pot с карточками.
    val gridAds0 = g0.core.ads
    val onlyMatchingInfoOpt = loadAdsAction
      .flatMap(_.onlyMatching)

    val newScAdPtrsEph = newScAds.toEphemeralStream

    val gridAds2 = (for {
      adsPtrs0 <- gridAds0.adsTreePot.toOption
      if !isCleanLoad && !adsPtrs0.subForest.isEmpty
    } yield {
      val adsPtrs2 = onlyMatchingInfoOpt.fold {
        // Без матчинга, без патчинга. Просто докидываем новые карточки в конец списка:
        Tree.Node(
          root = adsPtrs0.rootLabel,
          forest = adsPtrs0.subForest ++ newScAdPtrsEph
        )

      } { onlyMatchingInfo =>
        // Если активен матчинг, то надо удалить только обновляемые карточки, которые подпадают под матчинг.
        // В рамках текущей реализации, при пропатчивании выдачи, карточки заменяются (добавляются) только в начало.
        Tree.Node(
          root = adsPtrs0.rootLabel,
          forest = {
            val adsPtrs1 = adsPtrs0
              .subForest
              .iterator
              .filter { scAdDataSubtree =>
                val scAdData = scAdDataSubtree.rootLabel
                (for {
                  scAd <- scAdData.data.toOption
                } yield {
                  val isDelete = scAd.info.isMad404 || {
                    // Проверить данные карточки, чтобы решить, надо ли её удалять отсюда:
                    // - если карточка содержит критерии и подпадающие, и НЕподпадающие => смотрим по newScAdsById, удаляем если она там есть.
                    // - если карточка полностью подпадает под критерий отбора => удаляем
                    // - иначе - карточку оставляем в покое.
                    val scAdNodeMatches = scAd.info
                      .matchInfos
                      .flatMap(_.nodeMatchings)

                    val __isMatches = GridAh._isMatches( onlyMatchingInfo, _ )

                    // true значит - Карточка полностью удалябельна при обновлении (размещена только в ble-маячке)
                    val isDrop = scAdNodeMatches.nonEmpty && {
                      scAdNodeMatches.forall(__isMatches) || {
                        scAdNodeMatches.exists(__isMatches) &&
                          // Карточка размещена и в маячке, и как-то ещё, поэтому удалять её можно только
                          // с целью дедубликации между старыми карточками и добавляемым набором выдачи.
                          scAd.doc.tagId.nodeId
                            .fold(false)( newScAdsById.contains )
                      }
                    }
                    // If some Ad is dropped & re-added, first check reusableAds Map.
                    isDrop
                  }

                  !isDelete
                })
                  .getOrElse {
                    logger.error( ErrorMsgs.NODE_NOT_FOUND, msg = (scAdDataSubtree.rootLabel, g0.core.ads.adsTreePot.iterator.flatMap(_.flatten.iterator).flatMap(_.nodeId).mkString("[", ", ", "]")) )
                    false
                  }
              }
              .toList

            newScAdPtrsEph ++ adsPtrs1.toEphemeralStream
          }
        )
      }

      // Собрать обновлённый gridAds.
      gridAds0.copy(
        idCounter  = idCounterNext,
        adsTreePot = gridAds0.adsTreePot.ready( adsPtrs2 ),
      )
    })
      .getOrElse {
        // Чистый запуск. Сохранить только полученные карточки, забыл о предыдущих карточках.
        gridAds0.copy(
          adsTreePot = gridAds0.adsTreePot.ready(
            Tree.Node(
              gridAds0
                .adsTreePot
                .fold( MScAdData.empty )( _.rootLabel ),
              newScAdPtrsEph,
            )
          ),
          idCounter = idCounterNext,
        )
      }

    val jdRuntime2 = GridAh.mkJdRuntime(gridAds2, g0.core)
    val g2 = g0.copy(
      core = {
        var gbRes = GridAh.rebuildGrid( gridAds2, g0.core.jdConf, jdRuntime2 )

        if (isGridPatching) {
          gbRes = MGridBuildResult.nextRender
            .composeLens( MGridRenderInfo.animFromBottom )
            .set( true )(gbRes)
        }

        g0.core.copy(
          jdRuntime   = jdRuntime2,
          ads         = gridAds2,
          // Отребилдить плитку:
          gridBuild   = gbRes,
        )
      },
      hasMoreAds = {
        if (loadAdsAction.flatMap(_.onlyMatching).isEmpty) {
          qs.search.limit.fold(true) { limit =>
            gridResp.ads.lengthCompare(limit) >= 0
          }
        } else g0.hasMoreAds
      },
      afterUpdate = List.empty,
      gNotify = {
        // Если это patch-запрос карточек выдачи, то надо закинуть новые id карточек в seen
        (for {
          _ <- onlyMatchingInfoOpt
          if newScAdsById.nonEmpty
          addIds = newScAdsById.keySet
          // Подавляем пересборку seen-множества, если оно в итоге не изменится.
          if !(addIds subsetOf g0.gNotify.seenAdIds)
        } yield {
          MGridNotifyS.seenAdIds.modify(_ ++ addIds)( g0.gNotify )
        })
          .getOrElse( g0.gNotify )
      }
    )

    // Акк эффектов. В конце - afterUpdate-экшены. Тут List. LazyList при таком использовании переклинивает.
    var fxAcc = g0.afterUpdate

    // Если есть эффекты в самом исходном экшене, то отработать:
    for (reason <- loadAdsAction; fx <- reason.afterLoadFx)
      fxAcc ::= fx

    // Если patch-запрос выдачи, пришли новые карточки и всё такое, то можно отрендерить системный нотификейшен.
    if (
      // есть match-плитки.
      onlyMatchingInfoOpt.nonEmpty &&
      // Есть хотя бы одна не-404 карточка?
      newScAds.exists { scAdData =>
        !scAdData
          .rootLabel
          .data
          .exists(_.info.isMad404)
      } &&
      // Системно разрешён рендер нотификации в текущем состоянии выдачи.
      isDoOsNotify.value
    ) {
      // Нужны ОС-нотификейшены по возможным новым карточкам. Вычислить, какие новые карточки ещё не показывались в уведомлениях.
      val unNotifiedAdIds = newScAdsById -- g0.gNotify.seenAdIds
      // Собрать карточки по которым не было уведомлений (хотя бы одна карточка тут будет):
      val unNotifiedAds = (for {
        scAdTree      <- newScAds.iterator
        scAd = scAdTree.rootLabel
        adNodeId  <- scAd.nodeId
        if unNotifiedAdIds contains adNodeId
      } yield {
        scAd
      })
        .to( LazyList )

      for {
        notifyFx <- scNotifications.adsNearbyFound( unNotifiedAds )
      }
        fxAcc ::= notifyFx
    }

    // Опциональный эффект скролла вверх.
    if (isGridPatching) {
      for (fx <- GridAh.repairScrollPosFx( g0, g2 ))
        fxAcc ::= fx
    } else {
      // Возможно, требование скролла задано принудительно в исходном запросе перезагрузки плитки?
      val isScrollUp = isSilentOpt.fold(isCleanLoad)(!_)
      // А если вручную не задано, то определить нужность скроллинга автоматически:
      if (isScrollUp) {
        fxAcc ::= Effect.action {
          AnimateScroll.scrollToTop( GridScrollUtil.scrollOptions(isSmooth = true) )
          DoNothing
        }
      }
    }

    // И вернуть новый акк:
    val v2 = (MScRoot.grid set g2)(ctx.value0)
    ActionResult(Some(v2), fxAcc.mergeEffects)
  }

}
