package models

import io.suggest.model.{EsModelT, EsModelStaticT}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event.subscriber.SnClassSubscriber
import io.suggest.event.SNStaticSubscriber
import play.api.Play.current
import play.api.cache.Cache
import io.suggest.event.SioNotifier.Event
import akka.actor.ActorContext
import io.suggest.ym.model.common.EMAdNetMember
import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 11:30
 * Description: Барахло для создания кеширующих моделей. Реализовано в виде class'ов из-за
 * необходимости передавать ClassTag.
 */

/** В sioweb есть быстрый кеш, поэтому тут кеш-прослойка для моделей. */
// TODO Следует засунуть поддержку ehcache в sioutil и отправить этот трейт с кеш-поддержкой туда.
// TODO Это по идее как бы трейт, но из-за ClassTag использовать trait нельзя.
abstract class EsModelCache[T1 <: EsModelT : ClassTag] extends SNStaticSubscriber with SnClassSubscriber {

  type StaticModel_t <: EsModelStaticT { type T = T1 }
  def companion: StaticModel_t

  val EXPIRE_SEC: Int
  val CACHE_KEY_SUFFIX: String

  type GetAs_t

  /**
   * Генерация ключа кеша.
   * @param id id исходного документа.
   * @return Строка, пригодная для использования в качестве ключа кеша.
   */
  def cacheKey(id: String): String = id + CACHE_KEY_SUFFIX

  def getByIdFromCache(id: String): Option[T1] = {
    val ck = cacheKey(id)
    Cache.getAs[T1](ck)
  }

  /**
   * Вернуть закешированный результат либо прочитать его из хранилища.
   * @param id id исходного документа.
   * @return Тоже самое, что и исходный getById().
   */
  def getById(id: String)(implicit ec: ExecutionContext, client: Client): Future[Option[T1]] = {
    // 2014.nov.24: Форсируем полный асинхрон при работе с кешем.
    val ck = cacheKey(id)
    Future { Cache.getAs[T1](ck) }
      .filter { _.isDefined }
      .recoverWith { case ex: NoSuchElementException => getByIdAndCache(id, ck) }
  }

  /**
   * Аналог getByIdCached, но для multiget().
   * @param ids id'шники, которые надо бы получить.
   * @param acc0 Необязательный начальный аккамулятор.
   * @return Результаты в неопределённом порядке.
   */
  def multiGet(ids: TraversableOnce[String], acc0: List[T1] = Nil)(implicit ec: ExecutionContext, client: Client): Future[Seq[T1]] = {
    val (cached, nonCachedIds) = ids.foldLeft [(List[T1], List[String])] (acc0 -> Nil) {
      case ((accCached, notCached), id) =>
        getByIdFromCache(id) match {
          case Some(adnNode) =>
            (adnNode :: accCached) -> notCached
          case None =>
            accCached -> (id :: notCached)
        }
    }
    val resultFut = companion.multiGet(nonCachedIds, acc0 = cached)
    // Асинхронно отправить в кеш всё, чего там ещё не было.
    if (nonCachedIds.nonEmpty) {
      resultFut onSuccess { case results =>
        val ncisSet = nonCachedIds.toSet
        results.foreach { result =>
          if (ncisSet contains result.idOrNull) {
            val ck = cacheKey(result.idOrNull)
            Cache.set(ck, result, EXPIRE_SEC)
          }
        }
      }
    }
    resultFut
  }

  def multiGetMap(ids: TraversableOnce[String], acc0: List[T1] = Nil)
                 (implicit ec: ExecutionContext, client: Client): Future[Map[String, T1]] = {
    multiGet(ids, acc0)
      .map { companion.resultsToMap }
  }


  /**
   * Если id задан, то прочитать из кеша или из хранилища. Иначе вернуть None.
   * @param idOpt Опциональный id.
   * @return Тоже самое, что и [[getById]].
   */
  def maybeGetByIdCached(idOpt: Option[String])(implicit ec: ExecutionContext, client: Client): Future[Option[T1]] = {
    idOpt match {
      case Some(id) => getById(id)
      case None     => Future successful None
    }
  }

  /**
   * Прочитать из хранилища документ, и если всё нормально, то отправить его в кеш.
   * @param id id документа.
   * @param ck0 Ключ в кеше.
   * @return Тоже самое, что и исходный getById().
   */
  def getByIdAndCache(id: String, ck0: String = null)(implicit ec: ExecutionContext, client: Client): Future[Option[T1]] = {
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

/** EsModelCache - расширение [[EsModelCache]] с фильтрацией по adn.memberType. */
abstract class AdnEsModelCache[T <: EMAdNetMember : ClassTag] extends EsModelCache[T] {

  /**
   * Для AdnNode и других последователей EMAdNetMember.
   * @param id id элемента.
   * @param memberType тип участника рекламной сети.
   * @return Тоже самое, что и все остальные getById().
   */
  def getByIdType(id: String, memberType: AdNetMemberType)
                       (implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
    val ck = cacheKey(id)
    Cache.getAs[T](ck) match {
      case r @ Some(adnm) =>
        val result = if (adnm.adn.memberType == memberType)  r  else  None
        Future successful result

      case None =>
        getByIdAndCache(id, ck)
          .map { _.filter(_.adn.memberType == memberType) }
    }
  }

}
