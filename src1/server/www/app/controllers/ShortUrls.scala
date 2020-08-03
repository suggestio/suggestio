package controllers

import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import models.mctx.ContextUtil
import models.msc.ScJsState
import play.api.Mode
import play.api.mvc.{RequestHeader, Result}
import util.acl.MaybeAuth

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
  private def _scUrlRdrResp(beaconId: Option[String] = None)
                           (implicit request: RequestHeader): Future[Result] = {
    val call = routes.Sc.geoSite(
      ScJsState(
        generationOpt = None,
        // TODO надо id узла-маячка пробросить в выдачу.
      )
    )
    val toAbsUrl = contextUtil.SC_HOST_PORT + call.url
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

    // TODO Поискать маячки с urlCode в качестве id в ShortUrl-эдже.
    for {

      mnodeOpt <- mNodes.dynSearchOne(
        new MNodeSearch {
          override def isEnabled = Some( true )
          override def limit = 1
          override def outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              nodeIds     = urlCode :: Nil,
              predicates  = MPredicates.ShortUrl :: Nil,
            )
            MEsNestedSearch( cr :: Nil )
          }
        }
      )

      res <- mnodeOpt
        .flatMap { res =>
          // Разобраться, что тут у нас.
          res.common.ntype match {
            // Это маячок. Нужно, чтобы выдача открылась, как бы находясь в данном маячке, не имея bluetooth.
            case MNodeTypes.BleBeacon =>
              LOGGER.trace(s"$logPrefix Found beacon#${res.id}")
              Some( _scUrlRdrResp( res.id ) )

            case other =>
              LOGGER.warn(s"$logPrefix Unsupported node-type#$other for short-URL.")
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
