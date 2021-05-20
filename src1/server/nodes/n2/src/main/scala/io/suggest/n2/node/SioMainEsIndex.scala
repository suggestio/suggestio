package io.suggest.n2.node

import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.es.MappingDsl
import io.suggest.es.model.{EsModel, EsModelUtil}
import io.suggest.es.search.EsTypesFilter
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsDyn
import org.apache.lucene.index.IndexNotFoundException
import play.api.Configuration
import play.api.inject.Injector
import org.elasticsearch.{Version => EsVersion}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


/** Injected utilities for accessing/maintaining sio.main ElasticSearch index.
  * Primarily created for index upgrading between major ES updates.
  *
  * Singleton, because it is stateful, by now.
  */
@Singleton
final class SioMainEsIndex @Inject() (
                                       injector: Injector,
                                     )
  extends MacroLogsDyn
{

  private def _MAIN_INDEX_NAME_PREFIX = "sio.main.v"

  private def _mkMainIndexName(esMajorVsnOffset: Int = 0): String =
    _MAIN_INDEX_NAME_PREFIX + (EsVersion.CURRENT.major + esMajorVsnOffset)

  def OLD_INDEX_NAME: String = _mkMainIndexName( -1 )

  def CURR_INDEX_NAME: String = _mkMainIndexName()


  private var _currIndexName: String = CURR_INDEX_NAME


  /** Get current sio.main-index name.
    * During index upgrade, it changes.
    *
    * @return Currently in-use index name.
    */
  def getMainIndexName(): String = _currIndexName


  def deleteOldIndex(): Future[Boolean] = {
    implicit val esModel = injector.instanceOf[EsModel]
    implicit val ec = injector.instanceOf[ExecutionContext]

    val oldIndexName = OLD_INDEX_NAME
    esModel
      .deleteIndex( oldIndexName )
      .transform {
        case Success( r ) =>
          LOGGER.info( s"Deleted index $oldIndexName ${r.toString}" )
          Success( true )
        case Failure(ex: IndexNotFoundException) =>
          LOGGER.warn( "Index not found\n", ex )
          Success( false )
        case fail @ Failure(_) =>
          fail.asInstanceOf[Try[Boolean]]
      }
  }


  /** Start main index ensuring/initialization. */
  def doInit(force: Boolean = false)(implicit dsl: MappingDsl): Future[_] = {

    lazy val configuration = injector.instanceOf[Configuration]
    implicit val ec = injector.instanceOf[ExecutionContext]
    implicit lazy val esModel = injector.instanceOf[EsModel]

    lazy val loggerInst = LOGGER

    /** Detect, if sio-main index exist with given name.
      *
      * @param indexName Expected name of sio.main-index.
      * @return true, if sio.main index exist at given name.
      */
    def isMainIndexExists(indexName: String): Future[Boolean] = {
      lazy val logPrefix = s"isMainIndexExists($indexName):"
      loggerInst.trace(s"$logPrefix Looking up for old index '$indexName' ...")
      esModel
        .getIndexMeta( indexName )
        .map { oldIndexMetaOpt =>
          val r = oldIndexMetaOpt.exists { oldIndexMeta =>
            !oldIndexMeta.getMappings.isEmpty
          }

          if (r)
            loggerInst.trace(s"$logPrefix Found old index $indexName with mapping/data")
          else if (oldIndexMetaOpt.nonEmpty)
            loggerInst.warn(s"$logPrefix Main index '$indexName' is empty/uninitialized, this is unexpected")
          else
            loggerInst.trace(s"$logPrefix Index not exists: $indexName")

          r
        }
    }

    // Check about currently used settings.
    // is old sio-main index upgrade enabled?
    val isUpdateOldIndex = force || configuration
      .getOptional[Boolean]("es.index.main.migrate.enabled")
      .getOrElseTrue

    if (!isUpdateOldIndex) {
      loggerInst.trace("Old sio-main index migrate is disabled in config.")
      Future.successful()

    } else {
      // Check if current sio-main index exists:
      val currentIndexName = CURR_INDEX_NAME

      // Ensure, if current sio-main index exists:
      val isCurrentIndexHasBeenCreatedFut = esModel.ensureSioMainIndex(
        indexName = currentIndexName,
        shards    = EsModelUtil.SHARDS_COUNT_DFLT,
        replicas  = EsModelUtil.REPLICAS_COUNT_DFLT,
      )

      // Check if old sio-main index exists, in background:
      val oldIndexName = OLD_INDEX_NAME
      val isOldIndexExistsFut = isMainIndexExists( oldIndexName )

      for {
        _ <- isCurrentIndexHasBeenCreatedFut

        isOldIndexExists <- isOldIndexExistsFut
        if isOldIndexExists

        _ <- {
          // Found old sio-main index. Let's copy data to new main index...

          // Temporary reset current index to old index:
          _currIndexName = oldIndexName

          LOGGER.info(s"Will migrate sio-main index data from old=$oldIndexName to new=$currentIndexName")

          esModel.reindexData(
            fromIndex = oldIndexName,
            toIndex   = currentIndexName,
            // Copy only nodes from _type = "n2"
            filter    = {
              new EsTypesFilter {
                override def esTypes = MNodeFields.ES_TYPE_NAME :: Nil
              }
                .toEsQuery
            },
          )
        }

        _ <- {
          val isDeleteOldIndex = configuration
            .getOptional[Boolean]("es.index.main.old.autodelete")
            .getOrElseFalse

          if (isDeleteOldIndex)
            deleteOldIndex()
          else
            Future.successful(false)
        }

      } yield {
        // Permanently set main index name to current name:
        _currIndexName = currentIndexName
      }
    }
  }

}


sealed trait SioMainEsIndexJmxMBean {
  def doInit(force: Boolean): String
  def deleteOldIndex(): String
}

final class SioMainEsIndexJmx @Inject() (
                                          injector: Injector,
                                        )
  extends JmxBase
  with SioMainEsIndexJmxMBean
{

  override def _jmxType = JmxBase.Types.ELASTICSEARCH

  private def sioMainEsIndex = injector.instanceOf[SioMainEsIndex]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  override def doInit(force: Boolean): String = {
    val fut = for {
      r <- sioMainEsIndex.doInit(force)( MappingDsl.Implicits.mkNewDsl )
    } yield {
      "Done, " + r
    }
    JmxBase.awaitString( fut )
  }

  override def deleteOldIndex(): String = {
    val fut = sioMainEsIndex
      .deleteOldIndex()
      .map { isDeleted =>
        s"isDeleted?$isDeleted"
      }
    JmxBase.awaitString( fut )
  }

}
