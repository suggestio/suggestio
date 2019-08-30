package io.suggest.ads.c

import diode._
import diode.data.Pot
import io.suggest.ads.{LkAdsFormConst, MLkAdsConf, MLkAdsOneAdResp}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.ads.a.ILkAdsApi
import io.suggest.ads.m._
import io.suggest.jd.render.m.MJdRuntime
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.log.Log
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
        lkNodesApi.setAdv(
          adId      = m.adId,
          isEnabled = m.isShown,
          onNode    = confRO.value.nodeKey
        ).transform { tryResp =>
          Success( SetAdShownAtParentResp(m, tryResp, ts) )
        }
      }

      val v0 = value
      val v2 = MAdsS.ads.modify( adsPot0 =>
        for (ads <- adsPot0) yield {
          for (adProps0 <- ads) yield {
            if (adProps0.adResp.jdAdData.nodeId contains m.adId) {
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
            if (adProps0.adResp.jdAdData.nodeId contains m.reason.adId) {
              if (adProps0.shownAtParentReq isPendingWithStartTime m.timestampMs) {
                m.tryResp.fold(
                  {ex =>
                    MAdProps.shownAtParentReq
                      .modify(_.fail(ex))(adProps0)
                  },
                  {mLknNode2 =>
                    adProps0.copy(
                      adResp = MLkAdsOneAdResp.shownAtParent
                        .set( mLknNode2.hasAdv getOrElse m.reason.isShown )( adProps0.adResp ),
                      shownAtParentReq = Pot.empty
                    )
                  }
                )
              } else {
                // Неактуальный реквест.
                LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
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
        LOG.log( WarnMsgs.REFUSED_TO_UPDATE_EMPTY_POT_VALUE, msg = m )
        noChange

      } else if (v0.ads.isPending) {
        // Запрос к серверу уже запущен.
        LOG.log( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = m )
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
          val adsPropsResp = respAds.iterator
            .map(a => MAdProps(adResp = a) )
            .toVector

          val ads2 = v0.ads
            .filter(_ => !m.reason.clean)
            .fold(adsPropsResp)(_ ++ adsPropsResp)

          v0.copy(
            ads         = v0.ads.ready(ads2),
            hasMoreAds  = respAds.lengthCompare(LkAdsFormConst.GET_ADS_COUNT_PER_REQUEST) >= 0,
            jdRuntime   = MJdRuntime.make(
              tpls    = ads2.map(_.adResp.jdAdData.template),
              jdConf  = confRO.value.jdConf,
            ),
          )
        }
      )
      updated(v2)

  }

}
