package io.suggest.ads.c

import diode._
import io.suggest.ads.{LkAdsFormConst, MLkAdsConf}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.ads.a.ILkAdsApi
import io.suggest.ads.m.{GetMoreAds, GetMoreAdsResp, MAdProps, MAdsS}
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.log.Log

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:44
  * Description: Контроллер карточек.
  */
class NodeAdsAh[M](
                    api           : ILkAdsApi,
                    jdCssFactory  : JdCssFactory,
                    confRO        : ModelRO[MLkAdsConf],
                    modelRW       : ModelRW[M, MAdsS]
                  )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

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
