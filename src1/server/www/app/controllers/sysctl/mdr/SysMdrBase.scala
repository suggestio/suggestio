package controllers.sysctl.mdr

import controllers.SioController
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.node.MNode
import io.suggest.primo.id.OptId
import io.suggest.sys.mdr.MdrSearchArgs
import io.suggest.util.logs.IMacroLogs
import models.msys.MSysMdrFreeAdvsTplArgs
import models.req.IReq
import play.api.mvc.Result
import util.lk.ILkAdUtilDi
import util.mdr.ISysMdrUtilDi
import util.n2u.IN2NodesUtilDi
import views.html.sys1.mdr._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 11:52
  * Description: Контроллер [[controllers.SysMdr]] пришлось разбить на куски, чтобы разгрузить код.
  */

trait SysMdrBase
  extends SioController
  with IMacroLogs
  with IN2NodesUtilDi
  with ILkAdUtilDi
  with ISysMdrUtilDi
{

  import mCommonDi._


  /** Общий код отработки запроса страницы с карточками, нуждающимися в модерации, вынесен сюда. */
  protected[this] def _adsPage(madsFut0: Future[Seq[MNode]], args: MdrSearchArgs)
                              (implicit request: IReq[_]): Future[Result] = {
    val madsFut = args.hideAdIdOpt.fold(madsFut0) { hai =>
      for (mads0 <- madsFut0) yield {
        mads0.filter(_.id.get != hai)
      }
    }

    val prodsMapFut = for {
      mads  <- madsFut
      prods <- {
        // Сгребаем всех продьюсеров карточек + добавляем запрошенных продьюсеров, дедублицируем список.
        val prodIds = mads
          .iterator
          .flatMap { n2NodesUtil.madProducerId }
          .++( args.producerId )
          .toSet
        mNodesCache.multiGet( prodIds )
      }
    } yield {
      OptId.els2idMap[String, MNode](prods)
    }

    val brArgssFut = madsFut.flatMap { mads =>
      Future.traverse(mads) { mad =>
        lkAdUtil.tiledAdBrArgs(mad)
      }
    }

    val prodOptFut = FutureUtil.optFut2futOpt( args.producerId ) { prodId =>
      for (prodsMap <- prodsMapFut) yield {
        prodsMap.get(prodId)
      }
    }

    for {
      brArgss   <- brArgssFut
      prodsMap  <- prodsMapFut
      mnodeOpt  <- prodOptFut
    } yield {
      val rargs = MSysMdrFreeAdvsTplArgs(
        args0         = args,
        mads          = brArgss,
        prodsMap      = prodsMap,
        producerOpt   = mnodeOpt
      )
      Ok( freeAdvsTpl(rargs) )
    }
  }

}
