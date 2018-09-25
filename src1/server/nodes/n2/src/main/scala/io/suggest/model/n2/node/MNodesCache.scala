package io.suggest.model.n2.node

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import io.suggest.es.model.{EsModelCache, Sn4EsModelCache}
import io.suggest.event.SioNotifier.Event
import io.suggest.model.n2.node.event.{MNodeDeleted, MNodeSaved}
import play.api.cache.AsyncCacheApi

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.04.14 9:51
 * Description: Кеширующий прокси для модели MAdnNode. По сути содержит только getById() и функционал
 * для оперативного опустошения кеша.
 * getByIdCached() не следует активно использовать в личном кабинете, т.к. она не гарантирует реалтайма.
 */
@Singleton
class MNodesCache @Inject()(
                             // Тут нельзя инжектить MCommonDi, т.к. будет circular dep.
                             mNodes                          : MNodes,
                             override val cache              : AsyncCacheApi,
                             override implicit val ec        : ExecutionContext,
                             override implicit val mat       : Materializer,
                           )
  extends EsModelCache[MNode]
  with Sn4EsModelCache
{

  override val EXPIRE = 60.seconds

  override val CACHE_KEY_SUFFIX = ".nc"

  /** Карта событий adnNode для статического подписывания в SioNotifier. */
  override def snMap = {
    val subs = Seq(this)
    List(
      MNodeSaved.getClassifier()       -> subs,
      MNodeDeleted.getClassifier()     -> subs
    )
  }

  override def companion = mNodes

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


/** Интерфейс для DI-поля с моделью кеша для [[MNodes]]. */
trait INodeCache {

  val mNodesCache: MNodesCache

}
