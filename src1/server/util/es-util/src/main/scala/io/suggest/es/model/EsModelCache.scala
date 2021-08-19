package io.suggest.es.model

import akka.actor.ActorContext
import io.suggest.event.SNStaticSubscriber
import io.suggest.event.SioNotifier.Event
import io.suggest.event.subscriber.SnClassSubscriber
import play.api.cache.AsyncCacheApi

import scala.concurrent.duration.FiniteDuration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 11:30
 * Description: Барахло для создания кеширующих моделей. Реализовано в виде class'ов из-за
 * необходимости передавать ClassTag.
 */
trait EsModelStaticCacheableT extends EsModelStaticT {

  def EXPIRE            : FiniteDuration
  def CACHE_KEY_SUFFIX  : String

}

object EsModelStaticCacheableT {

  implicit class OpsExt(val model: EsModelStaticCacheableT) extends AnyVal {
    /**
      * Генерация ключа кеша.
      * @param id id исходного документа.
      * @return Строка, пригодная для использования в качестве ключа кеша.
      */
    def cacheKey(id: String): String =
      id + model.CACHE_KEY_SUFFIX
  }

}


// Логика переехала отсюда в EsModel.api.


/** Поддержка связи [[EsModel]].api Cache и событий SioNotifier. */
trait Sn4EsModelCache
  extends SNStaticSubscriber
  with SnClassSubscriber
{

  val model: EsModelStaticCacheableT
  def cache: AsyncCacheApi

  /**
   * Фунцкия возвращает строку id, извлеченную из полученного события.
   * @param event Полученное событие.
   * @return String либо null, если нет возможности извлечь id из события.
   */
  def event2id(event: Event): String

  /**
   * Передать событие подписчику.
   * @param event событие.
   * @param ctx контекст sio-notifier.
   */
  def publish(event: Event)(implicit ctx: ActorContext): Unit = {
    val idOrNull = event2id(event)
    if (idOrNull != null) {
      val ck = model.cacheKey(idOrNull)
      cache.remove(ck)
    }
  }

}
