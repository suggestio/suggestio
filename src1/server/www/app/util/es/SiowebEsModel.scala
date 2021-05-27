package util.es

import javax.inject.Inject
import io.suggest.es.model.{CopyContentResult, EsModel, EsModelCommonStaticT}
import io.suggest.es.util.{EsClientUtil, IEsClient, TransportEsClient}
import io.suggest.n2.node.{MNodes, SioMainEsIndex}
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImplLazy
import org.elasticsearch.common.transport.TransportAddress
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.es.MappingDsl
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:43
 * Description: Дополнительная утиль для ES-моделей.
 */
final class SiowebEsModel @Inject() (
                                      injector                  : Injector,
                                    )
  extends MacroLogsImplLazy
{


  private val configuration = injector.instanceOf[Configuration]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val sioMainEsIndex = injector.instanceOf[SioMainEsIndex]


  // Устанавливать ES-mapping'и сразу при запуске? [true]
  {
    val ck = "es.mapping.install_on_start"
    val r = configuration.getOptional[Boolean](ck).getOrElseTrue
    if (r) initializeEsModels()
    else LOGGER.warn(s"NOT installing ES-mappings, because $ck = $r")
  }


  /**
   * Список моделей, которые должны быть проинициалированы при старте.
   *
   * @return Список EsModelMinimalStaticT.
   */
  def ES_MODELS: Seq[EsModelCommonStaticT] = {
    injector.instanceOf[MNodes] #::
    LazyList.empty
  }


  /** Отправить маппинги всех моделей в хранилище. */
  def putAllMappings(models: Seq[EsModelCommonStaticT])(implicit dsl: MappingDsl): Future[Boolean] = {
    lazy val logPrefix = s"putAllMappings(${models.length}):"
    val ignoreExist = configuration
      .getOptional[Boolean]("es.mapping.model.ignore_exist")
      .getOrElseFalse
    LOGGER.trace(s"$logPrefix ignoreExists = $ignoreExist\n models = [${models.mkString(", ")}]")

    val resFut = esModel.putAllMappings( models, ignoreExist )

    resFut.onComplete {
      case Success(_)  => LOGGER.info(s"$logPrefix Finishied successfully.")
      case Failure(ex) => LOGGER.error(s"$logPrefix Failure", ex)
    }

    resFut
  }


  /** Запуск импорта данных ES-моделей из удалённого источника (es-кластера) в текущий.
    * Для подключения к стороннему кластеру будет использоваться transport client, не подключающийся к кластеру. */
  def importModelsFromRemote(addrs: Seq[TransportAddress], esModels: Seq[EsModelCommonStaticT] = ES_MODELS): Future[CopyContentResult] = {
    import esModel.api._

    val logPrefix = "importModelsFromRemote():"
    val esModelsCount = esModels.size
    LOGGER.trace(s"$logPrefix starting for $esModelsCount models: ${esModels.map(_.getClass.getSimpleName).mkString(", ")}")

    val fromClient = try {
      injector
        .instanceOf[TransportEsClient]
        .newTransportClient( addrs, clusterName = None )
    } catch {
      case ex: Throwable =>
        LOGGER.error(s"Failed to create transport client: addrs=$addrs", ex)
        throw ex
    }

    val toClient = injector.instanceOf[IEsClient].esClient

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
    val startedAtMs = System.currentTimeMillis()

    val esModels = ES_MODELS
    lazy val logPrefix = s"initializeEsModels($triedIndexUpdate, [${esModels.length}]):"

    implicit val dsl = MappingDsl.Implicits.mkNewDsl

    for {
      // Do main index initialization:
      isIndexCreated <- sioMainEsIndex.doInit()
      // Do needed mappings initializations:
      _ <- putAllMappings( esModels )
      _ <- {
        if (isIndexCreated)
          sioMainEsIndex.doReindex()
        else
          Future.successful(())
      }

    } yield {
      LOGGER.trace(s"$logPrefix Done, took ${System.currentTimeMillis() - startedAtMs}ms")
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
                                         injector: Injector,
                                       )
  extends JmxBase
  with SiowebEsModelJmxMBean
  with MacroLogsImplLazy
{

  import JmxBase._

  private def siowebEsModel = injector.instanceOf[SiowebEsModel]
  implicit private def ec = injector.instanceOf[ExecutionContext]

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
    val addrs = remotes
      .split("[\\s,]+")
      .iterator
      .map { hostPortStr =>
        val sockAddr = EsClientUtil.parseHostPortStr(hostPortStr)
        new TransportAddress(sockAddr)
      }
      .toSeq
    for (result <- siowebEsModel.importModelsFromRemote(addrs, models)) yield {
      import result._
      s"Total=${success + failed} success=$success failed=$failed"
    }
  }

}

