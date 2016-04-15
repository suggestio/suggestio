package io.suggest.model.es

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.util.{MacroLogsImplLazy, JMXBase, JacksonWrapper}
import org.elasticsearch.client.Client

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
  def resaveMany(maxResults: Int): String

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

  import LOGGER._

  def companion: EsModelCommonStaticT

  override def jmxName = "io.suggest:type=elasticsearch,name=" + getClass.getSimpleName.replace("Jmx", "")

  // Контексты, зависимые от конкретного проекта.
  implicit def ec: ExecutionContext
  implicit def client: Client
  implicit def sn: SioNotifierStaticClientI

  /** Ругнутся в логи и вернуть строку для возврата клиенту. */
  protected def _formatEx(logPrefix: String, data: String, ex: Throwable): String = {
    error(s"${logPrefix}Failed to make JMX Action:\n$data", ex)
    ex.getClass.getName + ": " + ex.getMessage + "\n\n" + ex.getStackTrace.mkString("\n")
  }

  override def resaveMany(maxResults: Int): String = {
    warn(s"resaveMany(maxResults = $maxResults)")
    val fut = for (resp <- companion.resaveMany(maxResults)) yield {
      s"Total: ${resp.getItems.length} took=${resp.getTook.seconds()}s\n---------\n ${resp.buildFailureMessage()}"
    }
    awaitString(fut)
  }


  override def isMappingExists: Boolean = {
    trace(s"isMappingExists()")
    companion.isMappingExists
  }

  override def putMapping(): String = {
    warn(s"putMapping()")
    companion.putMapping()
      .map(_.toString)
      .recover { case ex: Throwable => _formatEx(s"putMapping()", "", ex) }
  }

  override def generateMapping(): String = {
    trace("generateMapping()")
    val mappingText = companion.generateMapping.string()
    JacksonWrapper.prettify(mappingText)
  }

  override def readCurrentMapping(): String = {
    trace("readCurrentMapping()")
    val fut = companion.getCurrentMapping.map {
      _.fold("Mapping not found.") { JacksonWrapper.prettify }
    }
    awaitString(fut)
  }

  override def getRoutingKey(idOrNull: String): String = {
    trace(s"getRoutingKey($idOrNull)")
    val idOrNull2 = if (idOrNull == null) idOrNull else idOrNull.trim
    companion.getRoutingKey(idOrNull2).toString
  }

  override def getAllIds(maxResults: Int): String = {
    trace(s"getAllIds(maxResults = $maxResults)")
    val fut = companion.getAllIds(maxResults)
      .map { _.sorted.mkString("\n") }
    awaitString(fut)
  }

  override def getAll(maxResults: Int): String = {
    trace(s"getAll(maxResults = $maxResults)")
    val fut = companion.getAll(maxResults, withVsn = true)
      .map { r =>
        val resultNonPretty = EsModelUtil.toEsJsonDocs(r)
        JacksonWrapper.prettify(resultNonPretty)
      }
    awaitString(fut)
  }

  override def esTypeName: String = companion.ES_TYPE_NAME
  override def esIndexName: String = companion.ES_INDEX_NAME

  override def countAll(): String = {
    val fut = companion.countAll
      .map { _.toString }
    awaitString(fut)
  }
}

