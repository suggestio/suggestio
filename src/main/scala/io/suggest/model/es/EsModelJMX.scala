package io.suggest.model.es

import io.suggest.util.{SioConstants, JacksonWrapper}
import org.elasticsearch.search.lookup.SourceLookup

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:30
 * Description:
 */

trait EsModelJMXMBeanI extends EsModelJMXMBeanCommonI {

  /**
   * Выполнить удаление документа.
   * @param id id удаляемого докуметна.
   * @return true, если элемент был удалён. Иначе false.
   */
  def deleteById(id: String): Boolean

  /**
   * Прочитать из хранилища и вернуть данные по документу.
   * @param id id документа
   * @return pretty JSON или null.
   */
  def getById(id: String): String


  /**
   * Выдать документ "в сырую".
   * @param id id документа.
   * @return Сырая строка json pretty.
   */
  def getRawById(id: String): String

  /**
   * Выдать содержимое документа без парсинга.
   * @param id id документа.
   * @return Сырая строка json pretty.
   */
  def getRawContentById(id: String): String

  def resave(id: String): String

  /**
   * Отправить в хранилище один экземпляр модели, представленный в виде JSON.
   * @param data Сериализованный в JSON экземпляр модели.
   * @return id сохраненного документа.
   */
  def putOne(id: String, data: String): String

  /** Выхлоп getAll() отправить на сохранение в хранилище.
    * @param all Выхлоп getAll().
    */
  def putAll(all: String): String

}



trait EsModelJMXBase extends EsModelCommonJMXBase with EsModelJMXMBeanI {

  import LOGGER._

  override def companion: EsModelStaticT

  override def resave(id: String): String = {
    trace(s"resave($id): jmx")
    val fut = companion.resave(id) map {
      case Some(_id) => "Resaved " + _id
      case None      => "Not found id: " + id
    }
    awaitString(fut)
  }

  override def deleteById(id: String): Boolean = {
    val id1 = id.trim
    warn(s"deleteById($id1)")
    companion.deleteById(id1)
  }

  override def getById(id: String): String = {
    val id1 = id.trim
    trace(s"getById($id1)")
    val fut = for (res <- companion.getById(id1)) yield {
      res.fold("not found")(_.toJsonPretty)
    }
    awaitString(fut)
  }

  override def getRawById(id: String): String = {
    val id1 = id.trim
    trace(s"getRawById($id1)")
    val fut = for (res <- companion.getRawById(id1)) yield {
      res.fold("not found")(JacksonWrapper.prettify)
    }
    awaitString(fut)
  }

  override def getRawContentById(id: String): String = {
    val id1 = id.trim
    trace(s"getRawContentById($id1)")
    val fut = for (res <- companion.getRawContentById(id1)) yield {
      res.fold("not found")(JacksonWrapper.prettify)
    }
    awaitString(fut)
  }

  override def putOne(id: String, data: String): String = {
    val id1 = id.trim
    val logPrefix = s"putOne($id1):"
    info(s"$logPrefix $data")
    val idOpt = Option(id1)
      .filter(!_.isEmpty)
    val b = data.getBytes
    try {
      val dataMap = SourceLookup.sourceAsMap(b, 0, b.length)
      _saveOne(idOpt, dataMap)
    } catch {
      case ex: Throwable =>
        _formatEx(s"$logPrefix: ", data, ex)
    }
  }

  override def putAll(all: String): String = {
    info("putAll(): " + all)
    try {
      val raws = JacksonWrapper.deserialize[List[Map[String, AnyRef]]](all)
      val idsFut = Future.traverse(raws) { tmap =>
        val idOpt = tmap.get( SioConstants.FIELD_ID ).map(_.toString.trim)
        val sourceStr = JacksonWrapper.serialize(tmap get SioConstants.FIELD_SOURCE)
        val b = sourceStr.getBytes
        val dataMap = SourceLookup.sourceAsMap(b, 0, b.length)
        _saveOne(idOpt, dataMap)
      }
      val resFut = idsFut.map { ids =>
        (("Total saved: " + ids.size) :: "----" :: ids)
          .mkString("\n")
      }
      awaitString(resFut)
    } catch {
      case ex: Throwable =>
        _formatEx(s"putAll(${all.length}): ", all, ex)
    }
  }


  import java.{util => ju}
  import scala.collection.JavaConversions._

  /** Общий код парсинга и добавления элементов в хранилище вынесен сюда. */
  private def _saveOne(idOpt: Option[String], dataMap: ju.Map[String, AnyRef], versionOpt: Option[Long] = None): Future[String] = {
    // TODO Придумать что-то, использующее deserializeOne2()
    companion
      .deserializeOne(idOpt, dataMap, version = None)
      .save
  }

}

