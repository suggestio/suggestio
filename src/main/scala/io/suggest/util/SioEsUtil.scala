package io.suggest.util

import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.index_info.SioEsConstants._
import scala.concurrent.{Future, Promise}
import org.elasticsearch.action.{ActionListener, ListenableActionFuture}
import org.elasticsearch.action.admin.indices.optimize.{OptimizeResponse, OptimizeRequest}
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import org.elasticsearch.action.admin.indices.close.CloseIndexRequestBuilder
import org.elasticsearch.action.admin.indices.open.OpenIndexRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.03.13 16:58
 * Description: Функции для работы с ElasticSearch. В основном - функции генерации json-спек индексов.
 */

object SioEsUtil extends Logs {

  /**
   * Убедиться, что есть такой индекс
   * @param indexName имя индекса
   * @param shards кол-во шард. По дефолту = 1
   * @param replicas кол-во реплик. По дефолту = 1
   * @return true, если индекс принят.
   */
  def ensureIndex(indexName:String, shards:Int = 1, replicas:Int=1)(implicit c:Client, executor:ExecutionContext): Future[Boolean] = {
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
  def closeIndex(indexName:String)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    lazy val logPrefix = "closeIndex(%s): " format indexName
    debug(logPrefix + "Starting close ...")
    val adm = client.admin().indices()
    new CloseIndexRequestBuilder(adm).setIndex(indexName).execute().transform(
      {resp =>
        val result = resp.isAcknowledged
        debug(logPrefix + "Close index result = %s" format result)
        result
      },
      {ex =>
        warn(logPrefix + "Cannot close index", ex)
        ex}
    )
  }

  /**
   * Послать запрос на открытие индекса. Это загружает данные индекса в память соотвтествующих нод, если индекс был ранее закрыт.
   * @param indexName Имя открываемого индекса.
   * @return true, если всё нормально
   */
  def openIndex(indexName:String)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    lazy val logPrefix = "openIndex(%s): " format indexName
    debug(logPrefix + "Sending open request...")
    new OpenIndexRequestBuilder(client.admin().indices())
      .setIndex(indexName)
      .execute()
      .transform(
        {resp =>
          val result = resp.isAcknowledged
          debug(logPrefix + "Open index finished: %s" format result)
          result
        },
        {ex =>
          error(logPrefix + "Open index error", ex)
          ex}
      )
  }


  // Кол-во попыток поиска свободного имени для будущего индекса.
  val FREE_INDEX_NAME_MAX_FIND_ATTEMPTS = 8

  /**
   * Совсем асинхронно найти свободное имя индекса (не занятое другими индексами).
   * @return Фьючерс строки названия индекса.
   */
  def getFreeIndexName(maxAttempts: Int = FREE_INDEX_NAME_MAX_FIND_ATTEMPTS)(implicit client:Client, executor:ExecutionContext) : Future[String] = {
    lazy val logPrefix = "getFreeIndexName(%s): " format maxAttempts
    debug(logPrefix + "Starting...")
    val p = Promise[String]()
    // Тут как бы рекурсивный неблокирующий фьючерс.
    def freeIndexNameLookup(n:Int) {
      if (n < maxAttempts) {
        val id = StringUtil.randomId(10).toLowerCase + ".x"
        debug(logPrefix + "Asking for random index name %s..." format id)
        SioEsUtil.isIndexExist(id) onComplete {
          case Success(true) =>
            debug(logPrefix + "Index name '%s' is busy. Retrying." format id)
            freeIndexNameLookup(n + 1)

          case Success(false) =>
            debug(logPrefix + "Free index name found: %s" format id)
            p success id

          case Failure(ex) =>
            warn(logPrefix + "Cannot call isIndexExist(%s). Retry" format id, ex)
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
   * @param maxAttempts Число попыток поиска свободного имени индекса. просто передается в getFreeIndexName.
   * @return Фьчерс с именем созданного индекса.
   */
  def createRandomIndex(maxAttempts: Int = FREE_INDEX_NAME_MAX_FIND_ATTEMPTS)(implicit client:Client, executor:ExecutionContext): Future[String] = {
    lazy val logPrefix = "createRandomIndex(%s): " format maxAttempts
    getFreeIndexName(maxAttempts)
      .flatMap { indexName =>
        debug(logPrefix + "free index name: %s" format indexName)
        SioEsUtil.ensureIndex(indexName) map {
          case true  =>
            debug("Index '%s' ensured and ready." format indexName)
            indexName

          case false => throw new Exception("Index is not created. Free name became busy?")
        }
      }
  }


  protected def jsonGenerator(f: XContentBuilder => _json_object) : XContentBuilder = {
    val b = jsonBuilder().startObject()
    f(b).builder(b).endObject()
  }


  /**
   * Билдер настроек для индекса. Тут генерится в представление в виде дерева scala-классов и сразу конвертится в XContent.
   */
  def getNewIndexSettings(shards:Int, replicas:Int=1) = {
    val filters0 = List("f_std", "f_word_delim", "f_lowercase")
    val TOKENIZER_NAME = "t_std"
    // Начать генерацию в псевдокоде, затем сразу перегнать в XContentBuilder
    jsonGenerator { implicit b =>
      new _index_settings(
        number_of_shards = shards,
        number_of_replicas = replicas,
        cache_field_type = "soft",

        analyzers = Seq(
          new _analyzer_custom(
            id = ANALYZER_MINIMAL,
            tokenizer = TOKENIZER_NAME,
            filters = filters0
          ),
          new _analyzer_custom(
            id = ANALYZER_EDGE_NGRAM,
            tokenizer = TOKENIZER_NAME,
            filters = filters0 ++ List("f_edge_ngram")
          ),
          new _analyzer_custom(
            id = ANALYZER_FTS_RU,
            tokenizer = TOKENIZER_NAME,
            filters = filters0 ++ List("f_stop_en", "f_stop_ru", "f_stem_ru", "f_stem_en")
          )
        ),

        tokenizers = Seq(new _tokenizer_standard(TOKENIZER_NAME)),

        filters = Seq(
          new _filter_standard("f_std"),
          new _filter_lowercase("f_lowercase"),
          new _filter_stopwords("f_stop_en", "english"),
          new _filter_stopwords("f_stop_ru", "russian"),
          new _filter_word_delimiter("f_word_delim", preserve_original = true),
          new _filter_stemmer("f_stem_ru", "russian"),
          new _filter_stemmer("f_stem_en", "english"),
          new _filter_edge_ngram("f_edge_ngram", min_gram = 1, max_gram = 10, side = "front")
        )
      )
    }
  }


  /**
   * Маппинг для страниц, подлежащих индексированию.
   * @return
   */
  def getPageMapping(typeName:String, compressSource:Boolean=true) = {
    // Генератор полей для маппинга страниц
    def multiFieldFtsNgram(name:String, boostFts:Float, boostNGram:Float) = {
      new _field_multifield(name, fields = Seq(
        new _field_string(name, include_in_all=true, index="no", boost=Some(boostFts)),
        new _field_string(
          id = "gram",
          index = "analyzed",
          index_analyzer = ANALYZER_EDGE_NGRAM,
          search_analyzer = ANALYZER_MINIMAL,
          term_vector = "with_positions_offsets",
          boost = Some(boostNGram)
        )
      ))
    }

    jsonGenerator { implicit b =>
      new _index_mapping(
        typ = typeName,

        static_fields = Seq(
          new _field_source(true),
          new _field_all(true, analyzer = ANALYZER_FTS_RU)
        ),

        properties = Seq(
          new _field_string(FIELD_URL, index="no", include_in_all=false),
          new _field_string(FIELD_IMAGE_ID, index="no", include_in_all=false),   // TODO в старой версии почему-то было true
          new _field_number(FIELD_DATE_KILOSEC, typ="long", index="no", include_in_all=false),
          multiFieldFtsNgram(FIELD_TITLE, 4.1f, 2.7f),
          multiFieldFtsNgram(FIELD_CONTENT_TEXT, 1.0f, 0.7f),
          new _field_string(FIELD_LANGUAGE, index="not_analyzed", include_in_all=false)
        )
      )
    }

  }

  /**
   * Маппинг для эскизов иллюстраций.
   * @return
   */
  def getImageMapping(typeName:String) = jsonGenerator { implicit b =>
    new _index_mapping(
      typ = typeName,
      static_fields = Seq(
        new _field_source(true),
        new _field_all(false)
      ),
      properties = Seq(
        new _field_string("url", index="no"),
        new _field_binary("thumb")
      )
    )
  }


  /**
   * Запрос асинхронной оптимизации индекса.
   * @param req
   * @param client
   * @return
   */
  def optimize(req: OptimizeRequest)(implicit client:Client) : Future[OptimizeResponse] = {
    val p = Promise[OptimizeResponse]()
    val listener = actionListener(p)
    client.admin().indices().optimize(req, listener)
    p.future
  }


  /**
   * Неявный конвертов вызова execute() в scala-future.
   * @param laf результат execute(), т.е. ListenableActionFuture.
   * @tparam T Тип, с которым работаем.
   * @return Фьючерс типа T.
   */
  implicit def laFuture2sFuture[T](laf: ListenableActionFuture[T]): Future[T] = {
    val p = Promise[T]()
    laf.addListener(actionListener(p))
    p.future
  }


  /**
   * Листенер для scala promise. Отражает результат работы ListenableActionFuture[T] на Promise[T]
   * @param promise Обещалко.
   * @tparam T Тип значения.
   * @return ActionListener[T] пригодный для навешивания на ListenableActionFuture.
   */
  private def actionListener[T](promise: Promise[T]) = new ActionListener[T] {
    def onResponse(response: T) {
      promise.success(response)
    }

    def onFailure(e: Throwable) {
      promise.failure(e)
    }
  }

}


// Далее идут классы JSON-DSL-генераторы для упрощения написания всяких вещей.

trait _renderable {
  def builder(implicit b:XContentBuilder) : XContentBuilder
}

// Классы-записи

// Объявление настроек верхнего уровня
case class _index_settings(
  analyzers : Seq[_analyzer] = Nil,
  tokenizers :Seq[_tokenizer] = Nil,
  filters : Seq[_filter] = Nil,
  number_of_shards : Int = 5,
  number_of_replicas : Int = 0,
  cache_field_type : String = "soft"
) extends _json_object("settings") {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    b.startObject("index")
      // Рендерим настройки всякие
      .field("number_of_shards", number_of_shards)
      .field("number_of_replicas", number_of_replicas)
      .field("cache.field_type", cache_field_type)

      // Рендерим параметры анализа
      if (analyzers != Nil || tokenizers != Nil || filters != Nil) {
        b.startObject("analysis")
        maybeRenderListOf(analyzers,  "analysis")
        maybeRenderListOf(tokenizers, "tokenizer")
        maybeRenderListOf(filters,    "filter")
        b.endObject()
      }

    b.endObject()
  }


  /**
   * Опционально отрендерить список объектов _renderable.
   * @param list List[_renderable]
   * @param name Название возможного объекта.
   * @param b XContentBuilder
   * @return
   */
  protected def maybeRenderListOf(list:Seq[_renderable], name:String)(implicit b:XContentBuilder) {
    if (!list.isEmpty) {
      b.startObject(name)
        list map { _.builder }
      b.endObject()
    }
  }

}


// Абстрактный json-объект.
class _json_object(_id:String) extends _renderable {
  def builder(implicit b: XContentBuilder): XContentBuilder = {
    b.startObject(_id)
      fieldsBuilder
    b.endObject()
  }

  def fieldsBuilder(implicit b:XContentBuilder) {}
}


// Многие объекты JSON DSL имеют параметр "type".
// _typed_json_object подходит для описания фильтров, токенизаторов и т.д.
class _typed_json_object(id:String, typ:String) extends _json_object(id) {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder(b)
    if (typ != null)
      b.field("type", typ)
  }
}


// Анализаторы ---------------------------------------------------------------------------------------------------------
class _analyzer(_id:String, typ:String) extends _typed_json_object(_id, typ)
class _analyzer_custom(
  id : String,
  tokenizer: String,
  filters : List[String]
) extends _analyzer(id, "custom") {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    b.field("tokenizer", tokenizer)
     .array("filter", filters : _*)
  }
}
// END анализаторы -----------------------------------------------------------------------------------------------------



// Токенизаторы --------------------------------------------------------------------------------------------------------
// Абстрактный токенизер
class _tokenizer(_id : String, typ : String) extends _typed_json_object(_id, typ)

// Стандартный токенизер.
class _tokenizer_standard(
  id : String,
  max_token_length : Int = 255
) extends _tokenizer(id, "standard") {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (max_token_length != 255)
      b.field("max_token_length", max_token_length)
  }
}
// END токенизаторы ----------------------------------------------------------------------------------------------------


// Фильтры -------------------------------------------------------------------------------------------------------------
// объявление абстрактного фильтра в настройках индекса
class _filter(_id : String, typ : String) extends _typed_json_object(_id, typ)

// фильтр стоп-слов
class _filter_stopwords(
  id : String,
  stopwords : String    // = english, russian, etc
) extends _filter(id, "stopwords") {

  override def fieldsBuilder(implicit b:XContentBuilder) {
    super.fieldsBuilder

    b.field("stopwords", stopwords)
  }

}


// Фильтр word_delimiter для дележки склеенных слов.
class _filter_word_delimiter(
  id : String,
  preserve_original : Boolean = false
) extends _filter(id, "word_delimiter") {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    if (preserve_original != false)
      b.field("preserve_original", preserve_original)
  }

}


// Фильтр стемминга слов
class _filter_stemmer(
  id : String,
  language : String
) extends _filter(id, "stemmer") {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    b.field("language", language)
  }

}

// Фильтры lowercase и standard
class _filter_lowercase(id : String) extends _filter(id, "lowercase")
class _filter_standard(id: String) extends _filter(id, "standard")

// Фильтр edge-ngram
class _filter_edge_ngram(
  id: String,
  min_gram : Int = 1,
  max_gram : Int = 2,
  side : String = "front"
) extends _filter(id, "edgeNGram") {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    if (min_gram > 1)
      b.field("min_gram", min_gram)
    if (max_gram > 2)
      b.field("max_gram", max_gram)
    if (side != "front")
      b.field("side", side)
  }
}

// END фильтры ---------------------------------------------------------------------------------------------------------



// Поля документа ------------------------------------------------------------------------------------------------------
// Почти все поля содержат параметр index_name.
class _field_renameable(id:String, typ:String, index_name:String = null) extends _typed_json_object(id, typ) {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder(b)

    if (index_name != null)
      b.field("index_name", index_name)
  }
}


// Некое абстрактное индексируемое поле. Сюда не входит binary.
class _field_indexable(
  id : String,
  index_name : String = null,
  // _field_indexable
  typ : String,
  store : String = null,          // = [yes] | no
  index : String = null,          // = [analyzed] | not_analyzed | no
  boost : Option[Float] = None,
  include_in_all : Boolean = true,
  null_value : String = null
) extends _field_renameable(id, typ, index_name) {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (store != null)
      b.field("store", store)
    if (index != null)
      b.field("index", index)
    if (boost.isDefined)
      b.field("boost", boost.get)
    if (include_in_all != true)
      b.field("include_in_all", include_in_all)
    if (null_value != null)
      b.field("null_value", null_value)
  }
}


// Поле строки
class _field_string(
  id : String,
  index_name : String = null,
  // _field_indexable
  store : String = null,          // = [yes] | no
  index : String = null,          // = [analyzed] | not_analyzed | no
  boost : Option[Float] = None,
  include_in_all : Boolean = true,
  null_value : String = null,
  // _field_string
  term_vector : String = null,    // = [no] | yes | with_offsets | with_positions | with_positions_offsets
  omit_norms : Option[Boolean] = None,
  index_options : String = null,  // = docs | freqs | positions
  analyzer : String = null,
  index_analyzer : String = null,
  search_analyzer : String = null,
  ignore_above : Option[Boolean] = None,
  position_offset_gap : Option[Int] = None

) extends _field_indexable(
    id=id, typ="string", store=store, index=index, index_name=index_name, boost=boost,
    include_in_all=include_in_all, null_value=null_value
) {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (term_vector != null)
      b.field("term_vector", term_vector)
    if (omit_norms.isDefined)
      b.field("omit_norms", omit_norms.get)
    if (index_options != null)
      b.field("index_options", index_options)
    if (analyzer != null)
      b.field("analyzer", analyzer)
    if (index_analyzer != null)
      b.field("index_analyzer", index_analyzer)
    if (search_analyzer != null)
      b.field("search_analyzer", search_analyzer)
    if (ignore_above.isDefined)
      b.field("ignore_above", ignore_above.get)
    if (position_offset_gap.isDefined)
      b.field("position_offset_gap", position_offset_gap.get)
  }
}


// Абстрактное поле для хранения неточных данных типа даты-времени и чисел.
class _field_approx(
  id : String,
  typ : String,
  index_name : String = null,
  // _field_indexable
  store : String = null,          // = [yes] | no
  index : String = null,          // = [analyzed] | not_analyzed | no
  boost : Option[Float] = None,
  include_in_all : Boolean = true,
  null_value : String = null,
  // _field_approx
  precision_step : Option[Int] = None,
  ignore_malformed : Option[Boolean] = None
) extends _field_indexable(
    id=id, typ=typ, store=store, index=index, index_name=index_name, boost=boost,
    include_in_all=include_in_all, null_value=null_value
) {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (precision_step.isDefined)
      b.field("precision_step", precision_step.get)
    if (ignore_malformed.isDefined)
      b.field("ignore_malformed", ignore_malformed.get)
  }
}


// Поле с числом
class _field_number(
  id : String,
  typ : String,                   // = float | double | integer | long | short | byte
  index_name : String = null,
  store : String = null,          // = [yes] | no
  index : String = null,          // = [analyzed] | not_analyzed | no
  boost : Option[Float] = None,
  include_in_all : Boolean = true,
  null_value : String = null,
  precision_step : Option[Int] = None,
  ignore_malformed : Option[Boolean] = None

) extends _field_approx(
    id=id, typ=typ, store=store, index=index, index_name=index_name, boost=boost, include_in_all=include_in_all,
    null_value=null_value, precision_step=precision_step, ignore_malformed=ignore_malformed
)


// Поле с датой.
class _field_date(
  id : String,
  index_name : String = null,
  store : String = null,          // = [yes] | no
  index : String = null,          // = [analyzed] | not_analyzed | no
  boost : Option[Float] = None,
  include_in_all : Boolean = true,
  null_value : String = null,
  precision_step : Option[Int] = None,
  ignore_malformed : Option[Boolean] = None
) extends _field_approx(
    id=id, typ="date", store=store, index=index, index_name=index_name, boost=boost, include_in_all=include_in_all,
    null_value=null_value, precision_step=precision_step, ignore_malformed=ignore_malformed
)


// Булево значение
class _field_boolean(
  id : String,
  index_name : String = null,
  store : String = null,          // = [yes] | no
  index : String = null,          // = [analyzed] | not_analyzed | no
  boost : Option[Float] = None,
  include_in_all : Boolean = true,
  null_value : String = null
) extends _field_indexable(
  id=id, typ="boolean", index_name=index_name, store=store, index=index, boost=boost,
  include_in_all=include_in_all, null_value=null_value
)


// Поле типа binary с барахлом в base64
class _field_binary(
  id : String,
  index_name : String = null
) extends _field_renameable(id=id, typ="binary", index_name=index_name)


class _field_enableable(id:String, enabled:Boolean) extends _json_object(id) {
  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder
    b.field("enabled", enabled)
  }
}


// Поле _all
class _field_all(
  enabled : Boolean = true,
  store : String = null,
  term_vector : String = null,
  analyzer : String = null,
  index_analyzer : String = null,
  search_analyzer : String = null

) extends _field_enableable(FIELD_ALL, enabled) {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (store != null)
      b.field("store", store)
    if (term_vector != null)
      b.field("term_vector", term_vector)
    if (analyzer != null)
      b.field("analyzer", analyzer)
    if (index_analyzer != null)
      b.field("index_analyzer", index_analyzer)
    if (search_analyzer != null)
      b.field("search_analyzer", search_analyzer)
  }
}


// Поле _source
class _field_source(enabled:Boolean = true) extends _field_enableable(FIELD_SOURCE, enabled)


// Поле _routing
class _field_routing(
  required : Boolean = false,
  store : String = null,
  index : String = null,
  path  : String = null
) extends _json_object(FIELD_ROUTING) {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (required != false)
      b.field("required", required)
    if (store != null)
      b.field("store", store)
    if (index != null)
      b.field("index", index)
    if (path != null)
      b.field("path", path)
  }
}


// Мультиполе multi_field
class _field_multifield(id:String, fields:Seq[_json_object]) extends _typed_json_object(id, "multi_field") {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder

    if (!fields.isEmpty) {
      b.startObject("fields")
        fields map { _.builder }
      b.endObject()
    }
  }
}


// Генератор маппинга индекса со всеми полями и блекджеком.
class _index_mapping(typ:String, static_fields:Seq[_json_object], properties:Seq[_json_object]) extends _json_object(typ) {

  override def fieldsBuilder(implicit b: XContentBuilder) {
    super.fieldsBuilder(b)
    static_fields map { _.builder }

    if (!properties.isEmpty) {
      b.startObject("properties")
        properties map { _.builder }
      b.endObject()
    }
  }
}

// END поля документа --------------------------------------------------------------------------------------------------

