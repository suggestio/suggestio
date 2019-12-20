package io.suggest.es.model

import io.suggest.es.MappingDsl.Implicits._
import io.suggest.util.logs.MacroLogsImplLazy
import io.suggest.util.{JacksonWrapper, JmxBase}
import javax.inject.{Inject, Singleton}
import japgolly.univeq._

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:26
 * Description:
 * JMX-подсистема для ES-моделей. Включает общие для моделей MBean-интерфейсы и реализации.
 */

trait EsModelJMXMBeanCommonI {

  /** Асинхронно вызвать переиндексацию всех данных в модели. */
  def resaveAll(): String

  /**
   * Существует ли указанный маппинг сейчас?
   * @return true, если маппинг сейчас существует.
   */
  def isMappingExists: Boolean

  /** Асинхронно отправить маппинг в ES. */
  def putMapping(): String

  def generateMapping(): String
  def readCurrentMapping(): String

  /** Запросить routingKey у модели.
    * @param idOrNull исходный id
    * @return Будет распечатана строка вида "Some(..)" или "None".
    */
  def getRoutingKey(idOrNull: String): String

  def esIndexName: String
  def esTypeName: String

  /**
   * Выдать сколько-то id'шников в алфавитном порядке.
   * @param maxResults Макс.кол-во выдачи.
   * @return Текст, в каждой строчке новый id.
   */
  def getAllIds(maxResults: Int): String
  def getAll(maxResults: Int): String

  /** Удаление всех элементов.
    *
    * @param sure Уверен?
    * @return Данные об удалённом.
    */
  def truncate(sure: String): String

  /** Подсчитать кол-во элементов. */
  def countAll(): String
}



trait EsModelCommonJMXBase extends JmxBase with EsModelJMXMBeanCommonI with MacroLogsImplLazy {

  // DI:
  def companion: EsModelCommonStaticT { type T = X }
  val esModelJmxDi: EsModelJmxDi

  import esModelJmxDi.ec
  import esModelJmxDi.esModel.api._
  import JmxBase._

  type X <: EsModelCommonT

  // Контексты, зависимые от конкретного проекта.
  override def _jmxType = "elasticsearch"

  /** Ругнутся в логи и вернуть строку для возврата клиенту. */
  protected def _formatEx(logPrefix: String, data: String, ex: Throwable): String = {
    LOGGER.error(s"${logPrefix}Failed to make JMX Action:\n$data", ex)
    ex.getClass.getName + ": " + ex.getMessage + "\n\n" + ex.getStackTrace.mkString("\n")
  }

  override def resaveAll(): String = {
    LOGGER.warn(s"resaveAll()")
    val fut = for (
      totalProcessed <- companion.resaveAll()
    ) yield {
      s"Total: $totalProcessed; failures: ??? (unawailable here, see logs)"
    }
    awaitString(fut)
  }


  override def isMappingExists: Boolean = {
    LOGGER.trace(s"isMappingExists()")
    val fut = companion.isMappingExists()
    awaitFuture( fut )
  }

  override def putMapping(): String = {
    LOGGER.warn(s"putMapping()")
    val fut = companion
      .putMapping()
      .map(_.toString)
      .recover { case ex: Throwable => _formatEx(s"putMapping()", "", ex) }
    awaitFuture( fut )
  }

  override def generateMapping(): String = {
    val logPrefix = "generateMapping()"
    LOGGER.debug(s"$logPrefix called")
    JmxBase.tryCatch { () =>
      val mappingText = companion
        .generateMapping()
        .toString()
      JacksonWrapper.prettify(mappingText)
    }
  }

  override def readCurrentMapping(): String = {
    LOGGER.debug("readCurrentMapping()")
    val fut = for (res <- companion.getCurrentMapping()) yield {
      res.fold("Mapping not found.") { JacksonWrapper.prettify }
    }
    awaitString(fut)
  }

  override def getRoutingKey(idOrNull: String): String = {
    LOGGER.debug(s"getRoutingKey($idOrNull)")
    val idOrNull2 = if (idOrNull == null) idOrNull else idOrNull.trim
    companion.getRoutingKey(idOrNull2).toString
  }

  override def getAllIds(maxResults: Int): String = {
    LOGGER.debug(s"getAllIds(maxResults = $maxResults)")
    val fut = companion.getAllIds(maxResults)
      .map { _.sorted.mkString("\n") }
    awaitString(fut)
  }

  override def getAll(maxResults: Int): String = {
    LOGGER.info(s"getAll(maxResults = $maxResults)")
    val fut = companion.getAll(maxResults, withVsn = true)
      .map { r =>
        val resultNonPretty = companion.toEsJsonDocs(r)
        JacksonWrapper.prettify(resultNonPretty)
      }
    awaitString(fut)
  }

  override def esTypeName = companion.ES_TYPE_NAME
  override def esIndexName = companion.ES_INDEX_NAME

  override def countAll(): String = {
    LOGGER.debug(s"countAll()")
    val fut = companion.countAll()
      .map { _.toString }
    awaitString(fut)
  }

  /** Удаление всех элементов.
    *
    * @param sure Уверен?
    * @return Данные об удалённом.
    */
  override def truncate(sure: String): String = {
    LOGGER.warn(s"truncate($sure) called. Will delete all elemets...")
    val fut = for {
      totalDeleted <- companion.truncate( sure ==* "YES!!!" )
    } yield {
      s"Total ${totalDeleted} docs erased."
    }
    awaitString( fut )
  }

}


/** Контейнер DI-инстансов для  */
@Singleton
final case class EsModelJmxDi @Inject() (
                                          val esModel     : EsModel,
                                          implicit val ec : ExecutionContext,
                                        )
