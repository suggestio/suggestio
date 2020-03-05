package io.suggest.sc.c.grid

import com.github.fisshy.react.scroll.AnimateScroll
import diode.{ActionResult, Effect}
import diode.data.Pot
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.empty.OptionUtil
import io.suggest.grid.GridScrollUtil
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.{MScRoot, SetErrorState}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.grid.{GridLoadAds, MGridCoreS, MGridS, MScAdData}
import io.suggest.sc.sc3.{MSc3RespAction, MScRespActionType, MScRespActionTypes}
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DoNothing
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.03.2020 16:44
  * Description:
  */

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
              main = MJdDataJs(
                doc   = sc3AdData.jd.doc,
                edges = MEdgeDataJs.jdEdges2EdgesDataMap( sc3AdData.jd.edges ),
                info  = sc3AdData.info,
              ),
            )
          }
      }
      .to( Vector )

    // Самоконтроль для отладки: Проверить, совпадает ли SzMult между сервером и клиентом?
    //if (gridResp.szMult !=* g0.core.jdConf.szMult)
    //  LOG.warn(WarnMsgs.SERVER_CLIENT_SZ_MULT_MISMATCH, msg = (gridResp.szMult, g0.core.jdConf.szMult))


    // Опциональный эффект скролла вверх.
    val scrollFxOpt = {
      // Возможно, требование скролла задано принудительно в исходном запросе перезагрузки плитки?
      val isScrollUp = isSilentOpt.fold(isCleanLoad)(!_)
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

    val jdRuntime2 = GridAh.mkJdRuntime(ads2, g0.core)
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
    val v2 = (MScRoot.grid set g2)(ctx.value0)
    ActionResult(Some(v2), scrollFxOpt)
  }

}
