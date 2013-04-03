package util

import org.elasticsearch.node.NodeBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.{FilterBuilder, FilterBuilders, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.13 17:54
 * Description: Фунцкии для поддержки работы c ES на стороне веб-морды. В частности,
 * генерация запросов и поддержка инстанса клиента.
 */

object SiowebEsUtil {

  val RESULT_COUNT = 20

  // Настройки ret*-полей опций выдачи.
  type RET_T = Symbol
  val RET_ALWAYS      : RET_T = 'a
  val RET_NEVER       : RET_T = 'n
  val RET_IF_NO_IMAGE : RET_T = 'i

  val PER_SHARD_DOCUMENTS_LIMIT = 500

  val FIELD_LANG  = "lang"
  val FIELD_TITLE = "title"


  // Инстанс локальной не-data ноды ES. Отсюда начинаются все поисковые и другие запросы.
  // TODO стоит это вынести это в отдельный актор? Нет при условии вызова node.close() при остановке системы.
  implicit val client = NodeBuilder.nodeBuilder().client(true).node.client()


  /**
   * Фунция генерации поискового запроса и выполнения поиска. Используется query-генератор, горождаемый ES-клиентом.
   * @param indices Список индексов, по которым будет идти поиск.
   * @param types Список типов, которые затрагиваются поиском.
   * @param query Буквы, которые ввел юзер
   * @param options Параметры запроса.
   */
  def searchIndex(indices:Seq[String], types:Seq[String], query:String, options:SioSearchOptions)(implicit client:Client) = {
    var filters : List[FilterBuilder] = List(FilterBuilders.limitFilter( queryShardLimit(query) ))

    // Обработать options.langs, дописав при необходимости дополнительный фильтр.
    if (!options.langs.isEmpty) {
      val langs = options.langs
      val termQuery = if (langs.tail.isEmpty)
        // Один язык - делаем простой term-фильтр
        QueryBuilders.termQuery(FIELD_LANG, langs.head)
      else
        // Запрошено несколько языков. Используем terms-query
        QueryBuilders.termsQuery(FIELD_LANG, langs: _*)
      filters = FilterBuilders.queryFilter(termQuery) :: filters
    }

    // Если получилось несколько фильтров, то нужно их объеденить через and-фильтр.
    val qfilter = if (!filters.tail.isEmpty)
      FilterBuilders.andFilter(filters : _*)
    else
      filters.head

    // TODO
    // TODO разрезать запрос, собрать дальше query
    // TODO

    val reqBuilder = client
      .prepareSearch(indices : _*)
      .setTypes(types : _*)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    reqBuilder
      .setQuery(QueryBuilders.termQuery("multi", "test"))
      .setFilter(FilterBuilders.rangeFilter("age").from(12).to(18))
      .setFrom(0)
      .setSize(RESULT_COUNT)
      .setExplain(true)
      .execute()
      .actionGet()
  }


  /**
   * Сколько анализировать документов на шарду макс.
   * @param query строка поиска
   * @return
   */
  def queryShardLimit(query:String) = PER_SHARD_DOCUMENTS_LIMIT


}


case class SioSearchOptions(
  // Настройки выдачи результатов
  retTitle : SiowebEsUtil.RET_T = SiowebEsUtil.RET_ALWAYS,
  retContentText : SiowebEsUtil.RET_T = SiowebEsUtil.RET_ALWAYS,
  retImage : SiowebEsUtil.RET_T = SiowebEsUtil.RET_ALWAYS,
  // Языки, по которым стоит искать
  langs : List[String] = List(),
  fields : List[String] = List(),
  // Подсветка результатов
  hlCtFragmentsCount : Int = 1,
  hlCtFragmentsSize : Int = 200,
  hlCtFragmentSeparator : String = "",
  // Дебажные настройки всякие
  use_date_scoring : Boolean = true
)