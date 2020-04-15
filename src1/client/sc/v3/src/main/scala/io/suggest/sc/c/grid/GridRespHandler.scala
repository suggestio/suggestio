package io.suggest.sc.c.grid

import com.github.fisshy.react.scroll.AnimateScroll
import diode.{ActionResult, Effect, ModelRO}
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants._
import io.suggest.dev.MOsFamilies
import io.suggest.geo.DistanceUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.grid.GridScrollUtil
import io.suggest.grid.build.{MGridBuildResult, MGridRenderInfo}
import io.suggest.i18n.MsgCodes
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.MNodeTypes
import io.suggest.os.notify.{MOsToast, MOsToastText, ShowNotify}
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.{MScRoot, SetErrorState}
import io.suggest.sc.m.grid.{GridLoadAds, MGridCoreS, MGridNotifyS, MGridS, MScAdData}
import io.suggest.sc.sc3.{MSc3RespAction, MScRespActionType, MScRespActionTypes}
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DoNothing
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.text.StringUtil
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.03.2020 16:44
  * Description:
  */

/** Поддержка resp-handler'а для карточек плитки без фокусировки. */
class GridRespHandler(
                       isDoOsNotify: ModelRO[Boolean],
                     )
  extends IRespWithActionHandler
  with Log
{

  /** Когда происходит рендер нотификации, надо укорачивать заголовок каждой карточки до указанной величины.
    * Это базовая величина, с которой идёт рассчёт длины заголовка для каждой конкретной карточки в зависимости от обстоятельств рендера.
    */
  def NOTIFY_AD_TITLE_MAX_LEN = 40

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[GridLoadAds]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( ctx.value0.grid.core.ads )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): ActionResult[MScRoot] = {
    val lens = MScRoot.grid
      .composeLens(MGridS.core)
      .composeLens( MGridCoreS.ads )

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

    val mGla = Some( ctx.m.reason ).collect {
      case gla: GridLoadAds => gla
    }

    val isSilentOpt = mGla.flatMap(_.silent)
    val qs = ctx.m.qs

    val isGridPatching = mGla.exists(_.onlyMatching.nonEmpty)

    val isCleanLoad =
      qs.search.offset.fold(true)(_ ==* 0) &&
      // Если происходит частичный патчинг выдачи, то это не clean-заливка:
      !isGridPatching

    // Если silent, то надо попытаться повторно пере-использовать уже имеющиеся карточки.
    val reusableAdsMap: Map[String, MScAdData] = {
      if (
        isCleanLoad &&
          (isSilentOpt contains true) &&
          gridResp.ads.nonEmpty &&
          g0.core.ads.nonEmpty
      ) {
        // Есть условия для сборки карты текущих карточек:
        g0.core.ads
          .iterator
          .flatten
          .zipWithIdIter[String]
          .to( Map )
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
        sc3AdData.jd.doc.tagId.nodeId
          .flatMap( reusableAdsMap.get )
          // Если карточка не найдена среди reusable-карточек, то перейки к сброке состояния новой карточки:
          .getOrElse {
            // Собрать начальное состояние карточки.
            // Сервер может присылать уже открытые карточи - это нормально.
            // Главное - их сразу пропихивать и в focused, и в обычные блоки.
            MScAdData(
              main = MJdDataJs.fromJdData( sc3AdData.jd, sc3AdData.info ),
            )
          }
      }
      .to( Vector )

    // Самоконтроль для отладки: Проверить, совпадает ли SzMult между сервером и клиентом?
    //if (gridResp.szMult !=* g0.core.jdConf.szMult)
    //  LOG.warn(WarnMsgs.SERVER_CLIENT_SZ_MULT_MISMATCH, msg = (gridResp.szMult, g0.core.jdConf.szMult))

    // Новые (добавляемые) карточки по id.
    lazy val newScAdsById = newScAds
      .zipWithIdIter[String]
      .to( Map )

    // Собрать обновлённый Pot с карточками.
    val ads0 = g0.core.ads
    val onlyMatchingInfoOpt = mGla
      .flatMap(_.onlyMatching)
    val ads2 = ads0.ready {
      (for {
        ads0 <- ads0.toOption
        if !isCleanLoad && ads0.nonEmpty
        // Если активен матчинг, то надо удалить только обновляемые карточки, которые подпадают под матчинг.
        // В рамках текущей реализации, при пропатчивании выдачи, карточки заменяются (добавляются) только в начало.
        ads9 = onlyMatchingInfoOpt.fold( ads0 ++ newScAds ) { onlyMatchingInfo =>
          newScAds ++ ads0.filterNot { scAd =>
            // Проверить данные карточки, чтобы решить, надо ли её удалять отсюда:
            // - если карточка содержит критерии и подпадающие, и НЕподпадающие => смотрим по newScAdsById, удаляем если она там есть.
            // - если карточка полностью подпадает под критерий отбора => удаляем
            // - иначе - карточку оставляем в покое.
            val scAdNodeMatches = scAd.main.info
              .matchInfos
              .flatMap(_.nodeMatchings)

            val __isMatches = GridAh._isMatches(onlyMatchingInfo, _)

            // true значит - Карточка полностью удалябельна при обновлении (размещена только в ble-маячке)
            scAdNodeMatches.forall(__isMatches) || {
              scAdNodeMatches.exists(__isMatches) &&
                // Карточка размещена и в маячке, и как-то ещё, поэтому удалять её можно только
                // с целью дедубликации между старыми карточками и добавляемым набором выдачи.
                scAd.nodeId.fold(false)( newScAdsById.contains )
            }
          }
        }

      } yield {
        ads9
      })
        .getOrElse( newScAds )
    }
    //println(s"+${newScAds.size} have=${g0.core.ads.fold(0)(_.size)} = ${ads2.get.size}")

    val jdRuntime2 = GridAh.mkJdRuntime(ads2, g0.core)
    val g2 = g0.copy(
      core = {
        var gbRes = GridAh.rebuildGrid( ads2, g0.core.jdConf, jdRuntime2 )

        if (isGridPatching) {
          gbRes = MGridBuildResult.nextRender
            .composeLens( MGridRenderInfo.animFromBottom )
            .set( true )(gbRes)
        }

        g0.core.copy(
          jdRuntime   = jdRuntime2,
          ads         = ads2,
          // Отребилдить плитку:
          gridBuild   = gbRes,
        )
      },
      hasMoreAds = {
        if (mGla.flatMap(_.onlyMatching).isEmpty) {
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

    // Если patch-запрос выдачи, пришли новые карточки и всё такое, то можно отрендерить системный нотификейшен.
    if (
      onlyMatchingInfoOpt.nonEmpty &&
      newScAds.nonEmpty &&
      isDoOsNotify.value
    ) {
      // Нужны ОС-нотификейшены по возможным новым карточкам. Вычислить, какие новые карточки ещё не показывались в уведомлениях.
      val unNotifiedAdIds = newScAdsById -- g0.gNotify.seenAdIds
      if (unNotifiedAdIds.nonEmpty) {
        // Есть новые карточки, по которым можно уведомить юзера.
        val showToastFx = Effect.action {
          // Собрать карточки по которым не было уведомлений (хотя бы одна карточка тут будет):
          val unNotifiedAds = (for {
            scAd <- newScAds.iterator
            adNodeId <- scAd.nodeId
            if unNotifiedAdIds contains adNodeId
          } yield {
            scAd
          })
            .to( List )

          val messages = ctx.value0.internals.info.commonReactCtx.messages

          // Надо собрать title'ы карточек, добавив расстояния до маячков.
          val adTitlesWithDistRendered = (for {
            scAd <- unNotifiedAds.iterator
            // Используем в работе только карточки с заголовком, присланным с сервера.
            adTitle <- scAd.main.title.iterator
          } yield {
            val distancesCm = (for {
              // Поискать id маячка, с помощью которого найдена карточка.
              adMatchInfo <- scAd.main.info.matchInfos.iterator
              if adMatchInfo.predicates.exists(_ eqOrHasParent MPredicates.Receiver)
              adMatchNodeInfo <- adMatchInfo.nodeMatchings.iterator
              if adMatchNodeInfo.ntype contains MNodeTypes.BleBeacon
              bleBeaconNodeId <- adMatchNodeInfo.nodeId.iterator
              // Есть id маячка. Найти в текущей инфе указанный маячок.
              uidBeacon <- ctx.value0.dev.beaconer
                .nearbyReportById
                .get( bleBeaconNodeId )
                .iterator
            } yield {
              uidBeacon.distanceCm
            })
              .to( LazyList )

            Option.when( distancesCm.nonEmpty ) {
              val distanceCm2 = if (distancesCm.lengthIs == 1) {
                distancesCm.head
              } else {
                // Найти среднее арифметическое всех известных расстояний среди имеющихся (одна и та же карточка может быть размещена в нескольких маячках):
                distancesCm.sum / distancesCm.length
              }

              // Переводим расстояние в метры, рендерим в строку:
              val distanceMeters2 = distanceCm2.toDouble / 100
              val distMsg = DistanceUtil.formatDistanceM( distanceMeters2 )

              // Рендерим в строку всё это
              val distanceStr = messages( distMsg )

              // Укоротить заголовок с сервера. 3 - оцениваем как доп."расходы" символов на рендер: скобки, пробелы и т.д. для MsgCodes.`0._inDistance.1`
              val adTitleLenMax = NOTIFY_AD_TITLE_MAX_LEN - distanceStr.length - 3
              val adTitleEllipsied = StringUtil.strLimitLen( adTitle, adTitleLenMax )
              messages( MsgCodes.`0._inDistance.1`, adTitleEllipsied, distanceStr )
            }
              .getOrElse {
                StringUtil.strLimitLen( adTitle, NOTIFY_AD_TITLE_MAX_LEN )
              }
          })
            .to( List )

          val unNotifiedAdsCount = unNotifiedAds.length

          val toast = MOsToast(
            uid = getClass.getSimpleName + `.` + MNodeTypes.BleBeacon.value,
            // Заголовок прост: вывести, что найдено сколько-то предложений рядом
            title = {
              // TODO Переехать на Mozilla Fluent, чтобы отрабатывать детали локализации на уровне каждого конкретного языка.
              if (unNotifiedAdsCount ==* 1) {
                messages( MsgCodes.`One.offer.nearby` )
              } else {
                messages( MsgCodes.`0.offers.nearby`, unNotifiedAdsCount )
              }
            },
            text = MOsToastText(
              text = Option
                .when( adTitlesWithDistRendered.nonEmpty ) {
                  adTitlesWithDistRendered
                    .mkString("\n")
                }
                .getOrElse {
                  messages(
                    MsgCodes.`Show.offers.0`,
                    ctx.value0.dev.beaconer.nearbyReport
                      .iterator
                      .map(_.distanceCm)
                      .maxOption
                      .fold("") { maxDistanceCm =>
                        // Нет текстов - вывести что-то типа "Показать предложения в радиусе 50 метров".
                        messages(
                          MsgCodes.`in.radius.of.0`,
                          messages( DistanceUtil.formatDistanceCM( maxDistanceCm ) ),
                        )
                      },
                  )
                }
            ) :: Nil,
            // Android: нужно задавать smallIcon, т.к. штатная иконка после обесцвечивания в пиктограмму плохо выглядит.
            smallIconUrl = Option.when {
              val p = ctx.value0.dev.platform
              p.isCordova && (p.osFamily contains MOsFamilies.Android)
            }( "res://ic_notification" ),
            // TODO appBadgeCounter - почему-то не работает
            appBadgeCounter = Option.when( unNotifiedAdsCount > 0 )(unNotifiedAdsCount),
            // TODO vibrate - выключить вибрацию? false или None - не помогают.
            vibrate = OptionUtil.SomeBool.someFalse,
            //silent = OptionUtil.SomeBool.someTrue,
            foreground = OptionUtil.SomeBool.someTrue,
            // TODO icon - вывести круглую иконку узла, в котором может быть находится пользователь? Взять присланную сервером или текущую какую-нибудь?
            // image - без картинки, т.к. это довольно узконаправленное решение.
            /*imageUrl = for {
                mainBlk <- scAd.main.doc.template.loc
                            .findByType( MJdTagNames.STRIP )
                bgImgEdgeId <- mainBlk.getLabel.props1.bgImg
                bgImg <- scAd.main.edges.get( bgImgEdgeId.edgeUid )
                bgImgUrl <- bgImg.jdEdge.url
              } yield {
                HttpClient.mkAbsUrl( bgImgUrl )
              },*/
          )

          ShowNotify( toast :: Nil )
        }

        fxAcc ::= showToastFx
      }
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
