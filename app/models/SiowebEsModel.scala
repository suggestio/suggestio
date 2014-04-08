package models

import io.suggest.model.{EsModelMinimalT, EsModel, EsModelMinimalStaticT}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import play.api.Play.current
import io.suggest.event.subscriber.SnClassSubscriber
import io.suggest.event.SNStaticSubscriber
import play.api.Play.current
import play.api.cache.Cache
import io.suggest.event.SioNotifier.Event
import akka.actor.ActorContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:43
 * Description: Дополнительная утиль для ES-моделей.
 */
object SiowebEsModel {

  /**
   * Список моделей, которые должны быть проинициалированы при старте.
   * @return Список EsModelMinimalStaticT.
   */
  def ES_MODELS: Seq[EsModelMinimalStaticT[_]] = {
    Seq(MBlog, MPerson, MozillaPersonaIdent, EmailPwIdent, EmailActivation, MMartCategory) ++
      EsModel.ES_MODELS
  }

  def putAllMappings(implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
    val ignoreExist = current.configuration.getBoolean("es.mapping.model.ignore_exist") getOrElse false
    EsModel.putAllMappings(ES_MODELS, ignoreExist)
  }

}


/** В sioweb есть быстрый кеш, поэтому тут кеш-прослойка для моделей. */
// TODO Следует засунуть поддержку ehcache в sioutil и отправить этот трейт с кеш-поддержкой туда.
trait EsModelCache[T <: EsModelMinimalT[T]] extends SNStaticSubscriber with SnClassSubscriber {
  def companion: EsModelMinimalStaticT[T]

  val EXPIRE_SEC: Int
  val CACHE_KEY_SUFFIX: String


  /**
   * Генерация ключа кеша.
   * @param id id исходного документа.
   * @return Строка, пригодная для использования в качестве ключа кеша.
   */
  def cacheKey(id: String): String = id + CACHE_KEY_SUFFIX

  /**
   * Вернуть закешированный результат либо прочитать его из хранилища.
   * @param id id исходного документа.
   * @return Тоже самое, что и исходный getById().
   */
  def getByIdCached(id: String)(implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
    val ck = cacheKey(id)
    // Негативные результаты не кешируются.
    Cache.getAs[T](ck) match {
      case Some(adnn) =>
        Future successful Some(adnn)

      case None => getByIdAndCache(id, ck)
    }
  }

  /**
   * Прочитать из хранилища документ, и если всё нормально, то отправить его в кеш.
   * @param id id документа.
   * @param ck0 Ключ в кеше.
   * @return Тоже самое, что и исходный getById().
   */
  def getByIdAndCache(id: String, ck0: String = null)(implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
    val ck: String = if (ck0 == null) cacheKey(id) else ck0
    val resultFut = companion.getById(id)
    resultFut onSuccess {
      case Some(adnn) =>
        Cache.set(ck, adnn, EXPIRE_SEC)
      case _ => // do nothing
    }
    resultFut
  }

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
  def publish(event: Event)(implicit ctx: ActorContext) {
    val idOrNull = event2id(event)
    if (idOrNull != null) {
      val ck = cacheKey(idOrNull)
      Cache.remove(ck)
    }
  }

}

