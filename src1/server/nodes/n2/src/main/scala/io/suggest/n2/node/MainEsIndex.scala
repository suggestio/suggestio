package io.suggest.n2.node

import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.es.MappingDsl
import io.suggest.es.model.{EsModel, EsModelConfig}
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImpl
import org.apache.lucene.index.IndexNotFoundException
import org.elasticsearch.index.reindex.BulkByScrollResponse
import play.api.Configuration
import play.api.inject.Injector
import org.elasticsearch.{Version => EsVersion}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object MainEsIndex {

  private def _MAIN_INDEX_NAME_PREFIX = "main-es"

  /** Internal index/mapping schema version.
    * Causes full index re-create and reindex if changed.
    * Incremented (changed) when dynamic mapping not enought to update, and it needs fresh index for data.
    */
  private def SCHEMA_VERSION = 1

  private def _mkMainIndexName(esMajorVsnOffset: Int = 0, schemaVsn: Option[Int] = None): String = {
    val sb = new StringBuilder(16, _MAIN_INDEX_NAME_PREFIX)
      .append( EsVersion.CURRENT.major + esMajorVsnOffset )

    for (schemaVsnValue <- schemaVsn)
      sb.append("-schema")
        .append(schemaVsnValue)

    sb.toString()
  }

  /** Alias name of current index. Alias name goes without schema version. */
  private val CURR_INDEX_ALIAS = _mkMainIndexName()

  /** Get current sio.main-index name.
    * During index upgrade, it changes.
    *
    * @return Currently in-use index name.
    */
  def getMainIndexName(): String = CURR_INDEX_ALIAS

}


/** Injected utilities for accessing/maintaining sio.main ElasticSearch index.
  * Primarily created for index upgrading between major ES updates.
  */
final class MainEsIndex @Inject()(
                                       injector: Injector,
                                     )
  extends MacroLogsImpl
{

  private lazy val configuration = injector.instanceOf[Configuration]
  private implicit lazy val ec = injector.instanceOf[ExecutionContext]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val esModelConfig = injector.instanceOf[EsModelConfig]

  private lazy val SHARDS_COUNT = configuration
    .getOptional[Int]("es.index.main.shards")
    .getOrElse( 5 )

  private lazy val REPLICAS_COUNT = configuration
    .getOptional[Int]("es.index.main.replicas")
    .getOrElse( esModelConfig.ES_INDEX_REPLICAS_COUNT )


  /** Old index or old index alias name. Alias, because name without schema version. */
  lazy val PREV_ES_VSN_INDEX_NAME: String = MainEsIndex._mkMainIndexName( -1, schemaVsn = None )

  /** Full index name of current version with schema version. */
  val CURR_INDEX_NAME = MainEsIndex._mkMainIndexName( schemaVsn = Some( MainEsIndex.SCHEMA_VERSION ) )


  /** Detect valid old index name. */
  lazy val OLD_INDEX_NAME: Future[String] = {
    def __check(indexName: String): Future[String] = {
      isMainIndexExists( indexName )
        .filter(identity)
        .map { _ => indexName }
    }

    lazy val logPrefix = "OLD_INDEX_NAME:"
    val currEsAliasName = MainEsIndex.CURR_INDEX_ALIAS
    LOGGER.trace( s"$logPrefix Try current ES major index alias: $currEsAliasName ..." )
    __check( currEsAliasName )
      .flatMap { currAliasName =>
        // Exclude current fresh index name from alias:
        for {
          aliasedIndexNames <- esModel.getAliasedIndexName( currAliasName )
          if {
            val r = !(aliasedIndexNames contains CURR_INDEX_NAME)
            if (!r) {
              LOGGER.debug(s"$logPrefix Index alias[$currAliasName] points to current index $CURR_INDEX_NAME")
              // IllegalStateException used to bypass all recoverWith NSEE-checks below.
              throw new IllegalStateException("Curr.index alias matching to current expected index name. Nothing to do here.")
            }
            r
          }
        } yield {
          LOGGER.trace(s"$logPrefix Curr.index alias[$currAliasName] => [${aliasedIndexNames.mkString(" ")}]")
          currAliasName
        }
      }
      .recoverWith { case _: NoSuchElementException =>
        val prevEsMajorIndexName = PREV_ES_VSN_INDEX_NAME
        LOGGER.trace( s"$logPrefix Try prev.major ES index name: $prevEsMajorIndexName ..." )
        __check( prevEsMajorIndexName )
      }
      .recoverWith { case _: NoSuchElementException =>
        // Delete this recoverWith code after upgrade from old naming scheme.
        val defaultIndexName = "sio.main.v7"
        LOGGER.trace( s"$logPrefix Try plain-old ES index name: $defaultIndexName ..." )
        __check( defaultIndexName )
      }
  }


  def deleteOldIndex(): Future[Boolean] = {
    def logPrefix = "deleteOldIndex():"

    (for {
      oldIndexName <- OLD_INDEX_NAME
      indexNamesUnaliased <- esModel.getAliasedIndexName( oldIndexName )
      indexNamesForDelete = {
        if (indexNamesUnaliased.isEmpty) {
          // No aliases. Old index (non-schema-versioned) is here.
          oldIndexName :: Nil
        } else {
          // Old index name(s) found. Delete old such indices using its real names.
          indexNamesUnaliased.toList
        }
      }
      r <- esModel.deleteIndex( indexNamesForDelete: _* )
    } yield {
      LOGGER.info( s"$logPrefix Deleted index $oldIndexName ${r.toString}" )
      true
    })
      .recover {
        case ex: IndexNotFoundException =>
          LOGGER.warn( s"$logPrefix Index not found", ex )
          false
      }
  }


  /** Detect, if sio-main index exist with given name.
    *
    * @param indexName Expected name of sio.main-index.
    * @return true, if sio.main index exist at given name.
    */
  def isMainIndexExists(indexName: String): Future[Boolean] = {
    lazy val logPrefix = s"isMainIndexExists($indexName):"
    LOGGER.trace(s"$logPrefix Looking up for old index '$indexName' ...")
    esModel
      .getIndexMeta( indexName )
      .map { oldIndexMetaOpt =>
        val r = oldIndexMetaOpt.exists { oldIndexMeta =>
          !oldIndexMeta.getMappings.isEmpty
        }

        if (r)
          LOGGER.trace(s"$logPrefix Found old index $indexName with mapping/data")
        else if (oldIndexMetaOpt.nonEmpty)
          LOGGER.warn(s"$logPrefix Main index '$indexName' is empty/uninitialized, this is unexpected")
        else
          LOGGER.trace(s"$logPrefix Index not exists: $indexName")

        r
      }
  }


  /** Start main index ensuring/initialization.
    * @param force Ignore configuration.
    * @return true, if index has been created now.
    */
  def doInit(force: Boolean = false)(implicit dsl: MappingDsl): Future[Boolean] = {
    lazy val loggerInst = LOGGER

    // Check about currently used settings.
    // is old sio-main index upgrade enabled?
    val isUpdateOldIndex = force || configuration
      .getOptional[Boolean]("es.index.main.migrate.enabled")
      .getOrElseTrue

    // Return isIndexCreated result.
    if (!isUpdateOldIndex) {
      loggerInst.trace("Old sio-main index migrate is disabled in config.")
      Future.successful( false )

    } else {
      // Ensure, if current sio-main index exists:
      esModel.ensureIndex( CURR_INDEX_NAME, getIndexSettings() )
    }
  }


  def doReindex(): Future[BulkByScrollResponse] = {
    val currentIndexName = CURR_INDEX_NAME

    lazy val logPrefix = s"doReindex(=> $currentIndexName):"
    LOGGER.debug(s"$logPrefix Starting.")

    for {
      oldIndexName <- OLD_INDEX_NAME

      res <- {
        // Found old sio-main index. Let's copy data to new main index...
        LOGGER.info(s"$logPrefix Will migrate sio-main index data from old=$oldIndexName to new=$currentIndexName")

        esModel.reindexData(
          fromIndex = oldIndexName,
          toIndex   = currentIndexName,
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

      // Reset index alias for current index. Alias name - without schema version, only major ES version.
      currIndexAlias = MainEsIndex.CURR_INDEX_ALIAS
      _ <- esModel.resetAliasToIndex(
        indexName = currentIndexName,
        aliasName = currIndexAlias ,
      )

    } yield {
      esModel.forceMerge( currentIndexName )
      LOGGER.info( s"$logPrefix Done, ${res.getTotal} docs reindexed. Old=$oldIndexName => $currentIndexName (alias: $currIndexAlias). ForceMerge in background" )
      res
    }
  }


  /**
    * Generate index settings using internal DSL.
    *
    * @return IndexSettings (internal dsl representation).
    */
  def getIndexSettings()(implicit dsl: MappingDsl): dsl.IndexSettings = {
    import dsl.{TokenCharTypes => TCT, _}
    import io.suggest.es.EsConstants._

    val chFilters = "html_strip" :: Nil
    val filters0 = List(WORD_DELIM_FN, LOWERCASE_FN)
    val filters1 = filters0 ++ List(STOP_EN_FN, STOP_RU_FN, STEM_RU_FN, STEM_EN_FN)

    // Without this settings, deprecation warning will be emitted: too much difference.
    val MAX_NGRAM_LEN = 10

    dsl.IndexSettings(
      shards = Some( SHARDS_COUNT ),
      replicas = Some( REPLICAS_COUNT ),
      maxNGramDiff = Some(MAX_NGRAM_LEN),
      analysis = IndexSettingsAnalysis(
        filters = Map(
          STOP_RU_FN -> Filter.stopWords("_russian_" :: Nil),
          STOP_EN_FN -> Filter.stopWords("_english_" :: Nil),
          WORD_DELIM_FN -> Filter.wordDelimiterGraph(preserveOriginal = true),
          STEM_RU_FN -> Filter.stemmer("russian"),
          STEM_EN_FN -> Filter.stemmer("english"),
          EDGE_NGRAM_FN_1 -> Filter.edgeNGram(minGram = 1, maxGram = MAX_NGRAM_LEN, side = "front"),
          //EDGE_NGRAM_FN_2   -> Filter.edgeNGram(minGram = 2, maxGram = 10, side = "front"),
        ),
        tokenizers = Map(
          // v2.0
          STD_TN -> Tokenizer.standard(),
          DEEP_NGRAM_TN -> Tokenizer.nGram(
            minGram = 1,
            maxGram = 10,
            tokenChars = TCT.Digit :: TCT.Letter :: Nil,
          ),
          // v2.2
          KEYWORD_TOKENIZER -> Tokenizer.keyWord(),
        ),
        analyzers = Map(
          // v2.0
          DEFAULT_ANALYZER -> Analyzer.custom(
            charFilters = chFilters,
            tokenizer = STD_TN,
            filters = filters1,
          ),
          ENGRAM_1LETTER_ANALYZER -> Analyzer.custom(
            charFilters = chFilters,
            tokenizer = STD_TN,
            filters = filters1 :+ EDGE_NGRAM_FN_1,
          ),
          /*ENGRAM_AN_2 -> Analyzer.custom(
            charFilters = chFilters,
            tokenizer   = STD_TN,
            filters     = filters1 ++ List(EDGE_NGRAM_FN_2),
          ),*/
          MINIMAL_ANALYZER -> Analyzer.custom(
            tokenizer = STD_TN,
            filters = filters0,
          ),
          // v2.1 Поддержка deep-ngram analyzer'а.
          /*DEEP_NGRAM_AN -> Analyzer.custom(
            tokenizer = DEEP_NGRAM_TN,
          )*/
          // v2.2
          KEYWORD_LOWERCASE_ANALYZER -> Analyzer.custom(
            tokenizer = KEYWORD_TOKENIZER,
            filters = LOWERCASE_FN :: Nil,
          ),
          /*FTS_NOSTOP_AN -> Analyzer.custom(
            tokenizer = STD_TN,
            filters   = WORD_DELIM_FN :: LOWERCASE_FN :: STEM_RU_FN :: STEM_EN_FN :: Nil,
          ),*/
          /*ENGRAM1_NOSTOP_AN -> Analyzer.custom(
            tokenizer = STD_TN,
            filters   = WORD_DELIM_FN :: LOWERCASE_FN :: STEM_RU_FN :: STEM_EN_FN :: Nil,
          )*/
        ),
      )
    )
  }

}


sealed trait MainEsIndexJmxMBean {
  def doInit(force: Boolean): String
  def doReindex(): String
  def deleteOldIndex(): String
}

final class MainEsIndexJmx @Inject()(
                                      injector: Injector,
                                    )
  extends JmxBase
  with MainEsIndexJmxMBean
{

  override def _jmxType = JmxBase.Types.ELASTICSEARCH

  private def mainEsIndex = injector.instanceOf[MainEsIndex]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  override def doInit(force: Boolean): String = {
    val fut = for {
      r <- mainEsIndex.doInit(force)( MappingDsl.Implicits.mkNewDsl )
    } yield {
      "Done, " + r
    }
    JmxBase.awaitString( fut )
  }


  override def doReindex(): String = {
    val fut = for {
      r <- mainEsIndex.doReindex()
    } yield {
      s"Done, total ${r.getTotal} documents"
    }
    JmxBase.awaitString( fut )
  }

  override def deleteOldIndex(): String = {
    val fut = mainEsIndex
      .deleteOldIndex()
      .map { isDeleted =>
        s"isDeleted?$isDeleted"
      }
    JmxBase.awaitString( fut )
  }

}
