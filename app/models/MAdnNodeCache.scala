package models

import com.google.inject.Inject
import play.api.Configuration
import io.suggest.event._
import io.suggest.event.SioNotifier.Event
import play.api.cache.CacheApi

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.04.14 9:51
 * Description: Кеширующий прокси для модели MAdnNode. По сути содержит только getById() и функционал
 * для оперативного опустошения кеша.
 * getByIdCached() не следует активно использовать в личном кабинете, т.к. она не гарантирует реалтайма.
 */
class MAdnNodeCache @Inject() (
  configuration         : Configuration,
  override val cache    : CacheApi
)
  extends AdnEsModelCache[MAdnNode]
{

  override val EXPIRE: FiniteDuration = {
    configuration.getInt("adn.node.cache.expire.seconds")
      .getOrElse(60)
      .seconds
  }

  override val CACHE_KEY_SUFFIX = ".nc"

  override type GetAs_t = MAdnNode

  /** Карта событий adnNode для статического подписывания в SioNotifier. */
  override def snMap = {
    val subs = Seq(this)
    List(
      AdnNodeSavedEvent.getClassifier()   -> subs,
      AdnNodeDeletedEvent.getClassifier() -> subs,
      AdnNodeOnOffEvent.getClassifier()   -> subs
    )
  }

  override type StaticModel_t = MAdnNode.type
  override def companion: StaticModel_t = MAdnNode

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
