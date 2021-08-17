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

  def refreshIndex(): String

}


trait EsModelJMXBase extends EsModelCommonJMXBase with EsModelJMXMBeanI {

  import esModel.api._
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
