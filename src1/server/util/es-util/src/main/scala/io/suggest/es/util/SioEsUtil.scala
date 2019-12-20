package io.suggest.es.util

import io.suggest.es.MappingDsl
import io.suggest.util.SioConstants._
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.index.mapper._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.03.13 16:58
 * Description: Функции для работы с ElasticSearch. В основном - функции генерации json-спек индексов.
 */

object SioEsUtil extends MacroLogsImpl {

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
  def STD_FN        = "fStd"
  def WORD_DELIM_FN = "fWordDelim"

  def STD_TN        = "tStd"
  def DEEP_NGRAM_TN = "deepNgramTn"

  /** Стандартные имена полей ES. */
  object StdFns {
    @deprecated("_all field is removed in ES-6.x", "ES-6.x")
    def FIELD_ALL           = AllFieldMapper.NAME
    def FIELD_SOURCE        = SourceFieldMapper.NAME
    def FIELD_ROUTING       = RoutingFieldMapper.NAME
    def FIELD_ID            = IdFieldMapper.NAME
    def FIELD_UID           = UidFieldMapper.NAME
    def FIELD_VERSION       = VersionFieldMapper.NAME
    def FIELD_PARENT        = ParentFieldMapper.NAME
    def FIELD_DOC           = "_doc"
  }

  /**
    * Версия документов, только что добавленных в elasticsearch.
    *
    * @see [[https://www.elastic.co/blog/elasticsearch-versioning-support]]
    *      That version number is a positive number between 1 and 2**63-1 (inclusive).
    */
  def DOC_VSN_0     = 1L


  /**
   * Сборка индекса по архитектуре второго поколения: без повсеместного edgeNgram, который жрёт как не в себя.
   * v2.1 добавляет кое-какой ngram analyzer для очень узконаправленных нужд.
    *
    * @param shards Кол-во шард.
   * @param replicas Кол-во реплик.
   * @return XCB с сеттингами.
   */
  def getIndexSettingsV2(shards: Int, replicas: Int = 1)(implicit dsl: MappingDsl): dsl.IndexSettings = {
    import dsl.{TokenCharTypes => TCT, _}

    val chFilters = "html_strip" :: Nil
    val filters0 = List(STD_FN, WORD_DELIM_FN, LOWERCASE_FN)
    val filters1 = filters0 ++ List(STOP_EN_FN, STOP_RU_FN, STEM_RU_FN, STEM_EN_FN)

    dsl.IndexSettings(
      shards = Some(shards),
      replicas = Some(replicas),
      analysis = IndexSettingsAnalysis(
        filters = Map(
          STD_FN            -> Filter.standard,
          LOWERCASE_FN      -> Filter.lowerCase,
          STOP_RU_FN        -> Filter.stopWords( "russian" ),
          STOP_EN_FN        -> Filter.stopWords( "english" ),
          WORD_DELIM_FN     -> Filter.wordDelimiter(preserveOriginal = true),
          STEM_RU_FN        -> Filter.stemmer( "russian" ),
          STEM_EN_FN        -> Filter.stemmer( "english" ),
          EDGE_NGRAM_FN_1   -> Filter.edgeNGram(minGram = 1, maxGram = 10, side = "front"),
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
            filters   = STD_FN :: WORD_DELIM_FN :: LOWERCASE_FN :: STEM_RU_FN :: STEM_EN_FN :: Nil,
          ),*/
          /*ENGRAM1_NOSTOP_AN -> Analyzer.custom(
            tokenizer = STD_TN,
            filters   = STD_FN :: WORD_DELIM_FN :: LOWERCASE_FN :: STEM_RU_FN :: STEM_EN_FN :: Nil,
          )*/
        ),
      )
    )
  }


}

