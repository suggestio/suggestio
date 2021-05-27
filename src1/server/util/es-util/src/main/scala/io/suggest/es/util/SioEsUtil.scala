package io.suggest.es.util

import io.suggest.es.MappingDsl
import io.suggest.util.SioConstants._
import org.elasticsearch.index.mapper._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.03.13 16:58
 * Description: Функции для работы с ElasticSearch. В основном - функции генерации json-спек индексов.
 */

object SioEsUtil {

  def ES_EXECUTE_WARN_IF_TAKES_TOO_LONG_MS = 1000

  // _FN - Filter Name. _AN - Analyzer Name, _TN - Tokenizer Name

  // Имена стеммеров
  def STEM_EN_FN    = "fStemEN"
  def STEM_RU_FN    = "fStemRU"

  // Имена stopwords-фильтров.
  def STOP_EN_FN    = "fStopEN"
  def STOP_RU_FN    = "fStopRU"

  //def EDGE_NGRAM_FN_2 = "fEdgeNgram2"
  def EDGE_NGRAM_FN_1 = "fEdgeNgram1"
  def LOWERCASE_FN  = "fLowercase"
  def WORD_DELIM_FN = "fWordDelim"

  def STD_TN        = "tStd"
  def DEEP_NGRAM_TN = "deepNgramTn"

  /** Стандартные имена полей ES. */
  object StandardFieldNames {
    def SOURCE        = SourceFieldMapper.NAME
    def ROUTING       = RoutingFieldMapper.NAME
    def ID            = IdFieldMapper.NAME
    def VERSION       = VersionFieldMapper.NAME
    def DOC           = "_doc"
  }

  /** Special use_field_mapping format tells Elasticsearch to use the format from the mapping.
    * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-request-docvalue-fields.html]]
    */
  final def USE_FIELD_MAPPING = "use_field_mapping"

  /**
    * Версия документов, только что добавленных в elasticsearch.
    *
    * @see [[https://www.elastic.co/blog/elasticsearch-versioning-support]]
    *      That version number is a positive number between 1 and 2**63-1 (inclusive).
    */
  final def DOC_VSN_0     = 1L

  final def REPLICAS_COUNT = 1

  /**
   * Сборка индекса по архитектуре второго поколения: без повсеместного edgeNgram, который жрёт как не в себя.
   * v2.1 добавляет кое-какой ngram analyzer для очень узконаправленных нужд.
    *
    * @param shards Кол-во шард.
   * @param replicas Кол-во реплик.
   * @return XCB с сеттингами.
   */
  def getIndexSettingsV2(shards: Int, replicas: Int = REPLICAS_COUNT)(implicit dsl: MappingDsl): dsl.IndexSettings = {
    import dsl.{TokenCharTypes => TCT, _}

    val chFilters = "html_strip" :: Nil
    val filters0 = List(WORD_DELIM_FN, LOWERCASE_FN)
    val filters1 = filters0 ++ List(STOP_EN_FN, STOP_RU_FN, STEM_RU_FN, STEM_EN_FN)

    // Without this settings, deprecation warning will be emitted: too much difference.
    val MAX_NGRAM_LEN = 10

    dsl.IndexSettings(
      shards = Some(shards),
      replicas = Some(replicas),
      maxNGramDiff = Some( MAX_NGRAM_LEN ),
      analysis = IndexSettingsAnalysis(
        filters = Map(
          LOWERCASE_FN      -> Filter.lowerCase,
          STOP_RU_FN        -> Filter.stopWords( "_russian_" :: Nil ),
          STOP_EN_FN        -> Filter.stopWords( "_english_" :: Nil ),
          WORD_DELIM_FN     -> Filter.wordDelimiter(preserveOriginal = true),
          STEM_RU_FN        -> Filter.stemmer( "russian" ),
          STEM_EN_FN        -> Filter.stemmer( "english" ),
          EDGE_NGRAM_FN_1   -> Filter.edgeNGram(minGram = 1, maxGram = MAX_NGRAM_LEN, side = "front"),
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
          KEYWORD_TN -> Tokenizer.keyWord(),
        ),
        analyzers = Map(
          // v2.0
          DFLT_AN -> Analyzer.custom(
            charFilters = chFilters,
            tokenizer   = STD_TN,
            filters     = filters1,
          ),
          ENGRAM_AN_1 -> Analyzer.custom(
            charFilters = chFilters,
            tokenizer = STD_TN,
            filters = filters1 :+ EDGE_NGRAM_FN_1,
          ),
          /*ENGRAM_AN_2 -> Analyzer.custom(
            charFilters = chFilters,
            tokenizer   = STD_TN,
            filters     = filters1 ++ List(EDGE_NGRAM_FN_2),
          ),*/
          MINIMAL_AN -> Analyzer.custom(
            tokenizer = STD_TN,
            filters   = filters0,
          ),
          // v2.1 Поддержка deep-ngram analyzer'а.
          /*DEEP_NGRAM_AN -> Analyzer.custom(
            tokenizer = DEEP_NGRAM_TN,
          )*/
          // v2.2
          KW_LC_AN -> Analyzer.custom(
            tokenizer = KEYWORD_TN,
            filters   = LOWERCASE_FN :: Nil,
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

