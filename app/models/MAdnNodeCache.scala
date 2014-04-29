package models

import play.api.Play.current
import io.suggest.event._
import io.suggest.event.SioNotifier.{Event, Subscriber, Classifier}
import scala.reflect._
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
object MAdnNodeCache extends AdnEsModelCache {

  override type T = MAdnNode
  override implicit val TTag: ClassTag[T] = classTag[MAdnNode]

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


  override protected def getById(id: String) = MAdnNode.getById(id)

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
