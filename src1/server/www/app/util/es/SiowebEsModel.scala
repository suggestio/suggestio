package util.es

import javax.inject.Inject
import io.suggest.es.model.{CopyContentResult, EsModel}
import io.suggest.es.util.{EsClientUtil, IEsClient, TransportEsClient}
import io.suggest.n2.node.{MNodes, MainEsIndex}
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

  private def configuration = injector.instanceOf[Configuration]
  implicit private def ec = injector.instanceOf[ExecutionContext]
  private def esModel = injector.instanceOf[EsModel]
  private def mainEsIndex = injector.instanceOf[MainEsIndex]
  private def mNodes = injector.instanceOf[MNodes]


  // Устанавливать ES-mapping'и сразу при запуске? [true]
  {
    val ck = "es.mapping.install_on_start"
    val r = configuration.getOptional[Boolean](ck).getOrElseTrue
    if (r) initializeEsModels()
    else LOGGER.warn(s"NOT installing ES-mappings, because $ck = $r")
  }


  /** Запуск импорта данных ES-моделей из удалённого источника (es-кластера) в текущий.
    * Для подключения к стороннему кластеру будет использоваться transport client, не подключающийся к кластеру. */
  def importModelsFromRemote(addrs: Seq[TransportAddress]): Future[CopyContentResult] = {
    val _esModel = esModel
    import _esModel.api._

    val logPrefix = s"importModelsFromRemote(${addrs.mkString(" ")}):"
    LOGGER.trace(s"$logPrefix starting")

    val fromClient = try {
      injector
        .instanceOf[TransportEsClient]
        .newTransportClient( addrs, clusterName = None )
    } catch {
      case ex: Throwable =>
        LOGGER.error(s"$logPrefix Failed to create transport client: addrs=$addrs", ex)
        throw ex
    }

    val toClient = injector.instanceOf[IEsClient].esClient

    val model = mNodes
    model
      .copyContent(fromClient, toClient)
      .andThen {
        case Success(result) =>
          LOGGER.info(s"$logPrefix Copy finished for model ${model.getClass.getSimpleName}. Total success=${result.success} failed=${result.failed}")
        case Failure(ex) =>
          LOGGER.error(s"$logPrefix Copy failed for model ${model.getClass.getSimpleName}", ex)
      }
  }


  /**
   * Проинициализировать все ES-модели и основной индекс.
   * @param triedIndexUpdate Флаг того, была ли уже попытка обновления индекса на последнюю версию.
   */
  def initializeEsModels(triedIndexUpdate: Boolean = false): Future[_] = {
    val _esModel = esModel
    import _esModel.api._

    val startedAtMs = System.currentTimeMillis()
    lazy val logPrefix = s"initializeEsModels($triedIndexUpdate):"

    implicit val dsl = MappingDsl.Implicits.mkNewDsl

    val _sioMainEsIndex = mainEsIndex
    for {
      // Do main index initialization:
      isIndexCreated <- _sioMainEsIndex.doInit()

      // Do needed mappings initializations:
      _ <- {
        val processDataFut = for {
          _ <- mNodes.putMapping(
            indexName = _sioMainEsIndex.CURR_INDEX_NAME,
          )
          _ <- {
            if (isIndexCreated)
              _sioMainEsIndex.doReindex()
            else
              Future.successful(())
          }
        } yield {
          isIndexCreated
        }

        if (isIndexCreated) {
          for (_ <- processDataFut.failed) {
            val createdIndexName = _sioMainEsIndex.CURR_INDEX_NAME
            LOGGER.warn(s"$logPrefix Failed to init es models. Will delete newly-created index $createdIndexName")
            esModel
              .deleteIndex( createdIndexName )
              .recover { case deleteEx =>
                LOGGER.warn(s"$logPrefix Cannot delete newly created index $createdIndexName", deleteEx)
                false
              }
          }
        }

        processDataFut
      }

    } yield {
      LOGGER.trace(s"$logPrefix Done, took ${System.currentTimeMillis() - startedAtMs}ms")
    }
  }

}


/** Интерфейс для JMX-бина.  */
trait SiowebEsModelJmxMBean {
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

  override def importModelsFromRemote(remotes: String): String = {
    val fut = _importModelsFromRemote( remotes )
    for (ex <- fut.failed) {
      LOGGER.error(s"importModelsFromRemote($remotes): Failed", ex)
    }
    awaitString(fut)
  }

  protected def _importModelsFromRemote(remotes: String): Future[String] = {
    val addrs = remotes
      .split("[\\s,]+")
      .iterator
      .map { hostPortStr =>
        val sockAddr = EsClientUtil.parseHostPortStr(hostPortStr)
        new TransportAddress(sockAddr)
      }
      .toSeq
    for {
      result <- siowebEsModel.importModelsFromRemote( addrs )
    } yield {
      import result._
      s"Total=${success + failed} success=$success failed=$failed"
    }
  }

}

