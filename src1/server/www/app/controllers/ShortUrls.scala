package controllers

import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNodeIdType, MNodeType, MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.spa.SioPages
import io.suggest.util.logs.MacroLogsImplLazy

import javax.inject.Inject
import models.mctx.ContextUtil
import play.api.Mode
import play.api.mvc.{RequestHeader, Result}
import util.acl.{MaybeAuth, SioControllerApi}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.2020 10:12
  * Description: Контроллер поддержки коротких ссылок.
  * Короткие ссылки для кодирования внутрь маячков EddyStone-URL, где макс.длина ссылки - это 17 байт.
  */
final class ShortUrls @Inject() (
                                  esModel             : EsModel,
                                  mNodes              : MNodes,
                                  maybeAuth           : MaybeAuth,
                                  sioControllerApi    : SioControllerApi,
                                )
  extends MacroLogsImplLazy
{

  import sioControllerApi._
  import mCommonDi.{ec, errorHandler, current}
  import esModel.api._

  lazy val contextUtil = current.injector.instanceOf[ContextUtil]


  /** Редиректить на главную? */
  private def IS_NOT_FOUND_RDR_TO_MAIN: Boolean = mCommonDi.current.mode == Mode.Prod


  /** Рендер ответа с редиректом в выдачу. */
  private def _scUrlRdrResp(beaconId: Option[MNodeIdType] = None)
                           (implicit request: RequestHeader): Future[Result] = {
    val call = sc.routes.ScSite.geoSite(
      SioPages.Sc3(
        virtBeacons = beaconId.toList,
      )
    )
    val toAbsUrl = contextUtil.SC_URL_PREFIX + call.url
    Future.successful( Redirect(toAbsUrl, SEE_OTHER) )
  }


  /** Экшен перехвата короткой ссылки: определить адрес для редиректа на основе кода в ссылке.
    *
    * @param urlCode Код после тильды: a-zA-Z0-9 и другие URL-safe символы.
    * @return Редирект куда-либо.
    */
  def handleUrl(urlCode: String) = maybeAuth().async { implicit request =>
    // Изначально, id содержал короткий алиас для URL внутри EddyStone-маячка.
    lazy val logPrefix = s"handleUrl($urlCode):"

    for {

      // Поискать маячки с urlCode в качестве id в ShortUrl-эдже.
      mnodeOpt <- mNodes.dynSearchOne(
        new MNodeSearch {
          override def isEnabled = Some( true )
          override def limit = 1
          override def outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              nodeIds     = urlCode :: Nil,
              predicates  = MPredicates.ShortUrl :: Nil,
            )
            MEsNestedSearch.plain( cr )
          }
        }
      )

      res <- mnodeOpt
        .flatMap { res =>
          // Разобраться, что тут у нас.
          val ntype = res.common.ntype
          if (MNodeTypes.lkNodesUserCanCreate contains[MNodeType] ntype) {
            // The radio-beacon or something alike. Showcase should open "inside-like-or-nearby" beacon without bluetooth.
            LOGGER.trace(s"$logPrefix Found beacon#${res.id}")
            val nodeIdTypeOpt = res.id.map { MNodeIdType(_, ntype) }
            Some( _scUrlRdrResp( nodeIdTypeOpt ) )
          } else {
            LOGGER.warn(s"$logPrefix Unsupported node-type#$ntype for short-URL.")
            None
          }
        }
        .getOrElse {
          val rdrToMain = IS_NOT_FOUND_RDR_TO_MAIN
          LOGGER.warn(s"$logPrefix Not found short URL ID. Rdr to main?$rdrToMain")
          if (rdrToMain) {
            _scUrlRdrResp()
          } else {
            errorHandler.onClientError( request, NOT_FOUND, "URL is not registered or disabled." )
          }
        }

    } yield {
      res
    }
  }

}
