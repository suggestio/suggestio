package io.suggest.es.model

import akka.actor.ActorContext
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink}
import io.suggest.common.fut.FutureUtil
import io.suggest.di.{ICacheApi, IExecutionContext}
import io.suggest.event.SNStaticSubscriber
import io.suggest.event.SioNotifier.Event
import io.suggest.event.subscriber.SnClassSubscriber

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
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
abstract class EsModelCache[T1 <: EsModelT : ClassTag]
  extends ICacheApi
  with IExecutionContext
{

  implicit protected val mat: Materializer

  def companion: EsModelStaticT { type T = T1 }

  val EXPIRE            : FiniteDuration
  val CACHE_KEY_SUFFIX  : String

  /**
   * Генерация ключа кеша.
   * @param id id исходного документа.
   * @return Строка, пригодная для использования в качестве ключа кеша.
   */
  def cacheKey(id: String): String = id + CACHE_KEY_SUFFIX

  def getByIdFromCache(id: String): Future[Option[T1]] = {
    val ck = cacheKey(id)
    cache.get[T1](ck)
  }

  /**
   * Вернуть закешированный результат либо прочитать его из хранилища.
   * @param id id исходного документа.
   * @return Тоже самое, что и исходный getById().
   */
  def getById(id: String): Future[Option[T1]] = {
    // 2014.nov.24: Форсируем полный асинхрон при работе с кешем.
    val ck = cacheKey(id)
    cache.get[T1](ck)
      .filter { _.isDefined }
      .recoverWith { case _: NoSuchElementException =>
        getByIdAndCache(id, ck)
      }
  }

  /**
   * Аналог getByIdCached, но для multiget().
   * @param ids id'шники, которые надо бы получить. Ожидается, что будут без дубликатов.
   * @return Результаты в неопределённом порядке.
   */
  def multiGet(ids: Iterable[String]): Future[Seq[T1]] = {
    if (ids.isEmpty) {
      Future.successful(Nil)

    } else {
      // Сначала ищем элементы в кэше. Можно искать в несколько потоков, что и делаем:
      val cachedFoundEithersFut = Future.traverse( ids ) { id =>
        for (resOpt <- getByIdFromCache(id)) yield
          resOpt.toRight( id )
      }

      cachedFoundEithersFut.flatMap { cachedFoundEiths =>

        // Как можно скорее запустить multiGET к базе за недостающими элементами:
        val nonCachedIds = cachedFoundEiths
          .iterator
          .collect {
            case Left(id) => id
          }
          .toSet

        // Ленивая коллекция уже закэшированных результатов:
        val cachedResults = cachedFoundEiths
          .iterator
          .collect {
            case Right(r) => r
          }
          // Максимально лениво, чтобы отложить это всё напотом без lazy val или def:
          .toStream

        // Если есть отсутствующие в кэше элементы, то поискать их в хранилищах:
        if (nonCachedIds.nonEmpty) {
          // Строим трубу для чтения и параллельного кэширования элементов. Не очень ясно, будет ли это быстрее, но вполне модно и реактивно:
          val nonCachedResultsFut = companion
            .multiGetSrc( nonCachedIds )
            .alsoTo(
              Sink.foreach[T1]( cacheThat )
            )
            // TODO Opt Дописывать сразу в аккамулятор из cachedResults вместо начально-пустого акка? Начальный seq builder инициализировать с size?
            .toMat( Sink.seq )( Keep.right )
            .run()

          // Объеденить результаты из кэша и результаты из хранилища:
          val allResFut = if (cachedResults.isEmpty) {
            nonCachedResultsFut
          } else {
            for (nonCachedResults <- nonCachedResultsFut) yield
              // Vector ++ Stream.
              nonCachedResults ++ cachedResults
          }

          // Вернуть итоговый фьючерс с объединёнными результатами (в неочень неопределённом порядке):
          allResFut

        } else {
          Future.successful( cachedResults )
        }
      }
    }
  }


  def multiGetMap(ids: Set[String]): Future[Map[String, T1]] = {
    multiGet(ids)
      .map { companion.resultsToMap }
  }


  def cacheThat(result: T1): Unit = {
    val id = result.id.get
    val ck = cacheKey(id)
    cache.set(ck, result, EXPIRE)
  }

  def cacheThese(results: T1*): Unit =
    cacheThese1(results)

  /** Принудительное кэширование для всех указанных item'ов. */
  def cacheThese1(results: TraversableOnce[T1]): Unit =
    results.foreach( cacheThat )


  /**
   * Если id задан, то прочитать из кеша или из хранилища. Иначе вернуть None.
   * @param idOpt Опциональный id.
   * @return Тоже самое, что и [[getById]].
   */
  def maybeGetByIdCached(idOpt: Option[String]): Future[Option[T1]] = {
    FutureUtil.optFut2futOpt(idOpt)(getById)
  }
  def maybeGetByEsIdCached(esIdOpt: Option[MEsUuId]): Future[Option[T1]] = {
    maybeGetByIdCached(
      esIdOpt.map(_.id)
    )
  }

  /**
   * Прочитать из хранилища документ, и если всё нормально, то отправить его в кеш.
   * @param id id документа.
   * @param ck0 Ключ в кеше.
   * @return Тоже самое, что и исходный getById().
   */
  def getByIdAndCache(id: String, ck0: String = null): Future[Option[T1]] = {
    val ck: String = if (ck0 != null) cacheKey(id) else ck0
    val resultFut = companion.getById(id)
    for (adnnOpt <- resultFut) {
      for (adnn <- adnnOpt)
        cache.set(ck, adnn, EXPIRE)
    }
    resultFut
  }


  def put(value: T1): Future[_] = {
    val ck = cacheKey( value.id.get )
    cache.set(ck, value, EXPIRE)
  }

}


/** Поддержка связи [[EsModelCache]] и событий SioNotifier. */
trait Sn4EsModelCache
  extends SNStaticSubscriber
  with SnClassSubscriber
  with ICacheApi
{

  def cacheKey(id: String): String

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
      cache.remove(ck)
    }
  }

}

