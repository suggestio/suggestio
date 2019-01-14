package io.suggest.es.model

import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.es.util.SioEsUtil._
import io.suggest.primo.id.OptId
import org.elasticsearch.action.DocWriteResponse.Result
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.get.MultiGetRequest.Item
import org.elasticsearch.action.get.MultiGetResponse
import org.elasticsearch.action.index.IndexRequestBuilder

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:32
 * Description: Файл содержит трейты для базовой сборки типичных ES-моделей, без parent-child и прочего.
 */

object EsModelStaticT {

  def delResp2isDeleted(dr: DeleteResponse): Boolean = {
    dr.getResult == Result.DELETED
  }

}

/** Базовый шаблон для статических частей ES-моделей, НЕ имеющих _parent'ов. Применяется в связке с [[EsModelT]].
  * Здесь десериализация полностью выделена в отдельную функцию. */
trait EsModelStaticT extends EsModelCommonStaticT {

  override type T <: EsModelT

  import mCommonDi._

  def prepareGet(id: String) = {
    prepareGetBase(id)
  }

  def prepareUpdate(id: String) = prepareUpdateBase(id)
  def prepareDelete(id: String) = prepareDeleteBase(id)

  /**
   * Существует ли указанный документ в текущем типе?
   *
   * @param id id документа.
   * @return true/false
   */
  def isExist(id: String): Future[Boolean] = {
    prepareGet(id)
      .setFetchSource(false)
      .executeFut()
      .map { _.isExists }
  }

  /** Дефолтовое значение GetArgs, когда GET-опции не указаны. */
  def _getArgsDflt: GetOpts = GetOptsDflt

  /**
   * Выбрать ряд из таблицы по id.
   *
   * @param id Ключ документа.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: String, options: GetOpts = _getArgsDflt): Future[Option[T]] = {
    val rq = prepareGet(id)
    for (sf <- options.sourceFiltering) {
      rq.setFetchSource(sf.includes.toArray, sf.excludes.toArray)
    }
    rq.executeFut()
      .map { deserializeGetRespFull }
  }

  /** Вернуть id если он задан. Часто бывает, что idOpt, а не id. */
  def maybeGetById(idOpt: Option[String], options: GetOpts = _getArgsDflt): Future[Option[T]] = {
    FutureUtil.optFut2futOpt(idOpt) {
      getById(_, options)
    }
  }

  /**
   * Выбрать документ из хранилища без парсинга. Вернуть сырое тело документа (его контент).
   *
   * @param id id документа.
   * @return Строка json с содержимым документа или None.
   */
  def getRawContentById(id: String): Future[Option[String]] = {
    prepareGet(id)
      .executeFut()
      .map { EsModelUtil.deserializeGetRespBodyRawStr }
  }

  /**
   * Прочитать документ как бы всырую.
   *
   * @param id id документа.
   * @return Строка json с документом полностью или None.
   */
  def getRawById(id: String): Future[Option[String]] = {
    prepareGet(id)
      .executeFut()
      .map { EsModelUtil.deserializeGetRespFullRawStr }
  }

  /**
   * Прочитать из базы все перечисленные id разом.
   *
   * @param ids id документов этой модели. Можно передавать как коллекцию, так и свеженький итератор оной.
   * @param acc0 Начальный аккамулятор.
   * @return Список результатов в порядке ответа.
   */
  def multiGet(ids: TraversableOnce[String], options: GetOpts = _getArgsDflt): Future[Stream[T]] = {
    if (ids.isEmpty) {
      Future.successful( Stream.empty )
    } else {
      multiGetRaw(ids, options)
        .map( mgetResp2Stream )
    }
  }
  def multiGetRaw(ids: TraversableOnce[String], options: GetOpts = _getArgsDflt): Future[MultiGetResponse] = {
    val req = esClient.prepareMultiGet()
      .setRealtime(true)
    for (id <- ids) {
      val item = new Item(ES_INDEX_NAME, ES_TYPE_NAME, id)
      for (sf <- options.sourceFiltering)
        item.fetchSourceContext( sf.toFetchSourceCtx )
      req.add(item)
    }
    req.executeFut()
  }


  /** Тоже самое, что и multiget, но этап разбора ответа сервера поточный: элементы возвращаются по мере парсинга. */
  def multiGetSrc(ids: Traversable[String], options: GetOpts = _getArgsDflt): Source[T, _] = {
    if (ids.isEmpty) {
      Source.empty
    } else {
      lazy val idsCount = ids.size
      lazy val logPrefix = s"multiGetSrc($idsCount)#${System.currentTimeMillis()}:"
      LOGGER.trace(s"$logPrefix Will source $idsCount items: ${ids.mkString(", ")}")

      val srcFut = for {
        resp <- multiGetRaw(ids, options = options)
      } yield {
        LOGGER.trace(s"$logPrefix Fetched ${resp.getResponses.length} of ${ids.size}")
        val items = resp.getResponses
        if (items.isEmpty) {
          Source.empty
        } else {
          Source( items.toStream )
        }
      }
      Source
        .fromFutureSource( srcFut )
        .filterNot { i =>
          val r = i.isFailed || !i.getResponse.isExists
          if (r)
            LOGGER.trace(s"$logPrefix Dropping ${i.getId} as failed or invalid: ${Option(i.getFailure).flatMap(f => Option(f.getMessage))}")
          r
        }
        .map { i =>
          deserializeOne2( i.getResponse )
        }
    }
  }


  /**
   * Пакетно вернуть инстансы модели с указанными id'шниками, но в виде карты (id -> T).
   * Враппер над multiget, но ещё вызывает resultsToMap над результатами.
   *
   * @param ids Коллекция или итератор необходимых id'шников.
   * @param acc0 Необязательный начальный акк. полезен, когда некоторые инстансы уже есть на руках.
   * @return Фьючерс с картой результатов.
   */
  def multiGetMap(ids: TraversableOnce[String], options: GetOpts = _getArgsDflt): Future[Map[String, T]] = {
    multiGet(ids, options = options)
      // Конвертим список результатов в карту, где ключ -- это id. Если id нет, то выкидываем.
      .map { resultsToMap }
  }


  /** Сконвертить распарсенные результаты в карту. */
  def resultsToMap(results: TraversableOnce[T]): Map[String, T] = {
    OptId.els2idMap[String, T](results)
  }


  /**
   * Генератор delete-реквеста. Используется при bulk-request'ах.
   *
   * @param id adId
   * @return Новый экземпляр DeleteRequestBuilder.
   */
  def deleteRequestBuilder(id: String): DeleteRequestBuilder = {
    val req = prepareDelete(id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  /**
   * Удалить документ по id.
   *
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def deleteById(id: String): Future[Boolean] = {
    deleteRequestBuilder(id)
      .executeFut()
      .map { EsModelStaticT.delResp2isDeleted }
  }

  /** Удаляем сразу много элементов.
    * @return Обычно Some(BulkResponse), но если нет id'шников в запросе, то будет None.
    */
  def deleteByIds(ids: TraversableOnce[String]): Future[Option[BulkResponse]] = {
    if (ids.isEmpty) {
      Future.successful(null)
    } else {
      val bulk = esClient.prepareBulk()
      for (id <- ids) {
        bulk.add(
          prepareDelete(id)
        )
      }
      bulk
        .executeFut()
        .map( EmptyUtil.someF )
    }
  }


  def resave(id: String): Future[Option[String]] = {
    resaveBase( getById(id) )
  }

  def reget(inst0: T): Future[Option[T]] = {
    getById(inst0.id.get)
  }

  /** Генератор indexRequestBuilder'ов. Помогает при построении bulk-реквестов. */
  override def prepareIndexNoVsn(m: T): IndexRequestBuilder = {
    val irb = super.prepareIndexNoVsn(m)

    val rkOpt = getRoutingKey(m.idOrNull)
    if (rkOpt.isDefined)
      irb.setRouting(rkOpt.get)

    irb
  }


  def MAX_WALK_STEPS = 50

  /** Обойти с помощью функции, которая выдаёт узлы для следующего шага.
    *
    * @param ids id элементов.
    * @param acc0 Аккамулятор.
    * @param multiGetSrcF Функция для сорсинга. Для кэша модели можно переопределить.
    * @param f Функция обработки одного элемента.
    * @tparam A Тип аккамулятора.
    * @return Фьючерс с финальным аккамулятором.
    */
  def walkUsing[A](acc0: A,  ids: Set[String],  multiGetSrcF: Set[String] => Source[T, _], counter: Int = 0)
                  (f: (A, T) => (A, Set[String])): Future[A] = {
    lazy val logPrefix = s"walkUsing(${ids.size})[$counter]:"

    if (counter >= MAX_WALK_STEPS) {
      LOGGER.error(s"$logPrefix Stop, too many walk steps.\n ids[${ids.size}] = ${ids.mkString(", ")}")
      Future.failed( new IllegalArgumentException(s"$logPrefix Too many iterations: $counter") )

    } else if (ids.isEmpty) {
      Future.successful(acc0)

    } else {
      // Сорсим элементы по id из хранилища:
      LOGGER.trace(s"$logPrefix Step#${counter}")
      multiGetSrc(ids)
        .toMat(
          Sink.fold (acc0 -> Set.empty[String]) {
            case ((xacc0, idsAcc0), el) =>
              // TODO Opt Для теоретического ускорения работы можно запускать чтение НОВЫХ id сразу после каждого f() и заброс фьючерсов в акк.
              val (xacc2, ids2) = f(xacc0, el)
              xacc2 -> (idsAcc0 ++ ids2)
          }
        )( Keep.right )
        .run()
        .flatMap { case (acc2, needIds2) =>
          walkUsing(acc2, needIds2, multiGetSrcF)(f)
        }
    }
  }


  /** Обойти с помощью функции, которая выдаёт узлы для следующего шага.
    * Можно применить Пригодно для поиска
    *
    * @param els Узлы, начиная от которых надо плясать.
    * @param acc0 Начальный аккамулятор.
    * @param f Функция, определяющая новый акк и список узлов, которые тоже надо тоже отработать.
    * @tparam A Тип аккамулятора.
    * @return Итоговый аккамулятор функции.
    */
  def walk[A](acc0: A, ids: Set[String])
             (f: (A, T) => (A, Set[String])): Future[A] = {
    walkUsing(acc0, ids, multiGetSrc(_))(f)
  }

}

/** Реализация трейта [[EsModelStaticT]] для уменьшения работы компилятору. */
abstract class EsModelStatic extends EsModelStaticT


/** Шаблон для динамических частей ES-моделей.
 * В минимальной редакции механизм десериализации полностью абстрактен. */
trait EsModelT extends EsModelCommonT {
}

/** Доп.API для инстансов ES-моделей с явной поддержкой версий. */
trait EsModelVsnedT[T <: EsModelVsnedT[T]] extends EsModelCommonT {

  def withVersion(versionOpt: Option[Long]): T // = copy(versionOpt = versionOpt)

  // После самого первого сохранения выставляется вот эта вот версия:
  def withFirstVersion = withVersion( Some(1L) )

}
