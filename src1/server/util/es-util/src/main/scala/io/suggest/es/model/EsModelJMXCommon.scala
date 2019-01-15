package io.suggest.es.model

import io.suggest.util.logs.MacroLogsImplLazy
import io.suggest.util.{JMXBase, JacksonWrapper}
import javax.inject.{Inject, Singleton}

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

  /** Подсчитать кол-во элементов. */
  def countAll(): String
}



trait EsModelCommonJMXBase extends JMXBase with EsModelJMXMBeanCommonI with MacroLogsImplLazy {

  // DI:
  def companion: EsModelCommonStaticT { type T = X }
  val esModelJmxDi: EsModelJmxDi
  override implicit def ec = esModelJmxDi.ec

  import esModelJmxDi.esModel.api._

  type X <: EsModelCommonT

  override def jmxName = "io.suggest:type=elasticsearch,name=" + getClass.getSimpleName.replace("Jmx", "")

  // Контексты, зависимые от конкретного проекта.

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
    companion.isMappingExists()
  }

  override def putMapping(): String = {
    LOGGER.warn(s"putMapping()")
    companion.putMapping()
      .map(_.toString)
      .recover { case ex: Throwable => _formatEx(s"putMapping()", "", ex) }
  }

  override def generateMapping(): String = {
    LOGGER.debug("generateMapping()")
    val mappingText = companion.generateMapping.string()
    JacksonWrapper.prettify(mappingText)
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

  override def esTypeName: String = companion.ES_TYPE_NAME
  override def esIndexName: String = companion.ES_INDEX_NAME

  override def countAll(): String = {
    LOGGER.debug(s"countAll()")
    val fut = companion.countAll()
      .map { _.toString }
    awaitString(fut)
  }

}


/** Контейнер DI-инстансов для  */
@Singleton
final case class EsModelJmxDi @Inject() (
                                          val esModel     : EsModel,
                                          implicit val ec : ExecutionContext,
                                        )
