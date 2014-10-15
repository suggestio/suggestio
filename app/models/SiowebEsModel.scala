package models

import io.suggest.model.{EsModelCommonStaticT, CopyContentResult, EsModel, EsModelStaticT}
import io.suggest.util.{JMXBase, SioEsUtil}
import org.elasticsearch.common.transport.{InetSocketTransportAddress, TransportAddress}
import util.SiowebEsUtil
import scala.concurrent.Future
import org.elasticsearch.client.Client
import play.api.Play.current
import org.slf4j.LoggerFactory
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:43
 * Description: Дополнительная утиль для ES-моделей.
 */
object SiowebEsModel {

  /**
   * Список моделей, которые должны быть проинициалированы при старте.
   * @return Список EsModelMinimalStaticT.
   */
  def ES_MODELS: Seq[EsModelCommonStaticT] = {
    EsModel.ES_MODELS ++ Seq(
      MBlog, MPerson, MozillaPersonaIdent, EmailPwIdent, EmailActivation, MMartCategory, MInviteRequest, MCalendar,
      MRemoteError
    )
  }

  def putAllMappings(implicit client: Client): Future[Boolean] = {
    val ignoreExist = current.configuration.getBoolean("es.mapping.model.ignore_exist") getOrElse false
    LoggerFactory.getLogger(getClass).trace("putAllMappings(): ignoreExists = " + ignoreExist)
    EsModel.putAllMappings(ES_MODELS, ignoreExist)
  }


  /** Запуск импорта данных ES-моделей из удалённого источника (es-кластера) в текущий.
    * Для подключения к стороннему кластеру будет использоваться transport client, не подключающийся к кластеру. */
  def importModelsFromRemote(addrs: Seq[TransportAddress], esModels: Seq[EsModelCommonStaticT] = ES_MODELS): Future[CopyContentResult] = {
    val logger = play.api.Logger(getClass)
    val logPrefix = "importModelsFromRemote():"
    val esModelsCount = esModels.size
    logger.trace(s"$logPrefix starting for $esModelsCount models: ${esModels.map(_.getClass.getSimpleName).mkString(", ")}")
    val fromClient = SioEsUtil.newTransportClient(addrs, clusterName = None)
    val toClient = SiowebEsUtil.client
    val resultFut = Future.traverse(esModels) { esModel =>
      val copyResultFut = esModel.copyContent(fromClient, toClient)
      copyResultFut onComplete {
        case Success(result) =>
          logger.info(s"$logPrefix Copy finished for model ${esModel.getClass.getSimpleName}. Total success=${result.success} failed=${result.failed}")
        case Failure(ex) =>
          logger.error(s"$logPrefix Copy failed for model ${esModel.getClass.getSimpleName}", ex)
      }
      copyResultFut
    }
    resultFut map { results =>
      val result = CopyContentResult(
        success = results.iterator.map(_.success).sum,
        failed  = results.iterator.map(_.failed).sum
      )
      import result._
      logger.info(s"$logPrefix Copy of all $esModelsCount es-models finished. Total=${success + failed} success=$success failed=$failed")
      result
    }
  }

}


/** Интерфейс для JMX-бина.  */
trait SiowebEsModelJmxMBean {
  def importModelFromRemote(modelStr: String, remotes: String): String
  def importModelsFromRemote(remotes: String): String
}

/** Реализация jmx-бина, открывающая доступ к функциям [[SiowebEsModel]]. */
final class SiowebEsModelJmx extends JMXBase with SiowebEsModelJmxMBean {
  override def jmxName = "io.suggest.model:type=elasticsearch,name=" + getClass.getSimpleName.replace("Jmx", "")

  /** Импорт может затянуться, несмотря на все ускорения. Увеличиваем таймаут до получения результата. */
  override def futureTimeout = 5 minutes

  override def importModelFromRemote(modelStr: String, remotes: String): String = {
    val modelStr1 = modelStr.trim
    val model = SiowebEsModel.ES_MODELS
      .find(_.getClass.getSimpleName equalsIgnoreCase modelStr1)
      .get
    _importModelsFromRemote(remotes, Seq(model))
  }

  override def importModelsFromRemote(remotes: String): String = {
    _importModelsFromRemote(remotes, SiowebEsModel.ES_MODELS)
  }

  protected def _importModelsFromRemote(remotes: String, models: Seq[EsModelCommonStaticT]) = {
    val addrs = remotes.split("[\\s,]+")
      .toIterator
      .map { hostPortStr =>
        val Array(host, portStr) = hostPortStr.split(':')
        val port = portStr.toInt
        new InetSocketTransportAddress(host, port)
      }
      .toSeq
    SiowebEsModel.importModelsFromRemote(addrs, models)
      .map { result =>
        import result._
        s"Total=${success + failed} success=$success failed=$failed"
      }
  }
}

