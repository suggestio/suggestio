package util

import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilder, FilterBuilders, QueryBuilders}
import org.elasticsearch.search.SearchHit
import io.suggest.model._
import scala.collection.JavaConversions._
import collection.mutable
import io.suggest.util.{UrlUtil, SioConstants, SioEsClient, LogsImpl}
import io.suggest.util.SioConstants._
import io.suggest.util.SioEsUtil.laFuture2sFuture
import controllers.routes
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import play.api.Play.{current, configuration}
import play.api.libs.json._
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.facet.terms.TermsFacet

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.13 17:54
 * Description: Фунцкии для поддержки работы c ES на стороне веб-морды. В частности,
 * генерация запросов и поддержка инстанса клиента.
 * Тут по сути нарезка необходимого из sio_elastic_search, sio_lucene_query, sio_m_page_es.
 */

object SiowebEsUtil extends SioEsClient {

  val RESULT_COUNT_DEFAULT = configuration.getInt("search.results.count.default") getOrElse 20

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  // Настройки ret*-полей опций выдачи. TODO Переписать через enum
  type RET_T = Char
  val RET_ALWAYS      : RET_T = 'a'
  val RET_NEVER       : RET_T = 'n'
  val RET_IF_NO_IMAGE : RET_T = 'i'

  val FACET_INVLINK_NAME = "tags"

  // Макс.число документов, анализируемых на совпадение после чтения из шарды
  val PER_SHARD_DOCUMENTS_LIMIT = 1800

  val FIELDS_ONLY_TITLE = List(FIELD_TITLE)
  val FIELDS_TEXT_ALL   = List(FIELD_TITLE, FIELD_CONTENT_TEXT)
  val FIELD_THUMB_REL_URL = "image_rel_url"

  // Только эти поля могут быть в карте результата
  val resultAllowedFields = Set(FIELD_URL, FIELD_TITLE, FIELD_CONTENT_TEXT, FIELD_LANGUAGE, FIELD_THUMB_REL_URL, FIELD_PAGE_TAGS)

  val hlFragSepDefault = " "

  /** Имя кластера elasticsearch, к которому будет коннектиться клиент. */
  override def getEsClusterName = configuration.getString("es.cluster.name") getOrElse super.getEsClusterName


  /**
   * Поиск в рамках домена.
   * @param sso Настройки поиска по мнению sioweb21
   * @return Фьючерс с результатом searchIndex или с экзепшеном.
   */
  def searchDkeys(implicit sso: SioSearchOptions): Future[SioSearchResult] = {
    import sso.{dkeys, queryStr}
    lazy val logPrefix = s"searchDomain(${dkeys.mkString(",")}, $queryStr): "
    trace(logPrefix + "Planning search request...")
    // TODO потом нужно добавить поддержку переключения searchPtr в каком-либо виде.
    Future.traverse(dkeys) { dkey =>
      MDVIActive.getStableForDkey(dkey).map {
        case Some(mdviActive) =>
          // Есть указатель поиска для домена. Возможно, их там несколько, и если так, то нужно выбрать current-указатель, т.е. старейший из них.
          val indices = mdviActive.getShards
          val types = mdviActive.getTypesForRequest(sso)
          trace(logPrefix + s"found index: $mdviActive :: indices -> ${indices.mkString(", ")} ;; types -> $types")
          Some(indices -> types)

        // Нет индекса для указанного dkey.
        case None =>
          warn(s"No MDVIActive not found for dkey '$dkey'! Suppressing any exception...")
          None
      }
    }
    // Смержить все индексы и типы в два списка. На выходе будет кортеж, который описан аккумулятором: allIndices -> allTypes
    .map { listOfIndicesTypesOpt =>
      val result = listOfIndicesTypesOpt.foldLeft [(List[String], List[String])] (Nil -> Nil) {
        case ((_accIndices, _accTypes), Some((_indices, _types))) =>
          (_accIndices ++ _indices) -> (_accTypes ++ _types)

        case (_acc, None) => _acc
      }
      result
    }
    .flatMap { case (indices, types) =>
      // запустить поиск
      if (indices.isEmpty) {
        // Нет метаданных для выполнения поиска. Скорее всего, запрошенный домен не установлен в системе.
        warn(s"${logPrefix}No indices found for search in dkeys = [${dkeys.mkString(", ")}]. allTypes -> ${types.mkString(", ")}")
        Future.failed(NoSuchDomainException(dkeys))
      } else {
        searchIndex(indices, types, queryStr)
      }
    }
  }


  /**
   * Фунцкия генерации поискового запроса и выполнения поиска. Используется query-DSL из ES-клиента.
   * Отделена от searchDomain для возможности создания произвольного поиска, а не только в рамках домена.
   * @param indices Список индексов, по которым будет идти поиск.
   * @param types Список типов, которые затрагиваются поиском.
   * @param queryStr Буквы, которые ввел юзер
   * @param sso Параметры запроса.
   */
  def searchIndex(indices:Seq[String], types:Seq[String], queryStr:String)(implicit sso: SioSearchOptions): Future[SioSearchResult] = {
    lazy val logPrefix = s"searchIndex(${indices.mkString(",")}, '$queryStr'): "
    trace(logPrefix + s"indices=$indices , types=$types")
    queryStr2QueryV1(queryStr).map { textQuery =>
      // Собираем нужные фильтры в порядке нарастания важности. Сначала идут самые последние.
      var filters : List[FilterBuilder] = {
        // TODO limit должен также зависеть от индекса.
        val limitFilter = FilterBuilders.limitFilter( queryShardLimit(queryStr) )
        List(limitFilter)
      }

      // Обработать options.langs, дописав при необходимости дополнительный фильтр.
      if (!sso.langs.isEmpty) {
        val langs = sso.langs
        val termQuery = if (langs.tail.isEmpty) {
          // Один язык - делаем простой term-фильтр
          QueryBuilders.termQuery(FIELD_LANGUAGE, langs.head)
        } else {
          // Запрошено несколько языков. Используем terms-query
          QueryBuilders.termsQuery(FIELD_LANGUAGE, langs: _*)
        }
        filters ::= FilterBuilders.queryFilter(termQuery)
      }

      // Включена фильтрация по фасетам. Нужно добавить фасетный фильтр.
      // !!! Этот фильтр должен добавлятся последним, т.е. в самое начало списка фильтров. !!!
      if (!sso.fiSearhInTags.isEmpty) {
        val facetTagFilter = FilterBuilders.termsFilter(FIELD_PAGE_TAGS, sso.fiSearhInTags : _*)
        filters ::= facetTagFilter
      }

      // Если получилось несколько фильтров, то нужно их объеденить через and-фильтр.
      val qFilter = if (!filters.tail.isEmpty) {
        FilterBuilders.andFilter(filters : _*)
      } else {
        filters.head
      }

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
        .setExplain(sso.isDebug)
        .setSize(sso.size)
        .setQuery(query)
        .addFields(FIELD_URL, FIELD_PAGE_TAGS, FIELD_DKEY)

      // Если выставлен флаг возвращения фасетов, сделать это.
      if (sso.fiOut) {
        val facetBuilder = FacetBuilders.termsFacet(FACET_INVLINK_NAME)
          .field(FIELD_PAGE_TAGS)
          .global(sso.fiOutGlobal)
        reqBuilder.addFacet(facetBuilder)
      }

      // Добавить доролнительные поля для запроса, если такие есть.
      if (!sso.fields.isEmpty) {
        reqBuilder.addFields(sso.fields : _*)
      }

      // Настраиваем выдачу заголовков и их подсветку
      if (sso.retTitle != RET_NEVER) {
        val hlField = subfield(FIELD_TITLE, SUBFIELD_ENGRAM)
        reqBuilder
          .addField(FIELD_TITLE)
          .addHighlightedField(hlField, -1, 0)
      }

      // Настраиваем выдачу текста страницы.
      if (sso.retContentText != RET_NEVER) {
        val hlField = subfield(FIELD_CONTENT_TEXT, SUBFIELD_ENGRAM)
        reqBuilder.addHighlightedField(
          hlField,
          sso.hlCtFragmentsSize,
          sso.hlCtFragmentsCount
        )
      }

      // Возвращать картинки?
      if (sso.retImage)
        reqBuilder.addField(FIELD_IMAGE_ID)

      // Выполнить асинхронный запрос.
      reqBuilder
        .execute()
        .map { esResp =>
          SioSearchResult(
            hits   = postprocessSearchHits(esResp.getHits.getHits),
            fiTags = esResp.getFacets.facet(classOf[TermsFacet], FACET_INVLINK_NAME).getEntries.toSeq.map {t  =>  t.getTerm.string -> t.getCount}
          )
        }

    } getOrElse {
      val ex = EmptySearchQueryException(sso.dkeys, queryStr)
      Future.failed(ex)
    }
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
   * @param sso опции, которые, в частности, описывают параметры выдачи.
   */
  def postprocessSearchHits(hits:Array[SearchHit])(implicit sso:SioSearchOptions) : List[SioSearchResultHit] = {
    // Собрать функцию маппинга одного словаря результата
    var mapHitF = { hit: SearchHit =>
      val resultMap = new mutable.ListMap[String, AnyRef]
      val hitFields = hit.getFields
      lazy val hitDkey = Option(hitFields.get(FIELD_DKEY)) map(_.getValue[String]) getOrElse {
        val url = hitFields.get(FIELD_URL).getValue[String]
        info(s"postprocessSearchHits(): 'dkey' field expected, but not found for doc inx=${hit.getIndex} type=${hit.getType} id=${hit.getId} url=$url")
        UrlUtil.url2dkey(url)
      }
      hit.getFields.foreach {
        case (FIELD_IMAGE_ID, field) =>
          val imageId = field.getValue[String]
          resultMap(FIELD_THUMB_REL_URL) = routes.Img.getThumb(hitDkey, imageId).toString()

        case (FIELD_PAGE_TAGS, fieldValue) =>
          val pageTags = fieldValue.getValue[java.util.List[String]]
          if (!pageTags.isEmpty) {
            resultMap(FIELD_PAGE_TAGS) = pageTags
          }

        case (name, field) =>
          resultMap(name) = field.getValue[AnyRef]
      }
      // Накатить сверху highlighted-данные
      hit.highlightFields.foreach { case (key, hl) =>
        // убрать возможные суффиксы .fts и .gram из имён подсвеченных полей
        val key1 = rmSubfieldSuffixRe.replaceFirstIn(key, "")
        val hlText = hl.getFragments
          .toSeq
          .map { textFragment => moveHlTags(textFragment.string()) }
          .mkString(sso.hlCtFragmentSeparator)
        resultMap(key1) = hlText
      }
      // Отмаппить различные исходные данные

      // Мержим два словаря, отдавая предпочтение подсвеченным элементам
      resultMap.filter { case (k, _) => resultAllowedFields.contains(k) }
    }

    // Возможно, нужно снести убрать content_text или title. Тут функция, обновляющая mapHitF если нужно что-то ещё делать.
    def removeFieldOptF(fieldName:String, ret:RET_T) = {
      if (ret == RET_NEVER) {
        mapHitF = mapHitF andThen { map =>
          map.remove(fieldName)
          map
        }

      } else if (ret == RET_IF_NO_IMAGE && sso.retImage) {
        // Или сносить только там, где есть картинка
        mapHitF = mapHitF andThen { map =>
          if(map.get(FIELD_IMAGE_ID).isDefined) {
            map.remove(FIELD_CONTENT_TEXT)
          }
          map
        }
      }
    }
    removeFieldOptF(FIELD_TITLE, sso.retTitle)
    removeFieldOptF(FIELD_CONTENT_TEXT, sso.retContentText)

    // Конечный словарь полей результатов оборачиваем в классы-хелперы для упрощения работы с ними на верхнем уровне.
    val mapHitF1 = mapHitF andThen { new SioSearchResultHit(_) }

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
        val result = if (endsWithSpace.matcher(queryStr).find()) {
          (words, "")
        } else {
          ("", words)
        }
        Some(result)

      case Array(fts, ngram:String) =>
        // Бывает, что fts-часть состоит из одного предлога или слова, которое кажется сейчас предлогом.
        // Если отправить в ES в качестве запроса предлог, то, очевидно, будет ноль результатов на выходе.
        val fts1 = if (Stopwords.ALL_STOPS contains ngram) {
          ""
        } else {
          fts
        }
        Some((fts1, ngram))
    }
  }


  /**
   * Взять queryString, вбитую юзером, распилить на куски, проанализировать и сгенерить запрос или комбинацию запросов
   * на её основе.
   * @param queryStr Строка, которую набирает в поиске юзер.
   */
  def queryStr2QueryV1(queryStr: String) : Option[QueryBuilder] = {
    // Дробим исходный запрос на куски
    val topQueriesOpt = splitQueryStr(queryStr).map { case (ftsQS, engramQS) =>

      val ftsLen = ftsQS.length
      val engramLen = engramQS.length

      // Отрабатываем edge-ngram часть запроса.
      var queries : List[QueryBuilder] = if (engramLen == 0) {
        Nil

      } else {
        // Если запрос короткий, то искать только по title
        val fields = if (ftsLen + engramLen <= 1)
          FIELDS_ONLY_TITLE
        else
          FIELDS_TEXT_ALL
        // Генерим базовый engram-запрос
        var queries1 = fields.map { _field =>
          val _subfield = subfield(_field, SUBFIELD_ENGRAM)
          QueryBuilders.matchQuery(_subfield, engramQS)
        }
        // Если чел уже набрал достаточное кол-во символов, то искать парралельно в fts
        if (engramLen >= 4) {
          val ftsQuery = QueryBuilders.matchQuery(FIELD_ALL, engramQS)
          queries1 = ftsQuery :: queries1
        }
        // Если получилось несколько запросов, то обернуть их в bool-query
        val finalEngramQuery = if (queries1.tail == Nil) {
          queries1.head
        } else {
          val minShouldMatch = 1
          val boolQB = QueryBuilders.boolQuery().minimumNumberShouldMatch(minShouldMatch)
          queries1.foreach { boolQB.should }
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
            topQueries.foreach { queryBool.must }
            Some(queryBool)
        }
    }
  }


  /**
   * Взять queryString, вбитую юзером, распилить на куски, проанализировать и сгенерить запрос или комбинацию запросов
   * на её основе. Версия для новых индексов, которые содержат ngram в _all.
   * @param queryStr Строка, которую набирает в поиске юзер.
   */
  def queryStr2QueryV2(queryStr: String) : Option[QueryBuilder] = {
    ??? // TODO Переписать сплиттер и используемые для поиска поля (_all).
    // Дробим исходный запрос на куски
    val topQueriesOpt = splitQueryStr(queryStr).map { case (ftsQS, engramQS) =>

      val ftsLen = ftsQS.length
      val engramLen = engramQS.length

      // Отрабатываем edge-ngram часть запроса.
      var queries : List[QueryBuilder] = if (engramLen == 0) {
        Nil

      } else {
        // Если запрос короткий, то искать только по title
        val fields = if (ftsLen + engramLen <= 1)
          FIELDS_ONLY_TITLE
        else
          FIELDS_TEXT_ALL
        // Генерим базовый engram-запрос
        var queries1 = fields.map { _field =>
          val _subfield = subfield(_field, SUBFIELD_ENGRAM)
          QueryBuilders.matchQuery(_subfield, engramQS)
        }
        // Если чел уже набрал достаточное кол-во символов, то искать парралельно в fts
        if (engramLen >= 4) {
          val ftsQuery = QueryBuilders.matchQuery(FIELD_ALL, engramQS)
          queries1 = ftsQuery :: queries1
        }
        // Если получилось несколько запросов, то обернуть их в bool-query
        val finalEngramQuery = if (queries1.tail == Nil) {
          queries1.head
        } else {
          val minShouldMatch = 1
          val boolQB = QueryBuilders.boolQuery().minimumNumberShouldMatch(minShouldMatch)
          queries1.foreach { boolQB.should }
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
            topQueries.foreach { queryBool.must }
            Some(queryBool)
        }
    }
  }

  protected def subfield(field:String, subfield:String) = field + "." + subfield
}


import SiowebEsUtil._


// Класс, хранящий опции поиска. Ориентирован на работу в режиме аккамулятора настроек, которые втыкаются в разных местах.
final case class SioSearchOptions(
  var queryStr: String,
  var dkeys: List[String] = Nil,
  var domains: Seq[models.MDomain] = Nil,

  // Настройки выдачи результатов
  var retTitle : RET_T = RET_ALWAYS,
  var retContentText : RET_T = RET_ALWAYS,
  var retImage : Boolean = true,
  var size : Int = RESULT_COUNT_DEFAULT,

  // Языки, по которым стоит искать
  var langs  : List[String] = Nil,
  var fields : List[String] = Nil,

  // Подсветка результатов
  var hlCtFragmentsCount : Int = 1,
  var hlCtFragmentsSize : Int = 200,
  var hlCtFragmentSeparator : String = hlFragSepDefault,

  // Фасеты: авто-теги.
  // По каким фасетам фильтровать выдачу? По умолчанию - без них.
  var fiSearhInTags: List[String] = Nil,
  // ES должен считать и выдавать список тегов и их кол-во? http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-facets.html
  var fiOut: Boolean = true,
  var fiOutGlobal: Boolean = false,

  // Дебажные настройки всякие
  var withDateScoring : Boolean = true,
  var isDebug : Boolean = false
) extends SioSearchContext


/** Объект используется для представления результата поиска. */
case class SioSearchResultHit(data: collection.Map[String, AnyRef]) {

  def title       = data.get(FIELD_TITLE).asInstanceOf[Option[String]]
  def contentText = data.get(FIELD_CONTENT_TEXT).asInstanceOf[Option[String]]
  def thumbRelUrl = data.get(FIELD_THUMB_REL_URL).asInstanceOf[Option[String]]
  def url         = data(FIELD_URL).asInstanceOf[String]
  def lang        = data.get(FIELD_LANGUAGE).asInstanceOf[Option[String]]
  def pageTags    = data.get(FIELD_PAGE_TAGS).asInstanceOf[Option[Seq[String]]]

  def toJsValue: JsValue = {
    JsObject(
      data.foldLeft[List[(String, JsValue)]] (Nil) { case (acc, (k, v)) =>
        val jsV = k match {
          case FIELD_TITLE | FIELD_CONTENT_TEXT | FIELD_URL | FIELD_LANGUAGE | FIELD_THUMB_REL_URL =>
            JsString(v.asInstanceOf[String])

          case FIELD_PAGE_TAGS =>
            v match {
              case al: java.util.List[_]  =>  JsArray(al.toSeq.asInstanceOf[Seq[String]].map(JsString))
            }
        }
        k -> jsV  ::  acc
      }
    )
  }
}

case class NoSuchDomainException(dkeys: Seq[String]) extends Exception(s"Domain ${dkeys.mkString(",")} is not installed.")
case class EmptySearchQueryException(dkeys: Seq[String], query: String) extends Exception("Search request is empty.")
case class IndexMdviNotFoundException(dkey: String, vin: String) extends Exception("Internal service error.")

case class SioSearchResult(
  hits: List[SioSearchResultHit],
  fiTags: Seq[(String, Int)] = Nil
)
