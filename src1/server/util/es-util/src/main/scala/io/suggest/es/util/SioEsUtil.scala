package io.suggest.es.util

import java.io.ByteArrayInputStream

import io.suggest.util.SioConstants._
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.action.{ActionListener, ActionRequestBuilder, ActionResponse}
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentType}
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import org.elasticsearch.index.mapper._
import org.elasticsearch.transport.client.PreBuiltTransportClient

import scala.concurrent.{ExecutionContext, Future, Promise}

// TODO Как показала практика, XContentBuilder слегка взрывоопасен и слишком изменяем. Следует тут задействовать
//      статически-типизированный play.json для генерации json-маппингов.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.03.13 16:58
 * Description: Функции для работы с ElasticSearch. В основном - функции генерации json-спек индексов.
 */

object SioEsUtil extends MacroLogsImpl {

  import DocFieldTypes.DocFieldType
  import LOGGER._
  import TermVectorVariants.TermVectorVariant

  // _FN - Filter Name. _AN - Analyzer Name, _TN - Tokenizer Name

  // Имена стеммеров
  def STEM_EN_FN    = "fStemEN"
  def STEM_RU_FN    = "fStemRU"

  // Имена stopwords-фильтров.
  def STOP_EN_FN    = "fStopEN"
  def STOP_RU_FN    = "fStopRU"

  def EDGE_NGRAM_FN_2 = "fEdgeNgram2"
  def EDGE_NGRAM_FN_1 = "fEdgeNgram1"
  def LOWERCASE_FN  = "fLowercase"
  def STD_FN        = "fStd"
  def WORD_DELIM_FN = "fWordDelim"

  def STD_TN        = "tStd"
  def DEEP_NGRAM_TN = "deepNgramTn"

  /** Стандартные имена полей ES. */
  object StdFns {
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
   * Создать параллельно пачку одинаковых индексов.
    *
    * @param indices Имена индексов.
   * @param shardsPerIndex Шардинг
   * @param replicasPerIndex Репликация
   * @return Список результатов (список isAcknowledged()).
   */
  def createIndices(indices: Seq[String], shardsPerIndex: Int = 1, replicasPerIndex: Int = 1)
                   (implicit client: Client, ec:ExecutionContext): Future[Seq[Boolean]] = {
    Future.traverse(indices) {
      createIndex(_, shardsPerIndex, replicasPerIndex)
    }
  }

  /**
    * Убедиться, что есть такой индекс
    *
    * @param indexName имя индекса
    * @param shards кол-во шард. По дефолту = 1
    * @param replicas кол-во реплик. По дефолту = 1
    * @return true, если индекс принят.
    */
  def createIndex(indexName: String, shards: Int = 1, replicas: Int = 1)
                 (implicit client: Client, ec: ExecutionContext): Future[Boolean] = {
    client.admin()
      .indices()
      .prepareCreate(indexName)
      .setSettings(getNewIndexSettings(shards=shards, replicas=replicas))
      .executeFut()
      .map(_.isAcknowledged)
  }


  /**
    * Отправить маппинг в индекс. Маппинги обычно генеряться в методах get*Mapping() этого модуля.
    *
    * @param indexName имя индекса, в который записать маппинг.
    * @param typeName имя типа для маппинга.
    * @param mapping маппинг.
    * @return true, если маппинг принят кластером.
    */
  def putMapping(indexName: String, typeName: String, mapping: XContentBuilder)
                (implicit client: Client, ec: ExecutionContext): Future[Boolean] = {
    client.admin()
      .indices()
      .preparePutMapping(indexName)
      .setType(typeName)
      .setSource(mapping)
      .executeFut()
      .map(_.isAcknowledged)
  }

  /**
    * Асинхронное определение наличия указанного индекса/указанных индексов в кластере.
    *
    * @param indexNames список индексов
    * @return true, если все перечисленные индексы существуют.
    */
  def isIndexExist(indexNames: String *)(implicit client: Client, ec: ExecutionContext): Future[Boolean] = {
    client.admin()
      .indices()
      .prepareExists(indexNames: _*)
      .executeFut()
      .map(_.isExists)
  }

  /**
    * Существует ли указанный индекс/индексы? Синхронный блокирующий запрос.
    *
    * @param indexNames Имена индексов.
    * @return true, если индексы с такими именами существуют.
    */
  @deprecated("Please use non-blocking isIndexExist()", "2013.07.05")
  def isIndexExistSync(indexNames: String*)(implicit client: Client) : Boolean = {
    client.admin()
      .indices()
      .exists( new IndicesExistsRequest(indexNames : _*) )
      .actionGet()
      .isExists
  }


  /**
    * Закрыть индекс указанный
    *
    * @param indexName имя индекса.
    */
  def closeIndex(indexName: String)(implicit client: Client, ec: ExecutionContext): Future[Boolean] = {
    lazy val logPrefix = s"closeIndex($indexName): "
    trace(logPrefix + "Starting close index ...")
    client.admin()
      .indices()
      .prepareClose(indexName)
      .executeFut()
      .map { _.isAcknowledged }
  }

  /**
    * Послать запрос на открытие индекса. Это загружает данные индекса в память соотвтествующих нод, если индекс был ранее закрыт.
    *
    * @param indexName Имя открываемого индекса.
    * @return true, если всё нормально
    */
  def openIndex(indexName: String)(implicit client: Client, ec: ExecutionContext): Future[Boolean] = {
    lazy val logPrefix = s"openIndex($indexName): "
    trace(logPrefix + "Sending open request...")
    client.admin().indices()
      .prepareOpen(indexName)
      .executeFut()
      .map { _.isAcknowledged }
  }


  // Кол-во попыток поиска свободного имени для будущего индекса.
  def FREE_INDEX_NAME_MAX_FIND_ATTEMPTS = 8


  /**
    * Собрать новый transport client для прозрачной связи с внешним es-кластером.
    *
    * @param addrs Адреса узлов.
    * @param clusterName Название удалённого кластера. Если None, то клиент будет игнорить проверку имени кластера.
    * @return TransportClient
    */
  def newTransportClient(addrs: Seq[TransportAddress], clusterName: Option[String]): TransportClient = {
    val settingsBuilder = Settings.builder()
      //.classLoader(classOf[Settings].getClassLoader)
    clusterName.fold {
      settingsBuilder.put("client.transport.ignore_cluster_name", true)
    } { _clusterName =>
      settingsBuilder.put("cluster.name", _clusterName)
    }
    val settings = settingsBuilder.build()
    new PreBuiltTransportClient(settings)
      .addTransportAddresses(addrs : _*)
  }


  /**
    * Метод для вызова генератора json'а.
    *
    * @param f Функция заполнения json-объекта данными.
    * @return Билдер с уже выстроенной структурой.
    */
  def jsonGenerator(f: XContentBuilder => JsonObject) : XContentBuilder = {
    val b = jsonBuilder().startObject()
    f(b).builder(b).endObject()
  }


  /**
   * Билдер настроек для индекса.
   * Тут генерится в представление в виде дерева scala-классов и сразу конвертится в XContent.
   */
  def getNewIndexSettings(shards: Int, replicas: Int = 1) = {
    val filters0 = List(STD_FN, WORD_DELIM_FN, LOWERCASE_FN)
    // Начать генерацию в псевдокоде, затем сразу перегнать в XContentBuilder
    jsonGenerator { implicit b =>
      IndexSettings(
        number_of_shards = shards,
        number_of_replicas = replicas,

        filters = Seq(
          FilterStandard(STD_FN),
          FilterLowercase(LOWERCASE_FN),
          FilterStopwords(STOP_EN_FN, "english"),
          FilterStopwords(STOP_RU_FN, "russian"),
          FilterWordDelimiter(WORD_DELIM_FN, preserve_original = true),
          FilterStemmer(STEM_RU_FN, "russian"),
          FilterStemmer(STEM_EN_FN, "english"),
          FilterEdgeNgram(EDGE_NGRAM_FN_2, minGram = 2, maxGram = 10, side = "front")
        ),

        analyzers = Seq(
          CustomAnalyzer(
            id = MINIMAL_AN,
            tokenizer = STD_TN,
            filters = filters0
          ),
          CustomAnalyzer(
            id = ENGRAM_AN_2,
            tokenizer = STD_TN,
            filters = filters0 ++ List(EDGE_NGRAM_FN_2)
          ),
          CustomAnalyzer(
            id = FTS_RU_AN,
            tokenizer = STD_TN,
            filters = filters0 ++ List(STOP_EN_FN, STOP_RU_FN, STEM_RU_FN, STEM_EN_FN)
          )
        ),

        tokenizers = Seq(
          TokenizerStandard(STD_TN)
        )
      )
    }
  }


  // Сеттинги для suggest.io / isuggest.ru и их обновления.
  // v2.1 Поддержка deep-ngram analyzer'а.

  /** Сборка спеки токенизера для DEEP_NGRAM_ANALYZER'а. */
  def _DEEP_NGRAM_TOKENIZER: Tokenizer = {
    NGramTokenizer(
      id          = DEEP_NGRAM_TN,
      minGram     = 1,
      maxGram     = 10,
      tokenChars  = Seq(TokenCharTypes.digit, TokenCharTypes.letter)
    )
  }

  /** Сборка спеки анализатора DEEP_NGRAM. */
  def _DEEP_NGRAM_ANALYZER: Analyzer = {
    CustomAnalyzer(
      id        = DEEP_NGRAM_AN,
      tokenizer = DEEP_NGRAM_TN,
      filters   = Nil
    )
  }

  // v2.2 Поддержка keyword analyzer для тегов.
  /** Дефолтовый keyword-токенизер. */
  def _KEYWORD_TOKENIZER: Tokenizer = {
    KeyWordTokenizer(
      id = KEYWORD_TN
    )
  }
 
  /** Спека для анализатора тегов. */
  def _KW_LC_ANALYZER: Analyzer = {
    CustomAnalyzer(
      id        = KW_LC_AN,
      tokenizer = KEYWORD_TN,
      filters   = Seq(LOWERCASE_FN)
    )
  }

  /** Сборка аналайзера, готовящего текст к FTS, но не дропаются стоп-слова никакие вообще. */
  def _FTS_NOSTOP_AN: Analyzer = {
    CustomAnalyzer(
      id        = FTS_NOSTOP_AN,
      tokenizer = STD_TN,
      filters   = Seq(STD_FN, WORD_DELIM_FN, LOWERCASE_FN, STEM_RU_FN, STEM_EN_FN)
    )
  }

  /** Сборка аналайзера, готовящего текст к индексации как по ENGRAM1, но БЕЗ фильтрации стоп-слов. */
  def _ENGRAM_AN1_NOSTOP : Analyzer = {
    CustomAnalyzer(
      id        = ENGRAM1_NOSTOP_AN,
      tokenizer = STD_TN,
      filters   = Seq(STD_FN, WORD_DELIM_FN, LOWERCASE_FN, STEM_RU_FN, STEM_EN_FN)
    )
  }

  /**
   * Сборка индекса по архитектуре второго поколения: без повсеместного edgeNgram, который жрёт как не в себя.
   * v2.1 добавляет кое-какой ngram analyzer для очень узконаправленных нужд.
    *
    * @param shards Кол-во шард.
   * @param replicas Кол-во реплик.
   * @return XCB с сеттингами.
   */
  def getIndexSettingsV2(shards: Int, replicas: Int = 1) = {
    jsonGenerator { implicit b =>
      IndexSettings(
        number_of_replicas = replicas,
        number_of_shards = shards,

        filters = Seq(
          // v2.0
          FilterStandard(STD_FN),
          FilterLowercase(LOWERCASE_FN),
          FilterStopwords(STOP_EN_FN, "english"),
          FilterStopwords(STOP_RU_FN, "russian"),
          FilterWordDelimiter(WORD_DELIM_FN, preserve_original = true),
          FilterStemmer(STEM_RU_FN, "russian"),
          FilterStemmer(STEM_EN_FN, "english"),
          FilterEdgeNgram(EDGE_NGRAM_FN_1, minGram = 1, maxGram = 10, side = "front"),
          FilterEdgeNgram(EDGE_NGRAM_FN_2, minGram = 2, maxGram = 10, side = "front")
        ),

        tokenizers = Seq(
          // v2.0
          TokenizerStandard(STD_TN),
          // v2.1
          _DEEP_NGRAM_TOKENIZER,
          // v2.2
          _KEYWORD_TOKENIZER
        ),

        analyzers = {
          val chFilters = Seq("html_strip")
          val filters0 = List(STD_FN, WORD_DELIM_FN, LOWERCASE_FN)
          val filters1 = filters0 ++ List(STOP_EN_FN, STOP_RU_FN, STEM_RU_FN, STEM_EN_FN)
          Seq(
            // v2.0
            CustomAnalyzer(
              id = DFLT_AN,
              charFilters = chFilters,
              tokenizer = STD_TN,
              filters = filters1
            ),
            CustomAnalyzer(
              id = ENGRAM_AN_1,
              charFilters = chFilters,
              tokenizer = STD_TN,
              filters = filters1 ++ List(EDGE_NGRAM_FN_1)
            ),
            CustomAnalyzer(
              id = ENGRAM_AN_2,
              charFilters = chFilters,
              tokenizer = STD_TN,
              filters = filters1 ++ List(EDGE_NGRAM_FN_2)
            ),
            CustomAnalyzer(
              id = MINIMAL_AN,
              tokenizer = STD_TN,
              filters = filters0
            ),
            // v2.1
            _DEEP_NGRAM_ANALYZER,
            // v2.2
            _KW_LC_ANALYZER,
            _FTS_NOSTOP_AN,
            _ENGRAM_AN1_NOSTOP
          )
        }
      )
    }
  }

  // Здесь был код установки анализатора тегов индекса (v2.2).


  /** Генератор мульти-полей title и contentText для маппинга страниц. Helper для getPageMapping(). */
  private def multiFieldFtsNgram(name: String, boostFts: Float, boostNGram: Float) = {
    FieldText(
      id = name,
      include_in_all  = true,
      index           = false,
      boost           = Some(boostFts),
      fields = Seq(
        FieldText(
          id              = SUBFIELD_ENGRAM,
          index           = true,
          analyzer        = ENGRAM_AN_2,
          search_analyzer = MINIMAL_AN,
          term_vector     = TermVectorVariants.with_positions_offsets,
          boost           = Some(boostNGram),
          include_in_all  = false
        )
      )
    )
  }


  /**
   * Маппинг для страниц, подлежащих индексированию.
    *
    * @return
   */
  def getPageMapping(typeName: String, compressSource: Boolean = true) = {
    jsonGenerator { implicit b =>
      IndexMapping(
        typ = typeName,

        staticFields = Seq(
          FieldSource(enabled = true),
          FieldAll(enabled = true, analyzer = FTS_RU_AN)
        ),

        properties = Seq(
          FieldText(
            id = FIELD_URL,
            index = false,
            include_in_all = false
          ),
          FieldText(
            id = FIELD_IMAGE_ID,
            index = false,
            include_in_all = false
          ),
          FieldNumber(
            id = FIELD_DATE_KILOSEC,
            fieldType = DocFieldTypes.long,
            index = false,
            include_in_all = false
          ),
          multiFieldFtsNgram(FIELD_TITLE, 4.1f, 2.7f),
          multiFieldFtsNgram(FIELD_CONTENT_TEXT, 1.0f, 0.7f),
          FieldKeyword(
            id = FIELD_LANGUAGE,
            index = true,
            include_in_all = false
          ),
          FieldKeyword(
            id = FIELD_DKEY,
            index = false,
            include_in_all = false
          ),
          // Тут array-поле, но для ES одинакого -- одно значение в поле или целый массив.
          FieldKeyword(
            id = FIELD_PAGE_TAGS,
            index = true,
            store = false,
            include_in_all = false
          )
        )
      )
    }
  }


  /** Класс для ActionRequestBuilder'ов, который явно возвращает Future.
    * Для ES >= 6.x, возможно для младших версий.
    */
  implicit class EsActionBuilderOpsExt[AResp <: ActionResponse](val esActionBuilder: ActionRequestBuilder[_, AResp, _]) extends AnyVal {

    /** Запуск экшена с возвращением Future[AResp]. */
    def executeFut(): Future[AResp] = {
      val p = Promise[AResp]()
      val l = new ActionListener[AResp] {
        override def onResponse(response: AResp): Unit =
          p.success(response)
        override def onFailure(e: Exception): Unit =
          p.failure(e)
      }
      esActionBuilder.execute(l)
      p.future
    }

  }


object TermVectorVariants extends Enumeration {
  type TermVectorVariant = Value
  val no, yes, with_offsets, with_positions, with_positions_offsets = Value
  def default = no
}

object DocFieldTypes extends Enumeration {
  type DocFieldType = Value
  val text, keyword, short, integer, long, float, double, boolean, `null`,
      multi_field, ip, geo_point, geo_shape, attachment, date, binary,
      nested, `object` = Value
}


// Далее идут классы JSON-DSL-генераторы для упрощения написания всяких вещей.

trait Renderable {
  def builder(implicit b: XContentBuilder) : XContentBuilder
}

// Классы-записи

// Объявление настроек верхнего уровня
case class IndexSettings(
  charFilters         : Seq[CharFilter] = Nil,
  analyzers           : Seq[Analyzer] = Nil,
  tokenizers          : Seq[Tokenizer] = Nil,
  filters             : Seq[Filter] = Nil,
  number_of_shards    : Int = -1,
  number_of_replicas  : Int = -1
) extends JsonObject {

  override def id = null

  override def fieldsBuilder(implicit b: XContentBuilder) {
    // Рендерим настройки всякие. Всякие неизменяемые вещи не должны рендерится, если не задано это.
    if (number_of_shards > 0)
      b.field("number_of_shards",   number_of_shards)
    if (number_of_replicas > 0)
      b.field("number_of_replicas", number_of_replicas)

    // Рендерим параметры анализа
    if (analyzers != Nil || tokenizers != Nil || filters != Nil) {
      b.startObject("analysis")
      maybeRenderListOf(analyzers,  "analyzer")
      maybeRenderListOf(tokenizers, "tokenizer")
      maybeRenderListOf(filters,    "filter")
      b.endObject()
    }
  }


  /**
   * Опционально отрендерить список объектов _renderable.
    *
    * @param list List[_renderable]
   * @param name Название возможного объекта.
   * @param b XContentBuilder
   * @return
   */
  protected def maybeRenderListOf(list:Seq[Renderable], name:String)(implicit b:XContentBuilder) {
    if (list.nonEmpty) {
      b.startObject(name)
        list map { _.builder }
      b.endObject()
    }
  }

}


/** Абстрактный json-объект в рамках DSL. */
trait JsonObject extends Renderable {
  def id: String
  
  override def builder(implicit b: XContentBuilder): XContentBuilder = {
    if (id != null) {
      b.startObject(id)
    }
    fieldsBuilder
    if (id != null) {
      b.endObject()
    }
    b
  }

  def fieldsBuilder(implicit b:XContentBuilder) {}
}


/** Многие объекты JSON DSL имеют параметр "type".
 * _typed_json_object подходит для описания фильтров, токенизаторов и т.д. */
trait TypedJsonObject extends JsonObject {
  def typ: String

  override def fieldsBuilder(implicit b: XContentBuilder) {
    if (typ != null)
      b.field("type", typ)
    super.fieldsBuilder(b)
  }
}


// Анализаторы ---------------------------------------------------------------------------------------------------------
trait Analyzer extends TypedJsonObject
case class CustomAnalyzer(
  id : String,
  charFilters: Seq[String] = Nil,
  tokenizer: String,
  filters : Seq[String]
) extends Analyzer {
  
  override def typ = "custom"

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (charFilters.nonEmpty)
      b.array("char_filter", charFilters : _*)
    if (tokenizer != null)
      b.field("tokenizer", tokenizer)
    if (filters.nonEmpty)
     b.array("filter", filters : _*)
  }
}
// END анализаторы -----------------------------------------------------------------------------------------------------



// Токенизаторы --------------------------------------------------------------------------------------------------------
/** Абстрактный токенизер. */
trait Tokenizer extends TypedJsonObject

/** Стандартный токенизер. */
case class TokenizerStandard(
  id : String,
  max_token_length : Int = 255
) extends Tokenizer {
  
  override def typ = "standard"

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (max_token_length != 255)
      b.field("max_token_length", max_token_length)
  }
}


object TokenCharTypes extends Enumeration {
  type TokenCharType = Value
  val letter, digit, whitespace, punctuation, symbol = Value
}

import TokenCharTypes.TokenCharType

/** Токенайзер, дробящий сорец в клочья на ngram'мы. */
case class NGramTokenizer(
  id: String,
  minGram: Int = 1,
  maxGram: Int = 2,
  tokenChars: Seq[TokenCharType] = Seq.empty
) extends Tokenizer with NgramBase {
  override def typ = "nGram"

  override def fieldsBuilder(implicit b: XContentBuilder): Unit = {
    super.fieldsBuilder
    if (tokenChars.nonEmpty) {
      val tcts = tokenChars.map(_.toString)
      b.array("token_chars", tcts : _*)
    }
  }
}


/**
 * Keyword tokenizer, т.е. отсутствие токенизации как таковой.
  *
  * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-keyword-tokenizer.html]]
 */
case class KeyWordTokenizer(
  id          : String,
  bufferSize  : Int = -1
) extends Tokenizer {

  override def typ = "keyword"

  override def fieldsBuilder(implicit b: XContentBuilder): Unit = {
    super.fieldsBuilder
    if (bufferSize > 0) {
      b.field("buffer_size", bufferSize)
    }
  }
}

// END токенизаторы ----------------------------------------------------------------------------------------------------


// Фильтры -------------------------------------------------------------------------------------------------------------
/** Объявление абстрактного фильтра в настройках индекса. */
trait Filter extends TypedJsonObject

// фильтр стоп-слов
case class FilterStopwords(
  id : String,
  stopwords : String    // = english, russian, etc
) extends Filter {

  override def typ = "stop"

  override def fieldsBuilder(implicit b:XContentBuilder) {
    super.fieldsBuilder
    b.field("stopwords", stopwords)
  }

}


/** Фильтр word_delimiter для дележки склеенных слов. */
case class FilterWordDelimiter(
  id : String,
  preserve_original : Boolean = false
) extends Filter {

  override def typ = "word_delimiter"

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (preserve_original)
      b.field("preserve_original", preserve_original)
  }

}


/** Фильтр стемминга слов. */
case class FilterStemmer(
  id : String,
  language : String
) extends Filter {

  override def typ = "stemmer"

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("language", language)
  }

}

/** Фильтр lowercase. */
case class FilterLowercase(id: String) extends Filter {
  override def typ = "lowercase"
}

/** Token-фильтр length для высеивания токенов с символьной длиной в заданном интервале длин. */
case class FilterLength(id: String,  min: Int = 0,  max: Int = Int.MaxValue) extends Filter {
  override def typ = "length"

  override def fieldsBuilder(implicit b: XContentBuilder): Unit = {
    super.fieldsBuilder
    b.field("min", min)
    b.field("max", max)
  }
}

/** Фильтр standard. */
case class FilterStandard(id: String) extends Filter {
  override def typ = "standard"
}

sealed trait NgramBase extends JsonObject {
  def minGram: Int
  def maxGram: Int

  override def fieldsBuilder(implicit b: XContentBuilder): Unit = {
    super.fieldsBuilder
    b.field("min_gram", minGram)
    b.field("max_gram", maxGram)
  }
}

/** Фильтр edge-ngram. */
case class FilterEdgeNgram(
  id: String,
  minGram : Int = 1,
  maxGram : Int = 2,
  side : String = "front"
) extends Filter with NgramBase {

  override def typ = "edgeNGram"

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (side != "front")
      b.field("side", side)
  }
}


case class FilterNgram(
  id: String,
  minGram: Int = 1,
  maxGram: Int = 2
) extends Filter with NgramBase {
  override def typ = "nGram"
}


trait CharFilter extends TypedJsonObject


// END фильтры ---------------------------------------------------------------------------------------------------------



// Поля документа ------------------------------------------------------------------------------------------------------
trait Field extends JsonObject

trait DocField extends Field with TypedJsonObject {
  def fieldType: DocFieldType
  override def typ: String = fieldType.toString
}

/** Почти все поля содержат параметр index_name. */
trait FieldInxName extends Field {
  def index_name: String

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder(b)
    if (index_name != null)
      b.field("index_name", index_name)
  }
}

trait FieldStoreable extends Field {
  def store: Boolean

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (store)
      b.field("store", "yes")
  }
}

trait FieldIndexable extends Field {
  def index : Boolean
  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("index", index)
  }
}


trait FieldNullable extends Field {
  def null_value : String
  
  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (null_value != null)
      b.field("null_value", null_value)
  }
}
  

@deprecated("not supported anymore", "ES >= 6.0")
trait FieldIncludeInAll extends Field {
  def include_in_all : Boolean
  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("include_in_all", include_in_all)
  }
}
  

trait FieldBoostable extends Field {
  def boost : Option[Float]

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (boost.isDefined)
      b.field("boost", boost.get)
  }
}

 
/** Некое абстрактное индексируемое поле. Сюда не входит binary. */
trait DocFieldIndexable extends DocField
with FieldInxName
with FieldStoreable
with FieldIndexable
with FieldIncludeInAll
with FieldBoostable
with FieldNullable


case class FieldText(
  id : String,
  index : Boolean,
  @deprecated("not supported anymore", "ES >= 6.0") include_in_all : Boolean,
  index_name : String = null,
  // _field_indexable
  store : Boolean = false,
  boost : Option[Float] = None,
  null_value : String = null,
  // _field_string
  term_vector : TermVectorVariant = null,
  omit_norms : Option[Boolean] = None,
  index_options : String = null,  // = docs | freqs | positions
  analyzer : String = null,
  search_analyzer : String = null,
  ignore_above : Option[Boolean] = None,
  position_offset_gap : Option[Int] = None,
  override val fields: Traversable[DocField] = Nil

) extends DocFieldIndexable with TextField with MultiFieldT {

  override def fieldType = DocFieldTypes.text

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (omit_norms.isDefined)
      b.field("omit_norms", omit_norms.get)
    if (index_options != null)
      b.field("index_options", index_options)
    if (ignore_above.isDefined)
      b.field("ignore_above", ignore_above.get)
    if (position_offset_gap.isDefined)
      b.field("position_offset_gap", position_offset_gap.get)
  }
}

case class FieldKeyword(
  id : String,
  index : Boolean,
  @deprecated("not supported anymore", "ES >= 6.0") include_in_all : Boolean,
  index_name : String = null,
  // _field_indexable
  store : Boolean = false,
  boost : Option[Float] = None,
  null_value : String = null,
  // _field_string
  ignore_above : Option[Boolean] = None,
  override val fields: Traversable[DocField] = Nil

) extends DocFieldIndexable with MultiFieldT {

  override def fieldType = DocFieldTypes.keyword

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (ignore_above.isDefined)
      b.field("ignore_above", ignore_above.get)
  }
}


/** Абстрактное поле для хранения неточных данных типа даты-времени и чисел. */
trait FieldApprox extends DocFieldIndexable {
  def precision_step : Option[Int]
  def ignore_malformed : Option[Boolean]

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (precision_step.isDefined)
      b.field("precision_step", precision_step.get)
    if (ignore_malformed.isDefined)
      b.field("ignore_malformed", ignore_malformed.get)
  }
}


/** Описание поля для ipv4 значений. Для es >= 5.0 можно хранить ipv6 (вроде). */
case class FieldIp(
  override val id             : String,
  override val boost          : Option[Float]         = None,
  override val docValues      : Option[Boolean]       = None,
  @deprecated("not supported anymore", "ES >= 6.0") override val include_in_all : Boolean               = true,
  override val index          : Boolean,
  override val null_value     : String                = null,
  override val precisionStep  : Int                   = -1,
  override val store          : Boolean               = false
)
  extends DocField
  with FieldBoostable
  with FieldDocValues
  with FieldIncludeInAll
  with FieldIndexable
  with FieldNullable
  with PrecisionStep
  with FieldStoreable
{
  override def fieldType = DocFieldTypes.ip
}

import StdFns._

/** Трейт для поля doc_values. */
trait FieldDocValues extends Field {
  /**
    * true или None, когда необходима аггрегация, сортировка, доступ из скриптов.
    * false, если это точно не требуется.
    */
  def docValues: Option[Boolean]

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    for (dv <- docValues)
      b.field("doc_values", dv)
  }
}



/** Поле с числом. */
case class FieldNumber(
  id : String,
  fieldType : DocFieldType,
  index : Boolean,
  @deprecated("not supported anymore", "ES >= 6.0") include_in_all : Boolean,
  index_name : String = null,
  store : Boolean = false,
  boost : Option[Float] = None,
  null_value : String = null,
  precision_step : Option[Int] = None,
  ignore_malformed : Option[Boolean] = None,
  fields: Traversable[DocField] = Nil
) extends FieldApprox with MultiFieldT


/** Поле с датой. */
case class FieldDate(
  id : String,
  index : Boolean,
  @deprecated("not supported anymore", "ES >= 6.0") include_in_all : Boolean,
  index_name : String = null,
  store : Boolean = false,
  boost : Option[Float] = None,
  null_value : String = null,
  precision_step : Option[Int] = None,
  ignore_malformed : Option[Boolean] = None
) extends FieldApprox {
  override def fieldType = DocFieldTypes.date
}


/** Булево значение. */
case class FieldBoolean(
  id : String,
  index : Boolean,
  @deprecated("not supported anymore", "ES >= 6.0") include_in_all : Boolean,
  index_name : String = null,
  store : Boolean = false,
  boost : Option[Float] = None,
  null_value : String = null
) extends DocFieldIndexable {
  override def fieldType = DocFieldTypes.boolean
}


/** Поле типа binary с барахлом в base64. */
case class FieldBinary(
  id : String,
  index_name : String = null
) extends DocField with FieldInxName {
  override def fieldType = DocFieldTypes.binary
}


trait FieldEnableable extends Field {
  def enabled: Boolean
  
  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("enabled", enabled)
  }
}

trait TextField extends Field {
  def term_vector: TermVectorVariant
  def search_analyzer: String
  def analyzer : String

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (term_vector != null)
      b.field("term_vector", term_vector.toString)
    if (search_analyzer != null)
      b.field("search_analyzer", search_analyzer)
    if (analyzer != null)
      b.field("analyzer", analyzer)
  }
}


/** Поле _all */
case class FieldAll(
  enabled : Boolean = true,
  store : Boolean = false,
  term_vector : TermVectorVariant = null,
  analyzer : String = null,
  search_analyzer : String = null
) extends FieldEnableable with TextField with FieldStoreable {
  override def id = FIELD_ALL
}


case class FieldId(
  override val index: Boolean,
  override val store: Boolean = false
) extends FieldStoreable with FieldIndexable {
  override def id = FIELD_ID
}


/** Поле _source */
case class FieldSource(enabled: Boolean = true) extends FieldEnableable {
  override def id = FIELD_SOURCE
}

/** static-поле для активации parent-child отношений. Автоматически включает принудительное поле _routing. */
case class FieldParent(typ: String) extends Field with TypedJsonObject {
  override def id = FIELD_PARENT
}

trait FieldWithPath extends Field {
  def path: String

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (path != null)
      b.field("path", path)
  }
}

/** Поле _routing. */
case class FieldRouting(
  required : Boolean = false,
  store : Boolean = false,
  index : Boolean,
  path  : String = null
) extends FieldStoreable with FieldIndexable with FieldWithPath {
  override def id = FIELD_ROUTING

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (required)
      b.field("required", required)
  }
}


/** Трейт для сборки multi-поля. В новом синтаксисе ElasticSearch, это должно примешиваться
  * к полям, в FieldString например. */
sealed trait MultiFieldT extends JsonObject {
  def fields: TraversableOnce[JsonObject]

  override def fieldsBuilder(implicit b: XContentBuilder): Unit = {
    super.fieldsBuilder
    if (fields.nonEmpty) {
      b.startObject("fields")
        fields foreach { _.builder }
      b.endObject()
    }
  }
}


trait FieldWithProperties extends Field {
  def properties: Seq[DocField]

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (properties.nonEmpty) {
      b.startObject("properties")
        properties foreach { _.builder }
      b.endObject()
    }
  }
}

/** Dynamic templates - механизм задания автоматики при автоматическом добавлении новых полей в маппинг.
  *
  * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-root-object-type.html#_dynamic_templates]]
  */
case class DynTemplate(id: String, nameMatch: String, matchMappingType: String = "{dynamic_type}", mapping: String)
extends JsonObject {
  val mapping1 = mapping.trim
  if (!mapping1.startsWith("{") || mapping1.endsWith("}"))
    throw new IllegalArgumentException("'mapping' field should contain json OBJECT")

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    b.field("match", nameMatch)
     .field("match_mapping_type", matchMappingType)
     .rawField("mapping", new ByteArrayInputStream( mapping1.getBytes ), XContentType.JSON)
  }
}

/** Генератор маппинга индекса со всеми полями и блекджеком. */
case class IndexMapping(typ:String, staticFields:Seq[Field], properties:Seq[DocField], dynTemplates: Seq[DynTemplate] = Nil) extends FieldWithProperties {
  override def id = typ

  override def fieldsBuilder(implicit b: XContentBuilder) {
    staticFields map { _.builder }
    super.fieldsBuilder(b)
    if (dynTemplates.nonEmpty) {
      b.startArray("dynamic_templates")
      dynTemplates.foreach { _.builder }
      b.endArray()
    }
  }
}

/** Просто object. Хранятся внутри исходного документа.
  * Имена полей обычного object распрямляются в "object.id", "someObj.someField.someSubfied" и т.д.
  * Гуглить по elasticsearch is flat. */
case class FieldObject(
  id          : String,
  properties  : Seq[DocField],
  enabled     : Boolean = true
) extends DocField with FieldWithProperties with FieldEnableable {
  override def fieldType = DocFieldTypes.`object`
}

/** Nested-объекты описаны тут. Nested хранятся как отдельные документы. */
case class FieldNestedObject(
  id          : String,
  properties  : Seq[DocField],
  enabled     : Boolean = true,
  includeInParent: Boolean = false,
  includeInRoot: Boolean = false
) extends DocField with FieldWithProperties with FieldEnableable {
  override def fieldType = DocFieldTypes.nested

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("include_in_parent", includeInParent)
     .field("include_in_root", includeInRoot)
  }
}


/** Поле с флагом для валидации. */
trait Validate extends Field {
  def validate: Boolean

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("validate", validate)
  }
}

trait ValidateLatLon extends Field {
  def validateLat: Boolean
  def validateLon: Boolean

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("validate_lat", validateLat)
     .field("validate_lon", validateLon)
  }
}

trait Normalize extends Field {
  def normalize: Boolean

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("normalize", normalize)
  }
}

trait NormalizeLatLon extends Field {
  def normalizeLat: Boolean
  def normalizeLon: Boolean

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (normalizeLat)
      b.field("normalize_lat", normalizeLat)
    if (normalizeLon)
      b.field("normalize_lon", normalizeLon)
  }
}

trait PrecisionStep extends Field {
  def precisionStep: Int

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (precisionStep > 0)
      b.field("precision_step", precisionStep)
  }
}



/** Маппинг для географической точки на земле. Точка выражается через координаты либо геохеш оных.
  *
  * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html]] */
case class FieldGeoPoint(
  id                : String,
  ignoreMalformed   : Option[Boolean] = None
)
  extends DocField
{
  override def fieldType = DocFieldTypes.geo_point

  override def fieldsBuilder(implicit b: XContentBuilder): Unit = {
    super.fieldsBuilder
    for (im <- ignoreMalformed)
      b.field("ignore_malformed", im)
  }
}

object GeoShapeTrees extends Enumeration {
  type GeoShapeTree = Value
  val geohash, quadtree = Value
}

case class FieldGeoShape(
  id: String,
  tree: GeoShapeTrees.GeoShapeTree = GeoShapeTrees.geohash,
  precision: String = "10m",
  treeLevels: Int = -1,
  distanceErrorPct: Float = -1
) extends DocField {
  override def fieldType = DocFieldTypes.geo_shape

  override def fieldsBuilder(implicit b: XContentBuilder): Unit = {
    super.fieldsBuilder
    b.field("tree", tree.toString)
     .field("precision", precision)
    if (treeLevels > 0)
      b.field("tree_levels", treeLevels)
    if (distanceErrorPct > 0)
      b.field("distance_error_pct", distanceErrorPct)
  }
}


// END: DSL полей документа --------------------------------------------------------------------------------------------

} // END: object SioEsUtil

