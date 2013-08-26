package util

import org.elasticsearch.node.NodeBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilder, FilterBuilders, QueryBuilders}
import org.elasticsearch.search.SearchHit
import io.suggest.model._
import scala.collection.JavaConversions._
import collection.mutable
import io.suggest.util.{LogsImpl, Lists}
import io.suggest.index_info.SioEsConstants._
import controllers.routes
import scala.Some
import io.suggest.model.SioSearchContext

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

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  // Настройки ret*-полей опций выдачи.
  type RET_T = Char
  val RET_ALWAYS      : RET_T = 'a'
  val RET_NEVER       : RET_T = 'n'
  val RET_IF_NO_IMAGE : RET_T = 'i'

  // Макс.число документов, анализируемых на совпадение после чтения из шарды
  val PER_SHARD_DOCUMENTS_LIMIT = 1800

  val FIELDS_ONLY_TITLE = List(FIELD_TITLE)
  val FIELDS_TEXT_ALL   = List(FIELD_TITLE, FIELD_CONTENT_TEXT)
  val FIELD_THUMB_REL_URL = "image_rel_url"

  // Только эти поля могут быть в карте результата
  val resultAllowedFields = Set(FIELD_URL, FIELD_TITLE, FIELD_CONTENT_TEXT, FIELD_LANGUAGE, FIELD_THUMB_REL_URL)

  val hlFragSepDefault    = " "

  protected type SioSearchResultsT = List[SioSearchResult]


  // Инстанс локальной не-data ноды ES. Отсюда начинаются все поисковые и другие запросы.
  // TODO стоит это вынести это в отдельный актор? Нет при условии вызова node.close() при остановке системы.
  implicit val client:Client = NodeBuilder.nodeBuilder().client(true).node.client()

  /**
   * Поиск в рамках домена.
   * @param domainSettings Состояние настроек домена по мнению siobix.
   * @param queryStr Строка, которую набирает юзер
   * @param options Настройки поиска по мнению sioweb21
   * @param searchContext Контекст поиска.
   * @return
   */
  def searchDomain(domainSettings:DomainSettingsT, queryStr:String, options:SioSearchOptions, searchContext:SioSearchContext) = {
    // TODO потом нужно добавить поддержку переключения searchPtr в каком-либо виде.
    import options.dkey
    MDVISearchPtr.getForDkeyId(dkey) match {
      case Some(searchPtr) =>
        // Собрать все индексы и типы со всех виртуальных индексов.
        val (indices, types) = searchPtr.dviNames.foldLeft[(List[String], List[String])] (Nil -> Nil) { (acc, dviName) =>
          MDVIActive.getForDkeyName(dkey, dviName) match {
            case Some(dviActive) =>
              val (accIndices, accTypes) = acc
              val accIndices1 = dviActive.getShards.toList ++ accIndices
              val accTypes1   = dviActive.getTypesForRequest(searchContext) ++ accTypes
              accIndices1 -> accTypes1

            case None =>
              warn("searchPtr '%s' points to nothing at dviName=%s" format (searchPtr, dviName))
              acc
          }
        }
        searchIndex(indices, types, queryStr, options)

      case None => Nil
    }
  }


  /**
   * Фунцкия генерации поискового запроса и выполнения поиска. Используется query-DSL из ES-клиента.
   * Отделена от searchDomain для возможности создания произвольного поиска, а не только в рамках домена.
   * @param indices Список индексов, по которым будет идти поиск.
   * @param types Список типов, которые затрагиваются поиском.
   * @param queryStr Буквы, которые ввел юзер
   * @param options Параметры запроса.
   */
  def searchIndex(indices:Seq[String], types:Seq[String], queryStr:String, options:SioSearchOptions) : SioSearchResultsT = {
    queryStr2Query(queryStr).map { textQuery =>

      var filters : List[FilterBuilder] = {
        val limitFilter = FilterBuilders.limitFilter( queryShardLimit(queryStr) )
        List(limitFilter)
      }

      // Обработать options.langs, дописав при необходимости дополнительный фильтр.
      if (!options.langs.isEmpty) {
        val langs = options.langs
        val termQuery = if (langs.tail.isEmpty) {
          // Один язык - делаем простой term-фильтр
          QueryBuilders.termQuery(FIELD_LANGUAGE, langs.head)
        } else {
          // Запрошено несколько языков. Используем terms-query
          QueryBuilders.termsQuery(FIELD_LANGUAGE, langs: _*)
        }
        filters = FilterBuilders.queryFilter(termQuery) :: filters
      }

      // Если получилось несколько фильтров, то нужно их объеденить через and-фильтр.
      val qFilter = if (!filters.tail.isEmpty)
        FilterBuilders.andFilter(filters : _*)
      else
        filters.head

      val query : QueryBuilder = QueryBuilders.filteredQuery(textQuery, qFilter)

      // TODO включить boost по датам. Чем свежее, тем выше score. Для этого нужна поддержка бустера в DomainSettings
      /*if (options.withDateScoring) {
        query = QueryBuilders.customScoreQuery(query)
      }*/

      // Начать собирать поисковый запрос
      val reqBuilder = client
        .prepareSearch(indices : _*)
        .setTypes(types : _*)
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setExplain(options.withExplain)
        .setSize(options.size)
        .setQuery(query)
        .addField(FIELD_URL)

      // Добавить доролнительные поля для запроса, если такие есть.
      if (!options.fields.isEmpty)
        reqBuilder.addFields(options.fields : _*)

      // Настраиваем выдачу заголовков и их подсветку
      if (options.retTitle != RET_NEVER) {
        val hlField = subfield(FIELD_TITLE, SUBFIELD_ENGRAM)
        reqBuilder
          .addField(FIELD_TITLE)
          .addHighlightedField(hlField, -1, 0)
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
        reqBuilder.addField(FIELD_IMAGE_ID)

      // Выполнить запрос
      val response = reqBuilder
        .execute()
        .actionGet()

      val hits = response.getHits.getHits

      // Функционал причесывания результатов вынесен в отдельную функцию.
      postprocessSearchHits(hits, options)

    }.getOrElse(List())
  }


  // Удалятор .fts и .ngram из хвостов названий полей
  val rmSubfieldSuffixRe = "\\.[a-z]+$".r

  // Подсвеченный хайлатером текст обычно на полслова и без окончания. Нужно это пофиксить, переместив </em> в конец слова.
  val hlTagsFixRe = "(\\p{L}{3,})(</em>)(\\p{L}{1,6})\\b".r
  val hlTagsFixReplace = "$1$3$2"


  /**
   * Двинуть закрывающие теги em в конец слов при условии выполнения вышеуказанного регэкспа.
   * @param str исходная строка -- выхлоп highlight
   * @return
   */
  def moveHlTags(str:String) = hlTagsFixRe.replaceAllIn(str, hlTagsFixReplace)


  /**
   * В зависимости от настроек выдачи, нужно почистить выдаваемые результаты.
   * @param hits Результаты поиска в сыром виде.
   * @param options опции, которые, в частности, описывают параметры выдачи.
   */
  def postprocessSearchHits(hits:Array[SearchHit], options:SioSearchOptions) : SioSearchResultsT = {
    // Собрать функцию маппинга одного словаря результата
    var mapHitF = { hit:SearchHit =>
      // подготовить highlighted-данные
      // .toMap нужен, ибо MapWrapper не совместим с типом Map, а является именно гребаным классом-враппером на ju.Map.
      val hlData = hit.highlightFields.map { case (key, hl) =>
        // убрать возможные суффиксы .fts и .gram из имён подсвеченных полей
        val key1 = rmSubfieldSuffixRe.replaceFirstIn(key, "")
        val hlText = hl.getFragments
          .map { textFragment => moveHlTags(textFragment.string()) }
          .mkString(options.hlCtFragmentSeparator)
        (key1, hlText)
      }
      // Отмаппить различные исходные данные
      val sourceData = hit.getFields.map { case (name:String, field) =>
        val fieldValue = field.getValue[String]
        name match {
          case FIELD_IMAGE_ID  => FIELD_THUMB_REL_URL -> routes.Thumb.getThumb(options.dkey, fieldValue).toString
          case _               => name -> fieldValue
        }
      }
      // Мержим два словаря, отдавая предпочтение подсвеченным элементам
      Lists
        .mergeMutableMaps(sourceData, hlData) { (_, _, v) => v }
        .filter { case (k:String, v) => resultAllowedFields.contains(k) }
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
          if(map.get(FIELD_IMAGE_ID).isDefined) {
            map.remove(FIELD_CONTENT_TEXT)
          }
          map
        }
      }
    }
    removeFieldOptF(FIELD_TITLE, options.retTitle)
    removeFieldOptF(FIELD_CONTENT_TEXT, options.retContentText)

    // Конечный словарь полей результатов оборачиваем в классы-хелперы для упрощения работы с ними на верхнем уровне.
    val mapHitF1 = mapHitF andThen { new SioSearchResult(_) }

    val result = hits.toList.map(mapHitF1)
    result
  }


  /**
   * Сколько анализировать документов на шарду макс.
   * @param query строка поиска
   * @return
   */
  def queryShardLimit(query:String) = {
    query.length match {
      case 0 => 0
      case 1 => 100
      case 2 => 200
      case 3 => 400
      case 5 => 600
      case 6 => 800
      case _ => PER_SHARD_DOCUMENTS_LIMIT
    }
  }


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
          val minShouldMatch = 1
          val boolQB = QueryBuilders.boolQuery().minimumNumberShouldMatch(minShouldMatch)
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
  // Данные состояния реквеста
  dkey : String,

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
case class SioSearchResult(data:mutable.Map[String,String]) {

  def title       = data.get(FIELD_TITLE)
  def contentText = data.get(FIELD_CONTENT_TEXT)
  def thumbRelUrl = data.get(SiowebEsUtil.FIELD_THUMB_REL_URL)
  def url         = data(FIELD_URL)
  def lang        = data.get(FIELD_LANGUAGE)

}

