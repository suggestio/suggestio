package io.suggest.ym.model.ad

import io.suggest.ym.model._
import org.elasticsearch.action.search.SearchResponse
import io.suggest.model.inx2.MInxT
import org.elasticsearch.index.query.{FilterBuilder, QueryBuilder, FilterBuilders, QueryBuilders}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.util.text.TextQueryV2Util
import io.suggest.ym.model.common.EMProducerId.PRODUCER_ID_ESFN
import io.suggest.ym.model.common.EMUserCatId.USER_CAT_ID_ESFN
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.model.EsModelMinimalStaticT
import io.suggest.ym.model.common.EMReceivers
import io.suggest.util.SioConstants

/** Статичная утиль для генерации поисковых ES-запросов. */
object AdsSearch {

  /**
   * Скомпилировать из аргументов запроса сам ES-запрос со всеми фильтрами и т.д.
   * @param adSearch Аргументы запроса.
   * @return
   */
  def prepareEsQuery(adSearch: AdsSearchArgsT): QueryBuilder = {
    import adSearch._
    // Собираем запрос в функциональном стиле, иначе получается многовато вложенных if-else.
    adSearch.qOpt.flatMap[QueryBuilder] { q =>
      // Собираем запрос текстового поиска.
      // TODO Для коротких запросов следует искать по receiverId и фильтровать по qStr (query-filter + match-query).
      TextQueryV2Util.queryStr2QueryMarket(q, s"${SioConstants.FIELD_ALL}")
        .map { _.q }
    } map { qstrQB =>
      // Если producerId задан, то навешиваем ещё фильтр.
      producerIdOpt.fold(qstrQB) { producerId =>
        val shopIdFilter = FilterBuilders.termFilter(PRODUCER_ID_ESFN, producerId)
        QueryBuilders.filteredQuery(qstrQB, shopIdFilter)
      }
    } orElse {
      // Всё ещё не удалось собрать поисковый запрос. Если задан shopId, то собираем query по магазину.
      producerIdOpt.map { producerId =>
        QueryBuilders.termQuery(PRODUCER_ID_ESFN, producerId)
      }
    } map { qb =>
      // Если есть q или shopId и указана catId, то добавляем catId-фильтр.
      catIdOpt.fold(qb) { catId =>
        val catIdFilter = FilterBuilders.termFilter(USER_CAT_ID_ESFN, catId)
        QueryBuilders.filteredQuery(qb, catIdFilter)
      }
    } orElse {
      // Запроса всё ещё нет, т.е. собрать запрос по shopId тоже не удалось. Пробуем собрать запрос с catIdOpt...
      catIdOpt.map { catId =>
        QueryBuilders.termQuery(USER_CAT_ID_ESFN, catId)
      }
    } map { qb =>
      // Если receiverId задан, то надо фильтровать в рамках ресивера. Сразу надо уровни отработать, т.к. они nested в одном поддокументе.
      if (receiverIdOpt.isDefined || levelOpt.isDefined) {
        var nestedSubfilters: List[FilterBuilder] = Nil
        if (receiverIdOpt.isDefined)
          nestedSubfilters ::= FilterBuilders.termFilter(EMReceivers.RCVRS_RECEIVER_ID_ESFN, receiverIdOpt.get)
        if (levelOpt.isDefined)
          nestedSubfilters ::= FilterBuilders.termFilter(EMReceivers.RCVRS_SLS_PUB_ESFN, levelOpt.get)
        // Если получилось несколько фильтров, то надо их объеденить.
        val finalNestedSubfilter: FilterBuilder = if (!nestedSubfilters.tail.isEmpty)
          FilterBuilders.andFilter(nestedSubfilters : _*)
        else
          nestedSubfilters.head
        // Оборачиваем результирующий фильтр в nested, и затем вешаем на исходную query.
        val nestedFilter = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, finalNestedSubfilter)
        QueryBuilders.filteredQuery(qb, nestedFilter)
      } else {
        qb
      }
    } orElse {
      // Нет поискового запроса. Попытаться собрать запрос по ресиверу с опциональным фильтром по level.
      receiverIdOpt.map { receiverId =>
        var nestedSubquery: QueryBuilder = QueryBuilders.termQuery(EMReceivers.RCVRS_RECEIVER_ID_ESFN, receiverId)
        if (levelOpt.isDefined) {
          val levelFilter = FilterBuilders.termFilter(EMReceivers.RCVRS_SLS_PUB_ESFN, levelOpt.get)
          nestedSubquery = QueryBuilders.filteredQuery(nestedSubquery, levelFilter)
        }
        QueryBuilders.nestedQuery(EMReceivers.RECEIVERS_ESFN, nestedSubquery)
      }
    } orElse {
      // Сборка запроса по receiverId тоже не удалась. Просто ищем по уровням.
      levelOpt map { level =>
        val levelQuery = QueryBuilders.termQuery(EMReceivers.SLS_PUB_ESFN, level.toString)
        QueryBuilders.nestedQuery(EMReceivers.RECEIVERS_ESFN, levelQuery)
      }
    } getOrElse {
      // Сборка реквеста не удалась вообще: все параметры не заданы. Просто возвращаем все объявы в рамках индекса.
      QueryBuilders.matchAllQuery()
    }
  }

}


/** Интерфейс для передачи параметров поиска объявлений в индексе/типе. */
trait AdsSearchArgsT {
  /** id "получателя" рекламы, т.е. id ТЦ, ресторана и просто поискового контекста. */
  def receiverIdOpt: Option[String]

  /** Необязательный id владельца рекламы. Полезно при поиске в рамках магазина. */
  def producerIdOpt: Option[String]

  /** Необязательный id категории */
  def catIdOpt: Option[String]

  /** Какого уровня требуются карточки. */
  def levelOpt: Option[AdShowLevel]

  /** Произвольный текстовый запрос, если есть. */
  def qOpt: Option[String]

  /** Макс.кол-во результатов. */
  def maxResults: Int

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int
}


/** Если нужно добавить в рекламную модель поиск по рекламным карточкам, то следует задействовать вот этот трейт. */
trait AdsSimpleSearchT extends EsModelMinimalStaticT {

  /**
   * Поиск карточек в ТЦ по критериям.
   * @return Список рекламных карточек, подходящих под требования.
   */
  def searchAds(adSearch: AdsSearchArgsT)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    val query = AdsSearch.prepareEsQuery(adSearch)
    // Запускаем собранный запрос.
    prepareSearch
      .setQuery(query)
      .setSize(adSearch.maxResults)
      .setFrom(adSearch.offset)
      .execute()
      .map { searchResp2list }
  }

}
