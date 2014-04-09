package models

import play.api.Play.current
import io.suggest.event._
import io.suggest.event.SioNotifier.{Event, Subscriber, Classifier}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.04.14 9:51
 * Description: Кеширующий прокси для модели MAdnNode. По сути содержит только getById() и функционал
 * для оперативного опустошения кеша.
 * getByIdCached() не следует активно использовать в личном кабинете, т.к. она не гарантирует реалтайма.
 */
object MAdnNodeCache extends AdnEsModelCache[MAdnNode] {

  val EXPIRE_SEC: Int = current.configuration.getInt("adn.node.cache.expire.seconds") getOrElse 60
  val CACHE_KEY_SUFFIX = ".nc"

  type GetAs_t = MAdnNode

  /** Карта событий adnNode для статического подписывания в SioNotifier. */
  def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    val subs = Seq(this)
    Seq(
      AdnNodeSavedEvent.getClassifier()   -> subs,
      AdnNodeDeletedEvent.getClassifier() -> subs,
      AdnNodeOnOffEvent.getClassifier()   -> subs
    )
  }

  def companion = MAdnNode

  /** Извлекаем adnId из события. */
  def event2id(event: Event): String = {
    // Все подписанные события реализуют интерфейс IAdnId. Но всё же надо перестраховаться.
    event match {
      case e: IAdnId =>
        e.adnId

      case _ => null
    }
  }

}
