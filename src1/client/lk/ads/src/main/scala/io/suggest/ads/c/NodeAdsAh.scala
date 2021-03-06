package io.suggest.ads.c

import diode._
import diode.data.Pot
import io.suggest.ads.{LkAdsFormConst, MLkAdsConf, MLkAdsOneAdResp}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.ads.m._
import io.suggest.common.empty.OptionUtil
import io.suggest.jd.render.m.GridRebuild
import io.suggest.jd.render.u.JdUtil
import io.suggest.lk.api.ILkAdsApi
import io.suggest.lk.nodes.{MLknModifyQs, MLknOpKeys, MLknOpValue}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.spa.DiodeUtil.Implicits.PotOpsExt
import io.suggest.sjs.common.vsz.ViewportSz

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:44
  * Description: Контроллер карточек.
  */
class NodeAdsAh[M](
                    api           : ILkAdsApi,
                    lkNodesApi    : ILkNodesApi,
                    confRO        : ModelRO[MLkAdsConf],
                    modelRW       : ModelRW[M, MAdsS]
                  )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Реакция на скроллинг страницы со списком карточек: подгрузить ещё карточек...
    case m: AdsScroll =>
      val v0 = value
      if (
        v0.hasMoreAds &&
        !v0.ads.isPending && {
          // Оценить уровень скролла: пора подгружать или нет. Тут по аналогии с GridAdsAh.
          val blocksCount = v0.ads.fold(0)(_.length) + 1
          val rowsCount = blocksCount % LkAdsFormConst.ADS_PER_ROW
          val gridHeight = rowsCount * LkAdsFormConst.ONE_ITEM_FULL_HEIGHT_PX
          val screenHeight = ViewportSz.getViewportSize().fold(768)(_.height)
          val fullContentHeight = gridHeight + LkAdsFormConst.GRID_TOP_SCREEN_OFFSET_PX
          val delta = fullContentHeight - screenHeight - m.scrollTop
          delta < LkAdsFormConst.LOAD_MORE_SCROLL_DELTA_PX
        }
      ) {
        // Надо подгрузить ещё карточек.
        val fx = GetMoreAds(clean = false).toEffectPure
        effectOnly(fx)
      } else {
        // Скролл отфильтрован.
        noChange
      }


    // Экшен замены значения галочки размещения какой-то карточки на родительском узле.
    case m: SetAdShownAtParent =>
      val ts = System.currentTimeMillis()

      val fx = Effect {
        lkNodesApi
          .modifyNode(
            MLknModifyQs(
              onNodeRk  = confRO.value.nodeKey,
              adIdOpt   = Some( m.adId ),
              opKey     = MLknOpKeys.AdvEnabled,
              opValue = MLknOpValue(
                bool = OptionUtil.SomeBool( m.isShown ),
              ),
            )
          )
          .transform { tryResp =>
            Success( SetAdShownAtParentResp(m, tryResp, ts) )
          }
      }

      val v0 = value
      val v2 = MAdsS.ads.modify( adsPot0 =>
        for (ads <- adsPot0) yield {
          for (adProps0 <- ads) yield {
            if (adProps0.adResp.jdAdData.doc.tagId.nodeId contains[String] m.adId) {
              MAdProps.shownAtParentReq
                .modify(_.pending(ts))(adProps0)
            } else {
              adProps0
            }
          }
        }
      )(v0)

      updated(v2, fx)


    // Завершение запроса к серверу за апдейтом галочки размещения на родительском узле.
    case m: SetAdShownAtParentResp =>
      val v2 = MAdsS.ads.modify( adsPot0 =>
        for (ads <- adsPot0) yield {
          for (adProps0 <- ads) yield {
            if (adProps0.adResp.jdAdData.doc.tagId.nodeId contains[String] m.reason.adId) {
              if (adProps0.shownAtParentReq isPendingWithStartTime m.timestampMs) {
                m.tryResp.fold(
                  {ex =>
                    MAdProps.shownAtParentReq
                      .modify(_.fail(ex))(adProps0)
                  },
                  {mLknNode2 =>
                    adProps0.copy(
                      adResp = MLkAdsOneAdResp.shownAtParent.replace(
                        mLknNode2.options
                          .get( MLknOpKeys.AdvEnabled )
                          .flatMap( _.bool )
                          .getOrElse( m.reason.isShown )
                      )( adProps0.adResp ),
                      shownAtParentReq = Pot.empty
                    )
                  }
                )
              } else {
                // Неактуальный реквест.
                logger.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
                adProps0
              }
            } else {
              adProps0
            }
          }
        }
      )(value)

      updated(v2)


    // Команда к скачке карточек с сервера.
    case m: GetMoreAds =>
      val v0 = value
      if (!v0.hasMoreAds) {
        // Больше нет карточек на сервере
        logger.log( ErrorMsgs.REFUSED_TO_UPDATE_EMPTY_POT_VALUE, msg = m )
        noChange

      } else if (v0.ads.isPending) {
        // Запрос к серверу уже запущен.
        logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = m )
        noChange

      } else {
        val fx = Effect {
          // Запуск запроса к серверу:
          api.getAds(
            nodeKey = confRO.value.nodeKey,
            offset  = if (m.clean) 0 else v0.ads.iterator.flatten.size
          )
            .transform { tryResp =>
              val action = GetMoreAdsResp(
                reason  = m,
                tryResp = tryResp
              )
              Success(action)
            }
        }

        // Выставить pending в состояние:
        val v2 = MAdsS.ads.modify(_.pending())(v0)
        updated(v2, fx)
      }


    // Закончен запрос к серверу за очередной порцией карточек.
    case m: GetMoreAdsResp =>
      val v0 = value

      // Залить ответ сервера в состояние:
      val v2 = m.tryResp.fold(
        {ex =>
          MAdsS.ads
            .modify(_.fail(ex))(v0)
        },
        {respAds =>
          val adsPropsResp = respAds
            .iterator
            .map(a => MAdProps(adResp = a) )
            .toVector

          val ads2 = v0.ads
            .filter(_ => !m.reason.clean)
            .fold(adsPropsResp)(_ ++ adsPropsResp)

          v0.copy(
            ads         = v0.ads.ready(ads2),
            hasMoreAds  = respAds.lengthCompare(LkAdsFormConst.GET_ADS_COUNT_PER_REQUEST) >= 0,
            jdRuntime   = JdUtil
              .prepareJdRuntime(confRO.value.jdConf)
              .docs(
                ads2
                  .iterator
                  .map(_.adResp.jdAdData.doc)
                  .to( LazyList )
              )
              .prev( v0.jdRuntime )
              .make,
          )
        }
      )
      updated(v2)


    // No grid here (jdAh notifies about changes in qdBlockless layout).
    case _: GridRebuild =>
      noChange

  }

}
