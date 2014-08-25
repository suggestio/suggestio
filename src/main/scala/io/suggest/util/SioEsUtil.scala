package io.suggest.util

import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.common.xcontent.XContentBuilder
import SioConstants._
import scala.concurrent.{Future, Promise}
import org.elasticsearch.action.{ActionListener, ListenableActionFuture}
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import io.suggest.model.MVirtualIndex
import org.elasticsearch.node.{NodeBuilder, Node}
import org.elasticsearch.cluster.ClusterName

// TODO Как показала практика, XContentBuilder слегка взрывоопасен и слишком изменяем. Следует тут задействовать
//      статически-типизированный play.json для генерации json-маппингов.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.03.13 16:58
 * Description: Функции для работы с ElasticSearch. В основном - функции генерации json-спек индексов.
 */

object SioEsUtil extends MacroLogsImpl {

  import LOGGER._
  import FieldIndexingVariants.FieldIndexingVariant
  import TermVectorVariants.TermVectorVariant
  import DocFieldTypes.DocFieldType

  // _FN - Filter Name. _AN - Analyzer Name, _TN - Tokenizer Name

  // Имена стеммеров
  val STEM_EN_FN    = "fStemEN"
  val STEM_RU_FN    = "fStemRU"

  // Имена stopwords-фильтров.
  val STOP_EN_FN    = "fStopEN"
  val STOP_RU_FN    = "fStopRU"

  val EDGE_NGRAM_FN_2 = "fEdgeNgram2"
  val EDGE_NGRAM_FN_1 = "fEdgeNgram1"
  val LOWERCASE_FN  = "fLowercase"
  val STD_FN        = "fStd"
  val WORD_DELIM_FN = "fWordDelim"

  val STD_TN        = "tStd"
  val DFLT_AN       = "default"
  val DEEP_NGRAM_TN = "deepNgramTn"

  /**
   * Создать параллельно пачку одинаковых индексов.
   * @param indices Имена индексов.
   * @param shardsPerIndex Шардинг
   * @param replicasPerIndex Репликация
   * @return Список результатов (список isAcknowledged()).
   */
  def createIndices(indices:Seq[String], shardsPerIndex:Int = 1, replicasPerIndex:Int = 1)(implicit c:Client, executor:ExecutionContext) : Future[Seq[Boolean]] = {
    Future.traverse(indices) {
      createIndex(_, shardsPerIndex, replicasPerIndex)
    }
  }

  /**
   * Убедиться, что есть такой индекс
   * @param indexName имя индекса
   * @param shards кол-во шард. По дефолту = 1
   * @param replicas кол-во реплик. По дефолту = 1
   * @return true, если индекс принят.
   */
  def createIndex(indexName:String, shards:Int = 1, replicas:Int=1)(implicit c:Client, executor:ExecutionContext): Future[Boolean] = {
    c.admin().indices()
      .prepareCreate(indexName)
      .setSettings(getNewIndexSettings(shards=shards, replicas=replicas))
      .execute()
      .map(_.isAcknowledged)
  }


  /**
   * Отправить маппинг в индекс. Маппинги обычно генеряться в методах get*Mapping() этого модуля.
   * @param indexName имя индекса, в который записать маппинг.
   * @param typeName имя типа для маппинга.
   * @param mapping маппинг.
   * @return true, если маппинг принят кластером.
   */
  def putMapping(indexName:String, typeName:String, mapping:XContentBuilder)(implicit client:Client, executor:ExecutionContext) : Future[Boolean] = {
    client.admin().indices()
      .preparePutMapping(indexName)
      .setType(typeName)
      .setSource(mapping)
      .execute()
      .map(_.isAcknowledged)
  }

  /**
   * Асинхронное определение наличия указанного индекса/указанных индексов в кластере.
   * @param indexNames список индексов
   * @return true, если все перечисленные индексы существуют.
   */
  def isIndexExist(indexNames: String *)(implicit client:Client, executor:ExecutionContext) : Future[Boolean] = {
    client.admin().indices()
      .prepareExists(indexNames: _*)
      .execute()
      .map(_.isExists)
  }

  /**
   * Существует ли указанный индекс/индексы? Синхронный блокирующий запрос.
   * @param indexNames Имена индексов.
   * @return true, если индексы с такими именами существуют.
   */
  @deprecated("Please use non-blocking isIndexExist()", "2013.07.05")
  def isIndexExistSync(indexNames:String *)(implicit c:Client) : Boolean = {
    c.admin().indices()
      .exists(new IndicesExistsRequest(indexNames : _*))
      .actionGet()
      .isExists
  }


  /**
   * Закрыть индекс указанный
   * @param indexName имя индекса.
   */
  def closeIndex(indexName: String)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    lazy val logPrefix = s"closeIndex($indexName): "
    trace(logPrefix + "Starting close index ...")
    client.admin().indices()
      .prepareClose(indexName)
      .execute()
      .map { _.isAcknowledged }
  }

  /**
   * Послать запрос на открытие индекса. Это загружает данные индекса в память соотвтествующих нод, если индекс был ранее закрыт.
   * @param indexName Имя открываемого индекса.
   * @return true, если всё нормально
   */
  def openIndex(indexName:String)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    lazy val logPrefix = s"openIndex($indexName): "
    trace(logPrefix + "Sending open request...")
    client.admin().indices()
      .prepareOpen(indexName)
      .execute()
      .map { _.isAcknowledged }
  }


  // Кол-во попыток поиска свободного имени для будущего индекса.
  val FREE_INDEX_NAME_MAX_FIND_ATTEMPTS = 8

  /**
   * Совсем асинхронно найти свободное имя индекса (не занятое другими индексами).
   * @return Фьючерс строки названия индекса.
   */
  def findFreeVirtualIndex(shardCount: Int, maxAttempts: Int = FREE_INDEX_NAME_MAX_FIND_ATTEMPTS)(implicit client:Client, executor:ExecutionContext) : Future[MVirtualIndex] = {
    lazy val logPrefix = "findFreeIndexName(shardCount=%s, maxAttempts=%s): " format (shardCount, maxAttempts)
    trace(logPrefix + "Starting...")
    val p = Promise[MVirtualIndex]()
    // Тут как бы рекурсивный неблокирующий фьючерс.
    def freeIndexNameLookup(n: Int) {
      if (n < maxAttempts) {
        val vinPrefix = MVirtualIndex.generateVinPrefix()
        debug(logPrefix + "Trying vinPrefix = %s" format vinPrefix)
        val mvi = MVirtualIndex(vinPrefix, shardCount)
        val firstEsShard = mvi.head
        debug(logPrefix + "Asking for random index name %s..." format firstEsShard)
        SioEsUtil.isIndexExist(firstEsShard) onComplete {
          case Success(true) =>
            trace(logPrefix + "Index name '%s' is busy. Retrying." format firstEsShard)
            freeIndexNameLookup(n + 1)

          case Success(false) =>
            debug(logPrefix + "Free index name found: %s" format firstEsShard)
            p success mvi

          case Failure(ex) =>
            warn(logPrefix + "Cannot call isIndexExist(%s). Retry" format firstEsShard, ex)
            freeIndexNameLookup(n + 1)
        }
      } else {
        p failure new RuntimeException(logPrefix + "Too many failure attemps")
      }
    }
    freeIndexNameLookup(0)
    p.future
  }


  /**
   * Создать рандомный индекс.
   * @param shardCount Кол-во шард, будет вдолблено в имя домена.
   * @param maxAttempts Число попыток поиска свободного имени индекса. просто передается в getFreeIndexName.
   * @return Фьчерс с именами созданных индексов-шард.
   */
  def createRandomVirtualIndex(shardCount:Int, replicasCount:Int, maxAttempts: Int = FREE_INDEX_NAME_MAX_FIND_ATTEMPTS)(implicit client:Client, executor:ExecutionContext): Future[MVirtualIndex] = {
    lazy val logPrefix = "createRandomIndex(shardCount=%s, maxAttempts=%s): " format (shardCount, maxAttempts)
    trace(logPrefix + "starting")
    findFreeVirtualIndex(shardCount, maxAttempts)
      .flatMap { mvi =>
        info(logPrefix + "Ensuring shards for mvi=%s" format mvi)
        mvi.ensureShards(replicasCount) map { _ => mvi }
      }
  }


  /**
   * Метод для вызова генератора json'а.
   * @param f Функция заполнения json-объекта данными.
   * @return Билдер с уже выстроенной структурой.
   */
  def jsonGenerator(f: XContentBuilder => JsonObject) : XContentBuilder = {
    val b = jsonBuilder().startObject()
    f(b).builder(b).endObject()
  }


  /**
   * Билдер настроек для индекса. Тут генерится в представление в виде дерева scala-классов и сразу конвертится в XContent.
   */
  def getNewIndexSettings(shards: Int, replicas : Int = 1) = {
    val filters0 = List(STD_FN, WORD_DELIM_FN, LOWERCASE_FN)
    // Начать генерацию в псевдокоде, затем сразу перегнать в XContentBuilder
    jsonGenerator { implicit b =>
      IndexSettings(
        number_of_shards = shards,
        number_of_replicas = replicas,
        cache_field_type = "soft",

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
          AnalyzerCustom(
            id = MINIMAL_AN,
            tokenizer = STD_TN,
            filters = filters0
          ),
          AnalyzerCustom(
            id = EDGE_NGRAM_AN_2,
            tokenizer = STD_TN,
            filters = filters0 ++ List(EDGE_NGRAM_FN_2)
          ),
          AnalyzerCustom(
            id = FTS_RU_AN,
            tokenizer = STD_TN,
            filters = filters0 ++ List(STOP_EN_FN, STOP_RU_FN, STEM_RU_FN, STEM_EN_FN)
          )
        ),

        tokenizers = Seq(new TokenizerStandard(STD_TN))
      )
    }
  }

  /**
   * Сборка индекса по архитектуре второго поколения: без edgeNgram, который жрёт как не в себя.
   * Используется единственный анализатор.
   * @param shards Кол-во шард.
   * @param replicas Кол-во реплик.
   * @return XCB с сеттингами.
   */
  def getIndexSettingsV2(shards: Int, replicas: Int = 1) = {
    jsonGenerator { implicit b =>
      IndexSettings(
        number_of_replicas = replicas,
        number_of_shards = shards,
        cache_field_type = "soft",

        filters = Seq(
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
          new TokenizerStandard(STD_TN),
          new NGramTokenizer(DEEP_NGRAM_TN, minGram = 1, maxGram = 10, tokenChars = Seq(TokenCharTypes.digit, TokenCharTypes.letter))
        ),
        analyzers = {
          val chFilters = Seq("html_strip")
          val filters0 = List(STD_FN, WORD_DELIM_FN, LOWERCASE_FN)
          val filters1 = filters0 ++ List(STOP_EN_FN, STOP_RU_FN, STEM_RU_FN, STEM_EN_FN)
          Seq(
            AnalyzerCustom(
              id = DFLT_AN,
              charFilters = chFilters,
              tokenizer = STD_TN,
              filters = filters1
            ),
            AnalyzerCustom(
              id = EDGE_NGRAM_AN_1,
              charFilters = chFilters,
              tokenizer = STD_TN,
              filters = filters1 ++ List(EDGE_NGRAM_FN_1)
            ),
            AnalyzerCustom(
              id = EDGE_NGRAM_AN_2,
              charFilters = chFilters,
              tokenizer = STD_TN,
              filters = filters1 ++ List(EDGE_NGRAM_FN_2)
            ),
            AnalyzerCustom(
              id = MINIMAL_AN,
              tokenizer = STD_TN,
              filters = filters0
            ),
            AnalyzerCustom(
              id = DEEP_NGRAM_AN,
              tokenizer = DEEP_NGRAM_TN,
              filters = Nil
            )
          )
        }
      )
    }
  }

  /** 2014.aug.25: Добавить недостающий анализатор для разборки флагов sink show levels. */
  def getIndexSettingsV2_1 = {
    jsonGenerator { implicit b =>
      IndexSettings(
        tokenizers = Seq(
          new NGramTokenizer(DEEP_NGRAM_TN, minGram = 1, maxGram = 10, tokenChars = Seq(TokenCharTypes.digit, TokenCharTypes.letter))
        ),
        analyzers = Seq(
          AnalyzerCustom(
            id = DEEP_NGRAM_AN,
            tokenizer = DEEP_NGRAM_TN,
            filters = Nil
          )
        )
      )
    }
  }

  /** Запустить добавление новых настроек в индекс. Для этого индекс надо закрыть. */
  def updateIndexTo2_1(indexName: String)(implicit ec: ExecutionContext, client: Client): Future[_] = {
    closeIndex(indexName)
      .flatMap { _ =>
        client.admin().indices()
          .prepareUpdateSettings(indexName)
          .setSettings(getIndexSettingsV2_1.string())
          .execute()
      } flatMap { _ =>
        openIndex(indexName)
      }
  }


  /** Генератор мульти-полей title и contentText для маппинга страниц. Helper для getPageMapping(). */
  private def multiFieldFtsNgram(name:String, boostFts:Float, boostNGram:Float) = {
    new FieldMultifield(name, fields = Seq(
      FieldString(
        id = name,
        include_in_all = true,
        index = FieldIndexingVariants.no,
        boost = Some(boostFts)
      ),
      FieldString(
        id = "gram",
        index = FieldIndexingVariants.analyzed,
        index_analyzer = EDGE_NGRAM_AN_2,
        search_analyzer = MINIMAL_AN,
        term_vector = TermVectorVariants.with_positions_offsets,
        boost = Some(boostNGram),
        include_in_all = false
      )
    ))
  }


  /**
   * Маппинг для страниц, подлежащих индексированию.
   * @return
   */
  def getPageMapping(typeName:String, compressSource:Boolean=true) = {
    jsonGenerator { implicit b =>
      new IndexMapping(
        typ = typeName,

        staticFields = Seq(
          FieldSource(enabled = true),
          FieldAll(enabled = true, analyzer = FTS_RU_AN)
        ),

        properties = Seq(
          FieldString(
            id = FIELD_URL,
            index = FieldIndexingVariants.no,
            include_in_all = false
          ),
          FieldString(
            id = FIELD_IMAGE_ID,
            index = FieldIndexingVariants.no,
            include_in_all = false
          ),
          FieldNumber(
            id = FIELD_DATE_KILOSEC,
            fieldType = DocFieldTypes.long,
            index = FieldIndexingVariants.no,
            include_in_all = false
          ),
          multiFieldFtsNgram(FIELD_TITLE, 4.1f, 2.7f),
          multiFieldFtsNgram(FIELD_CONTENT_TEXT, 1.0f, 0.7f),
          FieldString(
            id = FIELD_LANGUAGE,
            index = FieldIndexingVariants.not_analyzed,
            include_in_all = false
          ),
          FieldString(
            id = FIELD_DKEY,
            index = FieldIndexingVariants.no,
            include_in_all = false
          ),
          // Тут array-поле, но для ES одинакого -- одно значение в поле или целый массив.
          FieldString(
            id = FIELD_PAGE_TAGS,
            index = FieldIndexingVariants.not_analyzed,
            store = false,
            include_in_all = false
          )
        )
      )
    }
  }


  /**
   * Неявный конвертов вызова execute() в scala-future.
   * @param laf результат execute(), т.е. ListenableActionFuture.
   * @tparam T Тип, с которым работаем.
   * @return Фьючерс типа T.
   */
  implicit def laFuture2sFuture[T](laf: ListenableActionFuture[T]): Future[T] = {
    val p = Promise[T]()
    laf.addListener(new EsAction2Promise(p))
    p.future
  }


object FieldIndexingVariants extends Enumeration {
  type FieldIndexingVariant = Value
  val analyzed, not_analyzed, no = Value
  def default = analyzed
  def isIndexed(fiv: FieldIndexingVariant) = fiv != no
}

object TermVectorVariants extends Enumeration {
  type TermVectorVariant = Value
  val no, yes, with_offsets, with_positions, with_positions_offsets = Value
  def default = no
}

object DocFieldTypes extends Enumeration {
  type DocFieldType = Value
  val string, integer, long, float, double, boolean, `null`,
      multi_field, ip, geo_point, geo_shape, attachment, date, binary,
      nested, `object` = Value
}


// Далее идут классы JSON-DSL-генераторы для упрощения написания всяких вещей.

trait Renderable {
  def builder(implicit b:XContentBuilder) : XContentBuilder
}

// Классы-записи

// Объявление настроек верхнего уровня
case class IndexSettings(
  charFilters         : Seq[CharFilter] = Nil,
  analyzers           : Seq[Analyzer] = Nil,
  tokenizers          : Seq[Tokenizer] = Nil,
  filters             : Seq[Filter] = Nil,
  number_of_shards    : Int = -1,
  number_of_replicas  : Int = -1,
  cache_field_type    : String = ""
) extends JsonObject {

  override def id = null

  override def fieldsBuilder(implicit b: XContentBuilder) {
    // Рендерим настройки всякие. Всякие неизменяемые вещи не должны рендерится, если не задано это.
    if (number_of_shards > 0)
      b.field("number_of_shards",   number_of_shards)
    if (number_of_replicas > 0)
      b.field("number_of_replicas", number_of_replicas)
    if (cache_field_type != null & !cache_field_type.isEmpty)
      b.field("cache.field_type",   cache_field_type)

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
case class AnalyzerCustom(
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
case class FilterLowercase(id : String) extends Filter{
  override def typ = "lowercase"
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
  def index : FieldIndexingVariant
  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (index != null)
      b.field("index", index.toString)
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

/** Поле строки. */
case class FieldString(
  id : String,
  index : FieldIndexingVariant,
  include_in_all : Boolean,
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
  index_analyzer : String = null,
  search_analyzer : String = null,
  ignore_above : Option[Boolean] = None,
  position_offset_gap : Option[Int] = None

) extends DocFieldIndexable with TextField {

  override def fieldType = DocFieldTypes.string

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


/** Поле с числом. */
case class FieldNumber(
  id : String,
  fieldType : DocFieldType,
  index : FieldIndexingVariant,
  include_in_all : Boolean,
  index_name : String = null,
  store : Boolean = false,
  boost : Option[Float] = None,
  null_value : String = null,
  precision_step : Option[Int] = None,
  ignore_malformed : Option[Boolean] = None
) extends FieldApprox


/** Поле с датой. */
case class FieldDate(
  id : String,
  index : FieldIndexingVariant,
  include_in_all : Boolean,
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
  index : FieldIndexingVariant,
  include_in_all : Boolean,
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
  def index_analyzer : String
  def analyzer : String

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (term_vector != null)
      b.field("term_vector", term_vector.toString)
    if (search_analyzer != null)
      b.field("search_analyzer", search_analyzer)
    if (index_analyzer != null)
      b.field("index_analyzer", index_analyzer)
    if (analyzer != null)
      b.field("analyzer", analyzer)
  }
}

/** Задание времени жизни документов в маппинге.
  * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-ttl-field.html]] */
case class FieldTtl(
  enabled: Boolean = false,
  default: String = null,
  store: Boolean = true,
  index: FieldIndexingVariant = null    // По дефолту not_analyzed - это техническая необходимость.
) extends FieldEnableable with FieldStoreable with FieldIndexable {
  override def id: String = FIELD_TTL

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (default != null)
      b.field("default", default)
  }
}

/** Поле _all */
case class FieldAll(
  enabled : Boolean = true,
  store : Boolean = false,
  term_vector : TermVectorVariant = null,
  analyzer : String = null,
  index_analyzer : String = null,
  search_analyzer : String = null
) extends FieldEnableable with TextField with FieldStoreable {
  override def id = FIELD_ALL
}


case class FieldId(
  index: FieldIndexingVariant = null,
  store: Boolean = false,
  path: String = null
) extends FieldStoreable with FieldIndexable with FieldWithPath {
  override def id = FIELD_ID
}


/** Поле _source */
case class FieldSource(enabled: Boolean = true) extends FieldEnableable {
  override def id = FIELD_SOURCE
}

/** static-поле для активации parent-child отношений. Автоматически включает принудительное поле _routing. */
case class FieldParent(typ: String) extends Field with TypedJsonObject {
  override def id = "_parent"
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
  index : FieldIndexingVariant = null,
  path  : String = null
) extends FieldStoreable with FieldIndexable with FieldWithPath {
  override def id = FIELD_ROUTING

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (required)
      b.field("required", required)
  }
}


/** Мультиполе multi_field. */
case class FieldMultifield(id:String, fields:Seq[JsonObject]) extends DocField {
  
  override def fieldType = DocFieldTypes.multi_field

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (fields.nonEmpty) {
      b.startObject("fields")
        fields map { _.builder }
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
     .rawField("mapping", mapping1.getBytes)
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


/** lat_lon флаг позволяет управлять отдельной индексацией широты и долготы.
  * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_indexed_fields]] */
trait LatLon extends Field {
  def latLon: Boolean

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("lat_lon", latLon)
  }
}

/** geohash -- система кодирования координат в географические регионы.
  * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_geohashes]]
 */
trait Geohash extends Field {
  def geohash: Boolean
  def geohashPrecision: String    // "12" | "20m"
  def geohashPrefix: Boolean      // Note: This option implicitly enables geohash

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    if (geohash)
      b.field("geohash", geohash)
    if (geohashPrecision != null && !geohashPrecision.isEmpty) {
      val ghFn = "geohash_precision"
      if (geohashPrecision matches "\\d+") {
        b.field(ghFn, geohashPrecision.toInt)
      } else {
        b.field(ghFn, geohashPrecision)
      }
    }
    if (geohashPrefix)
      b.field("geohash_prefix", geohashPrefix)
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


object GeoPointFieldDataFormats extends Enumeration {
  type GeoPointFieldDataFormat = Value
  val compressed, array = Value
}

import GeoPointFieldDataFormats._

case class GeoPointFieldData(format: GeoPointFieldDataFormat, precision: String = null)
trait GeoPointFieldDataField extends Field {
  def fieldData: GeoPointFieldData

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    val fd = fieldData
    if (fd != null) {
      b.startObject("fielddata")
        .field("format", fd.format.toString)
      if (fd.precision != null)
        b.field("precision", fd.precision)
      b.endObject()
    }
  }
}


/** Маппинг для географической точки на земле. Точка выражается через координаты либо геохеш оных.
  * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html]] */
case class FieldGeoPoint(
  id            : String,
  latLon        : Boolean = false,
  geohash       : Boolean = false,
  geohashPrecision: String = null,      // "12" | "20m"
  geohashPrefix : Boolean = false,      // Note: This option implicitly enables geohash
  validate      : Boolean = false,
  validateLat   : Boolean = false,
  validateLon   : Boolean = false,
  normalize     : Boolean = true,
  normalizeLat  : Boolean = false,
  normalizeLon  : Boolean = false,
  precisionStep : Int = -1,
  fieldData     : GeoPointFieldData = null
) extends DocField with LatLon with Geohash with Validate with ValidateLatLon with Normalize with NormalizeLatLon
  with PrecisionStep with GeoPointFieldDataField {
  override def fieldType = DocFieldTypes.geo_point
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


/** Неабстрактный трейт для подмешивания клиенского функционала в произольный объект.
  * Для управления именем кластера, нужно переопределить метод getEsClusterName.  */
trait SioEsClient {

  /** Тут хранится клиент к кластеру. В инициализаторе класса надо закинуть сюда начальный экземпляр клиент.
    * Это переменная для возможности остановки клиента. */
  protected var _node: Node = createNode

  /** Имя кластера elasticsearch, к которому будет коннектиться клиент. */
  def getEsClusterName: String = ClusterName.DEFAULT.value()

  /** Если нужен юникаст, то нужно передать непустой список из host или host:port */
  def unicastHosts: List[String] = Nil

  /** Убедиться, что клиент запущен. Обычно вызывается при запуске системы. */
  def ensureNode() = {
    synchronized {
      if (_node == null) {
        _node = createNode
      }
    }
    _node.start()
    _node
  }

  /** Собрать и запустить клиентскую ноду. */
  def createNode = {
    val nb = NodeBuilder.nodeBuilder()
      .client(true)
      .clusterName(getEsClusterName)
    // На продакшене бывает полезно задать адреса узла/узлов кластера по юникасту.
    val uh = unicastHosts
    if (uh.nonEmpty) {
      println("ES-client: using unicast cluster discovery: " + uh.mkString(", "))
      nb.getSettings
        .put("discovery.zen.ping.multicast.enabled", false)
        .put("discovery.zen.ping.unicast.enabled", true)
        .putArray("discovery.zen.ping.unicast.hosts", uh: _*)
    }
    nb.node
  }

  /** Остановить клиентскую ноду, если запущена. */
  def stopNode() {
    if (_node != null) {
      _node.close()
      _node = null
    }
  }

  /** Инстанс локальной client-ноды ES. Отсюда начинаются все поисковые и другие запросы. */
  implicit def client = _node.client()



  /** Перед вычищением из памяти класса следует убедится, что нода остановлена.
    * Маловероятно, что от этой функции есть какой-то толк. */
  override def finalize() {
    _node.stop()
  }
}



/**
 * ES-листенер, отражающий результат работы ListenableActionFuture[T] на Promise[T].
 * @param promise Пустой объект обещания.
 * @tparam T Тип будущего значения.
 * @return ActionListener[T] пригодный для навешивания на ListenableActionFuture.
 */
class EsAction2Promise[T](promise: Promise[T]) extends ActionListener[T] {
  override def onResponse(response: T) {
    promise success response
  }

  override def onFailure(ex: Throwable) {
    promise failure ex
  }
}


