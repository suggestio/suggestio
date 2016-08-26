package models

import java.net.InetAddress

import com.google.inject.Inject
import io.suggest.model.es.{CopyContentResult, EsModelCommonStaticT, EsModelUtil}
import io.suggest.model.n2.media.MMedias
import io.suggest.model.n2.node.MNodes
import io.suggest.util.{JMXBase, SioEsUtil}
import io.suggest.ym.model.stat.MAdStats
import models.adv.MExtTargets
import models.ai.MAiMads
import models.event.MEvents
import models.mcal.MCalendars
import models.merr.MRemoteErrors
import models.mproj.ICommonDi
import models.sec.MAsymKeys
import models.usr.{EmailActivations, EmailPwIdents, MExtIdents}
import org.elasticsearch.common.transport.{InetSocketTransportAddress, TransportAddress}
import util.{PlayLazyMacroLogsImpl, PlayMacroLogsDyn}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:43
 * Description: Дополнительная утиль для ES-моделей.
 */
class SiowebEsModel @Inject() (
  mNodes              : MNodes,
  mMedias             : MMedias,
  mCalendars          : MCalendars,
  mEvents             : MEvents,
  mExtTargets         : MExtTargets,
  mRemoteErrors       : MRemoteErrors,
  mAiMads             : MAiMads,
  mAdStats            : MAdStats,
  emailPwIdents       : EmailPwIdents,
  emailActivations    : EmailActivations,
  mExtIdents          : MExtIdents,
  mAsymKeys           : MAsymKeys,
  mCommonDi           : ICommonDi
)
  extends PlayMacroLogsDyn
{

  import mCommonDi._

  /**
   * Список моделей, которые должны быть проинициалированы при старте.
   *
   * @return Список EsModelMinimalStaticT.
   */
  def ES_MODELS = Seq[EsModelCommonStaticT](
    mNodes,
    emailPwIdents, emailActivations, mExtIdents,
    mCalendars,
    mRemoteErrors,
    mAiMads,
    mExtTargets,
    mEvents,
    mAsymKeys,
    mMedias,
    mAdStats
  )

  /** Вернуть экзепшен, если есть какие-то проблемы при обработке ES-моделей. */
  def maybeErrorIfIncorrectModels() {
    if (configuration.getBoolean("es.mapping.model.conflict.check.enabled") getOrElse true)
      EsModelUtil.errorIfIncorrectModels(ES_MODELS)
  }

  /** Отправить маппинги всех моделей в хранилище. */
  def putAllMappings(models: Seq[EsModelCommonStaticT] = ES_MODELS): Future[Boolean] = {
    val ignoreExist = configuration.getBoolean("es.mapping.model.ignore_exist") getOrElse false
    LOGGER.trace("putAllMappings(): ignoreExists = " + ignoreExist)
    EsModelUtil.putAllMappings(models, ignoreExist)
  }


  /** Запуск импорта данных ES-моделей из удалённого источника (es-кластера) в текущий.
    * Для подключения к стороннему кластеру будет использоваться transport client, не подключающийся к кластеру. */
  def importModelsFromRemote(addrs: Seq[TransportAddress], esModels: Seq[EsModelCommonStaticT] = ES_MODELS): Future[CopyContentResult] = {
    val logger = LOGGER
    val logPrefix = "importModelsFromRemote():"
    val esModelsCount = esModels.size
    logger.trace(s"$logPrefix starting for $esModelsCount models: ${esModels.map(_.getClass.getSimpleName).mkString(", ")}")
    val fromClient = try {
      SioEsUtil.newTransportClient(addrs, clusterName = None)
    } catch {
      case ex: Throwable =>
        logger.error(s"Failed to create transport client: addrs=$addrs", ex)
        throw ex
    }
    val toClient = mCommonDi.esClient
    val resultFut = Future.traverse(esModels) { esModel =>
      val copyResultFut = esModel.copyContent(fromClient, toClient)
      copyResultFut.onComplete {
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
final class SiowebEsModelJmx @Inject() (
  siowebEsModel           : SiowebEsModel,
  implicit private val ec : ExecutionContext
)
  extends JMXBase
  with SiowebEsModelJmxMBean
  with PlayLazyMacroLogsImpl
{

  import LOGGER._

  override def jmxName = "io.suggest:type=elasticsearch,name=" + getClass.getSimpleName.replace("Jmx", "")

  /** Импорт может затянуться, несмотря на все ускорения. Увеличиваем таймаут до получения результата. */
  override def futureTimeout = 5.minutes

  override def importModelFromRemote(modelStr: String, remotes: String): String = {
    val modelStr1 = modelStr.trim
    val model = siowebEsModel.ES_MODELS
      .find(_.getClass.getSimpleName equalsIgnoreCase modelStr1)
      .get
    val fut = _importModelsFromRemote(remotes, Seq(model))
    fut.onFailure { case ex =>
      error(s"importModelsFromRemote($modelStr, $remotes): Failed", ex)
    }
    awaitString(fut)
  }

  override def importModelsFromRemote(remotes: String): String = {
    val fut = _importModelsFromRemote(remotes, siowebEsModel.ES_MODELS)
    fut.onFailure { case ex =>
      error(s"importModelsFromRemote($remotes): Failed", ex)
    }
    awaitString(fut)
  }

  protected def _importModelsFromRemote(remotes: String, models: Seq[EsModelCommonStaticT]): Future[String] = {
    val addrs = remotes.split("[\\s,]+")
      .toIterator
      .map { hostPortStr =>
        val Array(host, portStr) = hostPortStr.split(':')
        val port = portStr.toInt
        val addr = InetAddress.getByName(host)
        new InetSocketTransportAddress(addr, port)
      }
      .toSeq
    for (result <- siowebEsModel.importModelsFromRemote(addrs, models)) yield {
      import result._
      s"Total=${success + failed} success=$success failed=$failed"
    }
  }

}

