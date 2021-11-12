package util.domain

import io.suggest.adn.MAdnRights
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.EsModel
import io.suggest.n2.extra.domain.{DomainCriteria, MDomainModes}
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.text.util.UrlUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.ContextUtil
import play.api.cache.AsyncCacheApi
import play.api.inject.Injector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}


/** Util for 3rd-party domains. */
@Singleton
final class Domains3pUtil @Inject()(
                                     injector: Injector,
                                   )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val contextUtil = injector.instanceOf[ContextUtil]
  private lazy val asyncCacheApi = injector.instanceOf[AsyncCacheApi]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /** Detect 3p-domain node using http-request context.
    *
    * @param domain domain from Origin: HTTP header.
    * @return Future with optional MNode found.
    */
  def find3pDomainNode(domain: String): Future[Option[MNode]] = {
    // Note! logPrefix also used as cache key here for simplicity. Never add System.currentTimeMillis() or anything random here.
    lazy val logPrefix = s"find3pDomainNode($domain):"

    val isSioDomain = contextUtil.isSuggestioDomain( domain )

    LOGGER.trace(s"$logPrefix isSioDomain?$isSioDomain")

    OptionUtil.maybeFut( !isSioDomain ) {
      import esModel.api._

      val dkey = try {
        UrlUtil.host2dkey(domain)
      } catch {
        case ex: Throwable =>
          LOGGER.warn(s"$logPrefix Failed to normalize host '$domain' into dkey", ex)
          domain
      }

      asyncCacheApi.getOrElseUpdate( logPrefix, expiration = 5.seconds ) {
        val msearch = new MNodeSearch {
          override def domains: Seq[DomainCriteria] = {
            val cr = DomainCriteria(
              dkeys = dkey :: Nil,
              modes = MDomainModes.ScServeIncomingRequests :: Nil
            )
            cr :: Nil
          }
          override def limit          = 1
          override def isEnabled      = OptionUtil.SomeBool.someTrue
          override def withAdnRights  = MAdnRights.RECEIVER :: Nil
        }
        val fut = mNodes.dynSearchOne(msearch)

        fut.onComplete {
          case Success(None)    => LOGGER.debug(s"$logPrefix No linked nodes not found")
          case Success(Some(r)) => LOGGER.trace(s"$logPrefix Found node[${r.idOrNull}] ${r.guessDisplayNameOrIdOrEmpty}")
          case Failure(ex)      => LOGGER.warn(s"$logPrefix Unable to make nodes search request:\n $msearch", ex)
        }

        // Вернуть основной фьючерс поиска подходящего под домен узла.
        fut
      }
    }
  }

}
