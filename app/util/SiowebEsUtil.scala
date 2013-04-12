package util

import org.elasticsearch.node.NodeBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilder, FilterBuilders, QueryBuilders}
import org.elasticsearch.search.SearchHit
import io.suggest.model.{SioSearchContext, DomainSettings}
import scala.collection.JavaConversions._
import collection.{MapLike, mutable}
import io.suggest.util.Lists

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.13 17:54
 * Description: Фунцкии для поддержки работы c ES на стороне веб-морды. В частности,
 * генерация запросов и поддержка инстанса клиента.
 * Тут по сути нарезка необходимого из sio_elastic_search, sio_lucene_query, sio_m_page_es.
 */

object SiowebEsUtil {

  val RESULT_COUNT_DEFAULT = 20

  // Настройки ret*-полей опций выдачи.
  type RET_T = Symbol
  val RET_ALWAYS      : RET_T = 'a
  val RET_NEVER       : RET_T = 'n
  val RET_IF_NO_IMAGE : RET_T = 'i

  val PER_SHARD_DOCUMENTS_LIMIT = 500

  val FIELD_URL   = "url"
  val FIELD_LANG  = "lang"
  val FIELD_TITLE = "title"
  val FIELD_CONTENT_TEXT = "content_text"
  val FIELD_IMAGE_KEY    = "image_key"
  val FIELD_ALL   = "_all"

  val FIELDS_ONLY_TITLE = List(FIELD_TITLE)
  val FIELDS_TEXT_ALL   = List(FIELD_TITLE, FIELD_CONTENT_TEXT)

  val SUBFIELD_ENGRAM = "gram"
  val SUBFIELD_FTS    = "fts"

  // Только эти поля могут быть в карте результата
  val resultAllowedFields = Set(FIELD_URL, FIELD_TITLE, FIELD_CONTENT_TEXT, FIELD_LANG, FIELD_IMAGE_KEY)

  val hlFragSepDefault    = " "

  // Инстанс локальной не-data ноды ES. Отсюда начинаются все поисковые и другие запросы.
  // TODO стоит это вынести это в отдельный актор? Нет при условии вызова node.close() при остановке системы.
  implicit val client:Client = NodeBuilder.nodeBuilder().client(true).node.client()

  /**
   * Поиск в рамках домена.
   * @param domainSettings
   * @param queryStr
   * @param options
   * @param searchContext
   * @return
   */
  def searchDomain(domainSettings:DomainSettings, queryStr:String, options:SioSearchOptions, searchContext:SioSearchContext) = {
    val (indices, types) = domainSettings.index_info.indexesTypesForRequest(searchContext)
    searchIndex(indices, types, queryStr, options)
  }


  /**
   * Фунция генерации поискового запроса и выполнения поиска. Используется query-генератор, горождаемый ES-клиентом.
   * @param indices Список индексов, по которым будет идти поиск.
   * @param types Список типов, которые затрагиваются поиском.
   * @param queryStr Буквы, которые ввел юзер
   * @param options Параметры запроса.
   */
  def searchIndex(indices:Seq[String], types:Seq[String], queryStr:String, options:SioSearchOptions) : Array[SearchHit] = {
    queryStr2Query(queryStr).map { textQuery =>

      var filters : List[FilterBuilder] = List(FilterBuilders.limitFilter( queryShardLimit(queryStr) ))

      // Обработать options.langs, дописав при необходимости дополнительный фильтр.
      if (!options.langs.isEmpty) {
        val langs = options.langs
        val termQuery = if (langs.tail.isEmpty) {
          // Один язык - делаем простой term-фильтр
          QueryBuilders.termQuery(FIELD_LANG, langs.head)
        } else {
          // Запрошено несколько языков. Используем terms-query
          QueryBuilders.termsQuery(FIELD_LANG, langs: _*)
        }
        filters = FilterBuilders.queryFilter(termQuery) :: filters
      }

      // Если получилось несколько фильтров, то нужно их объеденить через and-фильтр.
      val qFilter = if (!filters.tail.isEmpty)
        FilterBuilders.andFilter(filters : _*)
      else
        filters.head

      val query : QueryBuilder = QueryBuilders.filteredQuery(textQuery, qFilter)

      // TODO включить boost по датам. Чем свежее, тем выше score.
      /*if (options.withDateScoring) {
        query = QueryBuilders.customScoreQuery(query)
      }*/

      val reqBuilder = client
        .prepareSearch(indices : _*)
        .setTypes(types : _*)
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setExplain(options.withExplain)
        .setSize(options.size)
        .setQuery(query)
        .addField(FIELD_URL)

      if (!options.fields.isEmpty)
        reqBuilder.addFields(options.fields : _*)

      // Настраиваем выдачу заголовков и их подсветку
      if (options.retTitle != RET_NEVER) {
        val hlField = subfield(FIELD_TITLE, SUBFIELD_ENGRAM)
        reqBuilder
          .addField(hlField)
          .addHighlightedField(FIELD_TITLE, -1, 0)
      }

      // Настраиваем выдачу текста страницы.
      if (options.retContentText != RET_NEVER) {
        val hlField = subfield(FIELD_CONTENT_TEXT, SUBFIELD_ENGRAM)
        reqBuilder.addHighlightedField(
          hlField,
          options.hlCtFragmentsSize,
          options.hlCtFragmentsCount
        )
      }

      // Возвращать картинки?
      if (options.retImage)
        reqBuilder.addField(FIELD_IMAGE_KEY)

      // Выполнить запрос
      val response = reqBuilder
        .execute()
        .actionGet()

      val hits = response.getHits.getHits

      // Пора причесать результаты запроса

      ???

    }.getOrElse(Array())
  }

  // Удалятор .fts и .ngram из хвостов названий полей
  val rmSubfieldSuffixRe = "\\.[a-z]+$".r

  /**
   * В зависимости от настроек выдачи, нужно почистить выдаваемые результаты.
   * @param hits Результаты поиска в сыром виде.
   * @param options опции, которые, в частности, описывают параметры выдачи.
   */
  def postprocessSearchHits(hits:Array[SearchHit], options:SioSearchOptions) = {

    var mapHitF = { hit:SearchHit =>
      // подготовить highlighted-данные
      // .toMap нужен, ибо MapWrapper не совместим с типом Map, а является именно гребаным классом-враппером на ju.Map.
      val hlData = hit.highlightFields().map { case (key, hl) =>
        // убрать возможные суффиксы .fts и .gram из имён подсвеченных полей
        val key1 = rmSubfieldSuffixRe.replaceFirstIn(key, "")
        val hlText = hl.getFragments.mkString(options.hlCtFragmentSeparator)
        (key1, hlText)
      }
      val sourceData = hit.getFields.map { case (name, field) => (name, field.getValue[String]) }
      // Мержим два словаря, отдавая предпочтение подсвеченным элементам
      Lists
        .mergeMutableMaps(sourceData, hlData) { (_, _, v) => v }
        .filter { resultAllowedFields.contains(_) }
    }

    // Возможно, нужно снести убрать content_text или title. Тут функция, обновляющая mapHitF если нужно что-то ещё делать.
    val removeFieldOptF = { (fieldName:String, ret:RET_T) =>
      if (ret == RET_NEVER) {
        mapHitF = mapHitF andThen { map =>
          map.remove(fieldName)
          map
        }
      } else
      // Или сносить только там, где есть картинка
      if (ret == RET_IF_NO_IMAGE && options.retImage) {
        mapHitF = mapHitF andThen { map =>
          if(map.get(FIELD_IMAGE_KEY).isDefined) {
            map.remove(FIELD_CONTENT_TEXT)
          }
          map
        }
      } else {}
    }
    removeFieldOptF(FIELD_TITLE, options.retTitle)
    removeFieldOptF(FIELD_CONTENT_TEXT, options.retContentText)

    // Конечный словарь полей результата оборачиваем в класс-хелпер.
    val mapHitF1 = mapHitF andThen(new SioSearchResult(_))

    hits.map(mapHitF1)
  }


  /**
   * Сколько анализировать документов на шарду макс.
   * @param query строка поиска
   * @return
   */
  def queryShardLimit(query:String) = PER_SHARD_DOCUMENTS_LIMIT


  // Регэксп разбиения строки перед последним словом.
  val splitLastWordRe = "\\s+(?=\\S*+$)".r
  val endsWithSpace = "\\s$".r.pattern

  def splitQueryStr(queryStr:String) : Option[(String, String)] = {
    splitLastWordRe.split(queryStr) match {
      case Array() =>
        None

      case Array(words) =>
        // Нужно проверить words на наличие \\s внутри. Если есть, то это fts words. Иначе, engram-часть.
        val result = if (endsWithSpace.matcher(queryStr).find())
          (words, "")
        else
          ("", words)
        Some(result)

      case Array(fts, ngram:String) =>
        // Бывает, что fts-часть состоит из одного предлога или слова, которое кажется сейчас предлогом.
        // Если отправить в ES в качестве запроса предлог, то, очевидно, будет ноль результатов на выходе.
        val fts1 = if (Stopwords.ALL_STOPS.contains(ngram))
          ""
        else
          fts
        Some((fts1, ngram))
    }
  }


  /**
   * Взять queryString, вбитую юзером, распилить на куски, проанализировать и сгенерить запрос или комбинацию запросов
   * на её основе.
   * @param queryStr Строка, которую набирает в поиске юзер.
   */
  def queryStr2Query(queryStr:String) : Option[QueryBuilder] = {
    // Дробим исходный запрос на куски
    val topQueriesOpt = splitQueryStr(queryStr).map { case (ftsQS, engramQS) =>

      val ftsLen = ftsQS.length
      val engramLen = engramQS.length

      // Отрабатываем edge-ngram часть запроса.
      var queries : List[QueryBuilder] = if (engramLen == 0)
        List()

      else {
        // Если запрос короткий, то искать только по title
        val fields = if (ftsLen + engramLen <= 1)
          FIELDS_ONLY_TITLE
        else
          FIELDS_TEXT_ALL
        // Генерим базовый engram-запрос
        var engramQueries = fields.map { _field =>
          val _subfield = subfield(_field, SUBFIELD_ENGRAM)
          QueryBuilders.matchQuery(_subfield, engramQS)
        }
        // Если чел уже набрал достаточное кол-во символов, то искать парралельно в fts
        if (engramLen >= 4) {
          val _query = QueryBuilders.matchQuery(FIELD_ALL, engramQS)
          engramQueries = _query :: engramQueries
        }
        // Если получилось несколько запросов, то обернуть их в bool-query
        val finalEngramQuery = if (engramQueries.tail == Nil)
          engramQueries.head
        else {
          val boolQB = QueryBuilders.boolQuery().minimumNumberShouldMatch(1)
          engramQueries.foreach { boolQB.should(_) }
          boolQB
        }
        List(finalEngramQuery)
      }

      // Обработать fts-часть исходного запроса.
      if (ftsLen > 1) {
        val queryFts = QueryBuilders.matchQuery(FIELD_ALL, ftsQS)
        queries = queryFts :: queries
      }

      queries
    }

    // Если получилось несколько запросов верхнего уровня, то обернуть их bool-query must
    topQueriesOpt match {
      case None => None

      case Some(topQueries) =>
        topQueries match {

          case List(query) => Some(query)
          case Nil => None

          case _ =>
            val queryBool = QueryBuilders.boolQuery()
            topQueries.foreach { queryBool.must(_) }
            Some(queryBool)
        }
    }
  }


  protected def subfield(field:String, subfield:String) = field + "." + subfield
}


// Класс, хранящий опции поиска.
final case class SioSearchOptions(
  // Настройки выдачи результатов
  retTitle : SiowebEsUtil.RET_T = SiowebEsUtil.RET_ALWAYS,
  retContentText : SiowebEsUtil.RET_T = SiowebEsUtil.RET_ALWAYS,
  retImage : Boolean = true,
  size : Int = SiowebEsUtil.RESULT_COUNT_DEFAULT,

  // Языки, по которым стоит искать
  langs : List[String] = List(),
  fields : List[String] = List(),

  // Подсветка результатов
  hlCtFragmentsCount : Int = 1,
  hlCtFragmentsSize : Int = 200,
  hlCtFragmentSeparator : String = SiowebEsUtil.hlFragSepDefault,

  // Дебажные настройки всякие
  withDateScoring : Boolean = true,
  withExplain : Boolean = false
)


// Используем переменные для снижения количества мусора и упрощения логики, ибо класс заполняется в цикле.
case class SioSearchResult(map:mutable.Map[String,String]) {

  import SiowebEsUtil._

  def title       = map.get(FIELD_TITLE)
  def contentText = map.get(FIELD_CONTENT_TEXT)
  def imageKey    = map.get(FIELD_IMAGE_KEY)
  def url         = map(FIELD_URL)
  def lang        = map.get(FIELD_LANG)

}

