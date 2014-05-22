package models

import play.api.Play.current
import io.suggest.event._
import io.suggest.event.SioNotifier.{Event, Subscriber, Classifier}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.04.14 9:51
 * Description: Кеширующий прокси для модели MAdnNode. По сути содержит только getById() и функционал
 * для оперативного опустошения кеша.
 * getByIdCached() не следует активно использовать в личном кабинете, т.к. она не гарантирует реалтайма.
 */
object MAdnNodeCache extends AdnEsModelCache[MAdnNode] {

  override val EXPIRE_SEC: Int = current.configuration.getInt("adn.node.cache.expire.seconds") getOrElse 60
  override val CACHE_KEY_SUFFIX = ".nc"

  override type GetAs_t = MAdnNode

  /** Карта событий adnNode для статического подписывания в SioNotifier. */
  override def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    val subs = Seq(this)
    Seq(
      AdnNodeSavedEvent.getClassifier()   -> subs,
      AdnNodeDeletedEvent.getClassifier() -> subs,
      AdnNodeOnOffEvent.getClassifier()   -> subs
    )
  }


  override def getByIdNoCache(id: String) = MAdnNode.getById(id)

  /** Извлекаем adnId из события. */
  override def event2id(event: Event): String = {
    // Все подписанные события реализуют интерфейс IAdnId. Но всё же надо перестраховаться.
    event match {
      case e: IAdnId =>
        e.adnId

      case _ => null
    }
  }

}
