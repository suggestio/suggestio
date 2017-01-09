package models

import com.google.inject.{Inject, Singleton}
import io.suggest.model.n2.node.{INodeId, MNodes}
import io.suggest.model.n2.node.event.{MNodeDeleted, MNodeSaved}
import org.elasticsearch.client.Client
import play.api.Configuration
import io.suggest.event.SioNotifier.Event
import play.api.cache.CacheApi

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.04.14 9:51
 * Description: Кеширующий прокси для модели MAdnNode. По сути содержит только getById() и функционал
 * для оперативного опустошения кеша.
 * getByIdCached() не следует активно использовать в личном кабинете, т.к. она не гарантирует реалтайма.
 */
@Singleton
class MNodeCache @Inject() (
  // Тут нельзя инжектить MCommonDi, т.к. будет circular dep.
  configuration                   : Configuration,
  mNodes                          : MNodes,
  override val cache              : CacheApi,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client
)
  extends EsModelCache[MNode]
{

  override val EXPIRE: FiniteDuration = {
    configuration.getInt("adn.node.cache.expire.seconds")
      .getOrElse(60)
      .seconds
  }

  override val CACHE_KEY_SUFFIX = ".nc"

  override type GetAs_t = MNode

  /** Карта событий adnNode для статического подписывания в SioNotifier. */
  override def snMap = {
    val subs = Seq(this)
    List(
      MNodeSaved.getClassifier()       -> subs,
      MNodeDeleted.getClassifier()     -> subs
    )
  }

  override type StaticModel_t = MNodes
  override def companion: StaticModel_t = mNodes

  /** Извлекаем adnId из события. */
  override def event2id(event: Event): String = {
    // Все подписанные события реализуют интерфейс IAdnId. Но всё же надо перестраховаться.
    event match {
      case e: INodeId =>
        e.nodeId

      case _ => null
    }
  }

  def getByIdType(nodeId: String, ntype: MNodeType): Future[Option[MNode]] = {
    for (mnodeOpt <- getById(nodeId)) yield {
      mnodeOpt.filter { mnode =>
        mnode.common.ntype == ntype
      }
    }
  }

}
