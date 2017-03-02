package io.suggest.es.model

import io.suggest.common.fut.FutureUtil
import io.suggest.es.util.SioEsUtil._
import io.suggest.primo.id.OptId
import org.elasticsearch.action.delete.DeleteRequestBuilder
import org.elasticsearch.action.get.MultiGetRequest.Item
import org.elasticsearch.action.index.IndexRequestBuilder

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:32
 * Description: Файл содержит трейты для базовой сборки типичных ES-моделей, без parent-child и прочего.
 */

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
   * Существует ли указанный документ в хранилище?
   *
   * @param id id магазина.
   * @return true/false
   */
  def isExist(id: String): Future[Boolean] = {
    prepareGet(id)
      .setFields()
      .execute()
      .map { _.isExists }
  }

  /** Дефолтовое значение GetArgs, когда GET-опции не указаны. */
  def _getArgsDflt: IGetOpts = GetOptsDflt

  /**
   * Выбрать ряд из таблицы по id.
   *
   * @param id Ключ документа.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: String, options: IGetOpts = _getArgsDflt): Future[Option[T]] = {
    val rq = prepareGet(id)
    for (sf <- options.sourceFiltering) {
      rq.setFetchSource(sf.includes.toArray, sf.excludes.toArray)
    }
    rq.execute()
      .map { deserializeGetRespFull }
  }

  /** Вернуть id если он задан. Часто бывает, что idOpt, а не id. */
  def maybeGetById(idOpt: Option[String], options: IGetOpts = _getArgsDflt): Future[Option[T]] = {
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
      .execute()
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
      .execute()
      .map { EsModelUtil.deserializeGetRespFullRawStr }
  }

  /**
   * Прочитать из базы все перечисленные id разом.
   *
   * @param ids id документов этой модели. Можно передавать как коллекцию, так и свеженький итератор оной.
   * @param acc0 Начальный аккамулятор.
   * @return Список результатов в неопределённом порядке.
   */
  def multiGetRev(ids: TraversableOnce[String], acc0: List[T] = Nil, options: IGetOpts = _getArgsDflt): Future[List[T]] = {
    if (ids.isEmpty) {
      Future.successful(acc0)

    } else {
      val req = esClient.prepareMultiGet()
        .setRealtime(true)
      for (id <- ids) {
        val item = new Item(ES_INDEX_NAME, ES_TYPE_NAME, id)
        for (sf <- options.sourceFiltering) {
          item.fetchSourceContext( sf.toFetchSourceCtx )
        }
        req.add(item)
      }
      req.execute()
        .map { mgetResp2list(_, acc0) }
    }
  }

  /** Надстройка над multiGetRev(), но при этом возвращает элементы в исходном порядке (как в es response). */
  def multiGet(ids: TraversableOnce[String], options: IGetOpts = _getArgsDflt): Future[List[T]] = {
    multiGetRev(ids, options = options)
      // В инете не нагуглить гарантий того, что порядок результатов будет соблюдаться согласно ids.
      .map { _.reverse }
  }


  /**
   * Пакетно вернуть инстансы модели с указанными id'шниками, но в виде карты (id -> T).
   * Враппер над multiget, но ещё вызывает resultsToMap над результатами.
   *
   * @param ids Коллекция или итератор необходимых id'шников.
   * @param acc0 Необязательный начальный акк. полезен, когда некоторые инстансы уже есть на руках.
   * @return Фьючерс с картой результатов.
   */
  def multiGetMap(ids: TraversableOnce[String], acc0: List[T] = Nil, options: IGetOpts = _getArgsDflt): Future[Map[String, T]] = {
    multiGetRev(ids, acc0, options)
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
      .execute()
      .map { _.isFound }
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

}

/** Реализация трейта [[EsModelStaticT]] для уменьшения работы компилятору. */
abstract class EsModelStatic extends EsModelStaticT


/** Шаблон для динамических частей ES-моделей.
 * В минимальной редакции механизм десериализации полностью абстрактен. */
trait EsModelT extends EsModelCommonT {
}

