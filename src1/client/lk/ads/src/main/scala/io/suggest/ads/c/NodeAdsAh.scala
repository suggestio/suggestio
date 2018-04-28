package io.suggest.ads.c

import diode._
import diode.data.Pot
import io.suggest.ads.{LkAdsFormConst, MLkAdsConf}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.ads.a.ILkAdsApi
import io.suggest.ads.m._
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.react.ReactDiodeUtil.PotOpsExt
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
                    jdCssFactory  : JdCssFactory,
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
        val fx = Effect.action( GetMoreAds(clean = false) )
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
      val v2 = v0.withAds(
        ads = for (ads <- v0.ads) yield {
          for (adProps0 <- ads) yield {
            if (adProps0.adResp.jdAdData.nodeId contains m.adId) {
              adProps0.withShownAtParentReq(
                adProps0.shownAtParentReq.pending(ts)
              )
            } else {
              adProps0
            }
          }
        }
      )

      updated(v2, fx)


    // Завершение запроса к серверу за апдейтом галочки размещения на родительском узле.
    case m: SetAdShownAtParentResp =>
      val v0 = value
      val v2 = v0.withAds(
        ads = for (ads <- v0.ads) yield {
          for (adProps0 <- ads) yield {
            if (adProps0.adResp.jdAdData.nodeId contains m.reason.adId) {
              if (adProps0.shownAtParentReq isPendingWithStartTime m.timestampMs) {
                m.tryResp.fold(
                  {ex =>
                    adProps0.withShownAtParentReq(
                      adProps0.shownAtParentReq.fail(ex)
                    )
                  },
                  {mLknNode2 =>
                    adProps0.copy(
                      adResp            = adProps0.adResp.withShownAtParent( mLknNode2.hasAdv.getOrElse(m.reason.isShown) ),
                      shownAtParentReq  = Pot.empty
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
      )
      updated(v2)


    // Команда к скачке карточек с сервера.
    case m: GetMoreAds =>
      println(m)
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
        val v2 = v0.withAds(
          v0.ads.pending()
        )
        updated(v2, fx)
      }


    // Закончен запрос к серверу за очередной порцией карточек.
    case m: GetMoreAdsResp =>
      val v0 = value

      // Залить ответ сервера в состояние:
      val v2 = m.tryResp.fold(
        {ex =>
          v0.withAds(
            v0.ads.fail(ex)
          )
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
            jdCss       = jdCssFactory.mkJdCss(
              MJdCssArgs(
                templates = ads2.map(_.adResp.jdAdData.template),
                conf      = confRO.value.jdConf
              )
            )
          )
        }
      )
      updated(v2)

  }

}
