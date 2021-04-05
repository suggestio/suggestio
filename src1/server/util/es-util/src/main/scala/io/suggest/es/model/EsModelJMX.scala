package io.suggest.es.model

import io.suggest.xplay.json.PlayJsonUtil

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

  // put*() закомменчены, т.к. не используются, и надо переписать реализации.
  /**
   * Отправить в хранилище один экземпляр модели, представленный в виде JSON.
   * @param data Сериализованный в JSON экземпляр модели.
   * @return id сохраненного документа.
   */
  //def putOne(id: String, data: String): String

  /** Выхлоп getAll() отправить на сохранение в хранилище.
    * @param all Выхлоп getAll().
    */
  //def putAll(all: String): String

  def refreshIndex(): String

}


trait EsModelJMXBase extends EsModelCommonJMXBase with EsModelJMXMBeanI {

  import esModelJmxDi.ec
  import esModelJmxDi.esModel.api._
  import io.suggest.util.JmxBase._

  override type X <: EsModelT

  override def companion: EsModelStaticT { type T = X }

  override def resave(id: String): String = {
    LOGGER.info(s"resave($id): jmx")
    val fut = companion.resave(id) map {
      case Some(_id) => "Resaved " + _id
      case None      => "Not found id: " + id
    }
    awaitString(fut)
  }

  override def deleteById(id: String): Boolean = {
    val id1 = id.trim
    LOGGER.warn(s"deleteById($id1)")
    val fut = companion.deleteById(id1)
    awaitFuture( fut )
  }

  override def getById(id: String): String = {
    val id1 = id.trim
    LOGGER.debug(s"getById($id1)")
    val fut = for (res <- companion.getById(id1)) yield {
      res.fold("not found")(companion.toJsonPretty)
    }
    awaitString(fut)
  }

  override def getRawById(id: String): String = {
    val id1 = id.trim
    LOGGER.debug(s"getRawById($id1)")
    val fut = for (res <- companion.getRawById(id1)) yield {
      res.fold("not found")(PlayJsonUtil.prettify)
    }
    awaitString(fut)
  }

  override def getRawContentById(id: String): String = {
    val id1 = id.trim
    LOGGER.debug(s"getRawContentById($id1)")
    val fut = for (
      res <- companion.getRawContentById(id1)
    ) yield {
      res.fold("not found")(PlayJsonUtil.prettify)
    }
    awaitString(fut)
  }

  // Надо переписать на toJson / play-json.
  /*
  override def putOne(id: String, data: String): String = {
    val id1 = id.trim
    val logPrefix = s"putOne($id1):"
    LOGGER.info(s"$logPrefix $data")
    val idOpt = Option(id1)
      .filter(!_.isEmpty)
    try {
      val br = new BytesArray( data )
      val dataMap = SourceLookup.sourceAsMap(br)
      val fut = _saveOne(idOpt, dataMap)
      awaitFuture( fut )
    } catch {
      case ex: Throwable =>
        _formatEx(s"$logPrefix: ", data, ex)
    }
  }

  override def putAll(all: String): String = {
    import SioEsUtil.StdFns._
    LOGGER.info("putAll(): " + all)
    try {
      val raws = JacksonWrapper.deserialize[List[Map[String, AnyRef]]](all)
      val idsFut = Future.traverse(raws) { tmap =>
        val idOpt = tmap
          .get( FIELD_ID )
          .map(_.toString.trim)
        val sourceStr = JacksonWrapper.serialize(tmap.get( FIELD_SOURCE ))
        val br = new BytesArray(sourceStr)
        val dataMap = SourceLookup.sourceAsMap(br)
        _saveOne(idOpt, dataMap)
      }
      val resFut = for (ids <- idsFut) yield {
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
  import scala.jdk.CollectionConverters._

  // Общий код парсинга и добавления элементов в хранилище вынесен сюда.
  private def _saveOne(idOpt: Option[String], dataMap: ju.Map[String, AnyRef], versionOpt: Option[Long] = None): Future[String] = {
    val inst = companion
      // TODO Придумать что-то, использующее deserializeOne2()
      .deserializeOne2( dataMap.asScala )
    companion.save(inst)
  }
  */


  override def refreshIndex(): String = {
    LOGGER.debug(s"refreshIndex(): ${companion.ES_INDEX_NAME}")
    val fut = for (res <- companion.refreshIndex()) yield {
      s"Done, res =\n$res"
    }
    awaitString(fut)
  }

}

/** Недореализация [[EsModelJMXBase]] для снижения объемов кодогенерации scalac на ~75% (6.9кб -> 1.6кб). */
abstract class EsModelJMXBaseImpl extends EsModelJMXBase
