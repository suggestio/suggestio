package util.es

import javax.inject.Inject
import io.suggest.es.model.{CopyContentResult, EsModel, EsModelCommonStaticT}
import io.suggest.es.util.{EsClientUtil, IEsClient, SioEsUtil}
import io.suggest.model.n2.media.MMedias
import io.suggest.model.n2.node.MNodes
import io.suggest.sec.m.MAsymKeys
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImplLazy
import models.adv.MExtTargets
import models.ai.MAiMads
import models.mcal.MCalendars
import org.elasticsearch.common.transport.{InetSocketTransportAddress, TransportAddress}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:43
 * Description: Дополнительная утиль для ES-моделей.
 */
class SiowebEsModel @Inject() (
                                esModel             : EsModel,
                                mNodes              : MNodes,
                                mMedias             : MMedias,
                                mCalendars          : MCalendars,
                                mExtTargets         : MExtTargets,
                                mAiMads             : MAiMads,
                                mAsymKeys           : MAsymKeys,
                                configuration             : Configuration,
                                implicit private val ec   : ExecutionContext,
                                esClientP                 : IEsClient,
                              )
  extends MacroLogsImplLazy
{

  import esModel.api._

  // Constructor
  initializeEsModels()


  /**
   * Список моделей, которые должны быть проинициалированы при старте.
   *
   * @return Список EsModelMinimalStaticT.
   */
  def ES_MODELS = Seq[EsModelCommonStaticT](
    mNodes,
    mCalendars,
    mAiMads,
    mExtTargets,
    mAsymKeys,
    mMedias,
  )

  /** Вернуть экзепшен, если есть какие-то проблемы при обработке ES-моделей. */
  def maybeErrorIfIncorrectModels() {
    if (configuration.getOptional[Boolean]("es.mapping.model.conflict.check.enabled").getOrElseTrue)
      esModel.errorIfIncorrectModels(ES_MODELS)
  }

  /** Отправить маппинги всех моделей в хранилище. */
  def putAllMappings(models: Seq[EsModelCommonStaticT] = ES_MODELS): Future[Boolean] = {
    val ignoreExist = configuration.getOptional[Boolean]("es.mapping.model.ignore_exist")
      .getOrElseFalse
    LOGGER.trace("putAllMappings(): ignoreExists = " + ignoreExist)
    esModel.putAllMappings(models, ignoreExist)
  }


  /** Запуск импорта данных ES-моделей из удалённого источника (es-кластера) в текущий.
    * Для подключения к стороннему кластеру будет использоваться transport client, не подключающийся к кластеру. */
  def importModelsFromRemote(addrs: Seq[TransportAddress], esModels: Seq[EsModelCommonStaticT] = ES_MODELS): Future[CopyContentResult] = {
    val logPrefix = "importModelsFromRemote():"
    val esModelsCount = esModels.size
    LOGGER.trace(s"$logPrefix starting for $esModelsCount models: ${esModels.map(_.getClass.getSimpleName).mkString(", ")}")
    val fromClient = try {
      SioEsUtil.newTransportClient(addrs, clusterName = None)
    } catch {
      case ex: Throwable =>
        LOGGER.error(s"Failed to create transport client: addrs=$addrs", ex)
        throw ex
    }
    val toClient = esClientP.esClient
    val resultFut = Future.traverse(esModels) { esM =>
      val copyResultFut = esM.copyContent(fromClient, toClient)
      copyResultFut.onComplete {
        case Success(result) =>
          LOGGER.info(s"$logPrefix Copy finished for model ${esM.getClass.getSimpleName}. Total success=${result.success} failed=${result.failed}")
        case Failure(ex) =>
          LOGGER.error(s"$logPrefix Copy failed for model ${esM.getClass.getSimpleName}", ex)
      }
      copyResultFut
    }
    for (results <- resultFut) yield {
      val result = CopyContentResult(
        success = results.iterator.map(_.success).sum,
        failed  = results.iterator.map(_.failed).sum
      )
      import result._
      LOGGER.info(s"$logPrefix Copy of all $esModelsCount es-models finished. Total=${success + failed} success=$success failed=$failed")
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
    val futInx = esModel.ensureEsModelsIndices(esModels)
    val logPrefix = "initializeEsModels(): "
    futInx.onComplete {
      case Success(result) => LOGGER.trace(s"$logPrefix ensure() -> $result")
      case Failure(ex)     => LOGGER.error(s"$logPrefix ensureIndex() failed", ex)
    }
    val futMappings = futInx.flatMap { _ =>
      putAllMappings(esModels)
    }
    futMappings.onComplete {
      case Success(_)  => LOGGER.info(s"$logPrefix Finishied successfully.")
      case Failure(ex) => LOGGER.error(s"$logPrefix Failure", ex)
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
                                         implicit val ec         : ExecutionContext
                                       )
  extends JmxBase
  with SiowebEsModelJmxMBean
  with MacroLogsImplLazy
{

  import JmxBase._

  override def _jmxType = Types.ELASTICSEARCH

  override def importModelFromRemote(modelStr: String, remotes: String): String = {
    val modelStr1 = modelStr.trim
    val model = siowebEsModel.ES_MODELS
      .find( _.getClass.getSimpleName.equalsIgnoreCase(modelStr1) )
      .get
    val fut = _importModelsFromRemote(remotes, Seq(model))
    for (ex <- fut.failed) {
      LOGGER.error(s"importModelsFromRemote($modelStr, $remotes): Failed", ex)
    }
    awaitString(fut)
  }

  override def importModelsFromRemote(remotes: String): String = {
    val fut = _importModelsFromRemote(remotes, siowebEsModel.ES_MODELS)
    for (ex <- fut.failed) {
      LOGGER.error(s"importModelsFromRemote($remotes): Failed", ex)
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

