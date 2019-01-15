package io.suggest.model.n2.node

import javax.inject.Inject
import io.suggest.es.model.Sn4EsModelCache
import io.suggest.event.SioNotifier.Event
import io.suggest.model.n2.node.event.{MNodeDeleted, MNodeSaved}
import play.api.cache.AsyncCacheApi


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.04.14 9:51
 */

/** Удалялка из кэша инстанса MNode при событии измений ноды. */
class MNodesCacheListener @Inject()(
                                     override val model  : MNodes,
                                     override val cache  : AsyncCacheApi,
                                   )
  extends Sn4EsModelCache
{

  /** Карта событий adnNode для статического подписывания в SioNotifier. */
  override def snMap = {
    val subs = Seq(this)
    List(
      MNodeSaved.getClassifier()       -> subs,
      MNodeDeleted.getClassifier()     -> subs
    )
  }

  /** Извлекаем adnId из события. */
  override def event2id(event: Event): String = {
    // Все подписанные события реализуют интерфейс IAdnId. Но всё же надо перестраховаться.
    event match {
      case e: INodeId =>
        e.nodeId

      case _ => null
    }
  }

}
