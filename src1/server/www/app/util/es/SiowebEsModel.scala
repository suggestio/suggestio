package util.es

import javax.inject.Inject
import io.suggest.es.model.{CopyContentResult, EsModelCommonStaticT, EsModelUtil}
import io.suggest.es.util.{EsClientUtil, SioEsUtil}
import io.suggest.model.n2.media.MMedias
import io.suggest.model.n2.node.MNodes
import io.suggest.sec.m.MAsymKeys
import io.suggest.stat.m.MStats
import io.suggest.util.JMXBase
import io.suggest.util.logs.MacroLogsImplLazy
import models.adv.MExtTargets
import models.ai.MAiMads
import models.event.MEvents
import models.mcal.MCalendars
import models.mproj.ICommonDi
import models.usr.{EmailActivations, EmailPwIdents, MExtIdents}
import org.elasticsearch.common.transport.{InetSocketTransportAddress, TransportAddress}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
  mAiMads             : MAiMads,
  emailPwIdents       : EmailPwIdents,
  emailActivations    : EmailActivations,
  mExtIdents          : MExtIdents,
  mAsymKeys           : MAsymKeys,
  mStats              : MStats,
  mCommonDi           : ICommonDi
)
  extends MacroLogsImplLazy
{

  import mCommonDi._
  import LOGGER._

  // Constructor
  initializeEsModels()


  /**
   * Список моделей, которые должны быть проинициалированы при старте.
   *
   * @return Список EsModelMinimalStaticT.
   */
  def ES_MODELS = Seq[EsModelCommonStaticT](
    mNodes,
    emailPwIdents, emailActivations, mExtIdents,
    mCalendars,
    mAiMads,
    mExtTargets,
    mEvents,
    mAsymKeys,
    mMedias,
    mStats // Модель живёт в скользящих индексах, но это наверное безопасно...
  )

  /** Вернуть экзепшен, если есть какие-то проблемы при обработке ES-моделей. */
  def maybeErrorIfIncorrectModels() {
    if (configuration.getOptional[Boolean]("es.mapping.model.conflict.check.enabled").getOrElse(true))
      EsModelUtil.errorIfIncorrectModels(ES_MODELS)
  }

  /** Отправить маппинги всех моделей в хранилище. */
  def putAllMappings(models: Seq[EsModelCommonStaticT] = ES_MODELS): Future[Boolean] = {
    val ignoreExist = configuration.getOptional[Boolean]("es.mapping.model.ignore_exist")
      .contains(true)    // .getOrElse(false)
    trace("putAllMappings(): ignoreExists = " + ignoreExist)
    EsModelUtil.putAllMappings(models, ignoreExist)
  }


  /** Запуск импорта данных ES-моделей из удалённого источника (es-кластера) в текущий.
    * Для подключения к стороннему кластеру будет использоваться transport client, не подключающийся к кластеру. */
  def importModelsFromRemote(addrs: Seq[TransportAddress], esModels: Seq[EsModelCommonStaticT] = ES_MODELS): Future[CopyContentResult] = {
    val logPrefix = "importModelsFromRemote():"
    val esModelsCount = esModels.size
    trace(s"$logPrefix starting for $esModelsCount models: ${esModels.map(_.getClass.getSimpleName).mkString(", ")}")
    val fromClient = try {
      SioEsUtil.newTransportClient(addrs, clusterName = None)
    } catch {
      case ex: Throwable =>
        error(s"Failed to create transport client: addrs=$addrs", ex)
        throw ex
    }
    val toClient = mCommonDi.esClient
    val resultFut = Future.traverse(esModels) { esModel =>
      val copyResultFut = esModel.copyContent(fromClient, toClient)
      copyResultFut.onComplete {
        case Success(result) =>
          info(s"$logPrefix Copy finished for model ${esModel.getClass.getSimpleName}. Total success=${result.success} failed=${result.failed}")
        case Failure(ex) =>
          error(s"$logPrefix Copy failed for model ${esModel.getClass.getSimpleName}", ex)
      }
      copyResultFut
    }
    for (results <- resultFut) yield {
      val result = CopyContentResult(
        success = results.iterator.map(_.success).sum,
        failed  = results.iterator.map(_.failed).sum
      )
      import result._
      info(s"$logPrefix Copy of all $esModelsCount es-models finished. Total=${success + failed} success=$success failed=$failed")
      result
    }
  }


  /**
   * Проинициализировать все ES-модели и основной индекс.
   * @param triedIndexUpdate Флаг того, была ли уже попытка обновления индекса на последнюю версию.
   */
  def initializeEsModels(triedIndexUpdate: Boolean = false): Future[_] = {
    maybeErrorIfIncorrectModels()
    val esModels = ES_MODELS
    val futInx = EsModelUtil.ensureEsModelsIndices(esModels)
    val logPrefix = "initializeEsModels(): "
    futInx.onComplete {
      case Success(result) => debug(logPrefix + "ensure() -> " + result)
      case Failure(ex)     => error(logPrefix + "ensureIndex() failed", ex)
    }
    val futMappings = futInx.flatMap { _ =>
      putAllMappings(esModels)
    }
    futMappings.onComplete {
      case Success(_)  => info(logPrefix + "Finishied successfully.")
      case Failure(ex) => error(logPrefix + "Failure", ex)
    }
    // Это код обновления на следующую версию. Его можно держать и после обновления.
    /*
    import org.elasticsearch.index.mapper.MapperException
    futMappings.recoverWith {
      case ex: MapperException if !triedIndexUpdate =>
        info("Trying to update main index to v2.1 settings...")
        SioEsUtil.updateIndex2_1To2_2(EsModelUtil.DFLT_INDEX) flatMap { _ =>
          initializeEsModels(triedIndexUpdate = true)
        }
    }
    */
    futMappings
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
  with MacroLogsImplLazy
{

  import LOGGER._

  override def jmxName = "io.suggest:type=elasticsearch,name=" + getClass.getSimpleName.replace("Jmx", "")

  /** Импорт может затянуться, несмотря на все ускорения. Увеличиваем таймаут до получения результата. */
  override def futureTimeout = 5.minutes

  override def importModelFromRemote(modelStr: String, remotes: String): String = {
    val modelStr1 = modelStr.trim
    val model = siowebEsModel.ES_MODELS
      .find( _.getClass.getSimpleName.equalsIgnoreCase(modelStr1) )
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
        val sockAddr = EsClientUtil.parseHostPortStr(hostPortStr)
        new InetSocketTransportAddress(sockAddr)
      }
      .toSeq
    for (result <- siowebEsModel.importModelsFromRemote(addrs, models)) yield {
      import result._
      s"Total=${success + failed} success=$success failed=$failed"
    }
  }

}

