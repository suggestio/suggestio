package io.suggest.ym.model.ad

import io.suggest.ym.model._
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
import io.suggest.util.SioRandom.rnd

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
    }.map { qstrQB =>
      // Если producerId задан, то навешиваем ещё фильтр сверху.
      if (producerIds.isEmpty) {
        qstrQB
      } else {
        val shopIdFilter = FilterBuilders.termsFilter(PRODUCER_ID_ESFN, producerIds : _*)
        QueryBuilders.filteredQuery(qstrQB, shopIdFilter)
      }
    }.orElse[QueryBuilder] {
      // Всё ещё не удалось собрать поисковый запрос. Если задан shopId, то собираем query по магазину.
      if (producerIds.isEmpty) {
        None
      } else {
        val qb = QueryBuilders.termsQuery(PRODUCER_ID_ESFN, producerIds : _*)
        Some(qb)
      }
    }.map { qb =>
      // Если есть q или shopId и указана catId, то добавляем catId-фильтр.
      if (catIds.isEmpty) {
        qb
      } else {
        val catIdFilter = FilterBuilders.termsFilter(USER_CAT_ID_ESFN, catIds : _*)
        QueryBuilders.filteredQuery(qb, catIdFilter)
      }
    }.orElse[QueryBuilder] {
      // Запроса всё ещё нет, т.е. собрать запрос по shopId тоже не удалось. Пробуем собрать запрос с catIdOpt...
      if (catIds.isEmpty) {
        None
      } else {
        val qb = QueryBuilders.termsQuery(USER_CAT_ID_ESFN, catIds : _*)
        Some(qb)
      }
    }.map { qb =>
      // Если receiverId задан, то надо фильтровать в рамках ресивера. Сразу надо уровни отработать, т.к. они nested в одном поддокументе.
      if (!receiverIds.isEmpty || !levels.isEmpty) {
        var nestedSubfilters: List[FilterBuilder] = Nil
        if (!receiverIds.isEmpty) {
          nestedSubfilters ::= FilterBuilders.termsFilter(EMReceivers.RCVRS_RECEIVER_ID_ESFN, receiverIds: _*)
        }
        if (!levels.isEmpty) {
          nestedSubfilters ::= FilterBuilders.termsFilter(EMReceivers.RCVRS_SLS_PUB_ESFN, levels.map(_.toString) : _*)
        }
        // Если получилось несколько фильтров, то надо их объеденить.
        val finalNestedSubfilter: FilterBuilder = if (!nestedSubfilters.tail.isEmpty) {
          FilterBuilders.andFilter(nestedSubfilters: _*)
        } else {
          nestedSubfilters.head
        }
        // Оборачиваем результирующий фильтр в nested, и затем вешаем на исходную query.
        val nestedFilter = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, finalNestedSubfilter)
        QueryBuilders.filteredQuery(qb, nestedFilter)
      } else {
        qb
      }
    }.orElse[QueryBuilder] {
      // Нет поискового запроса. Попытаться собрать запрос по ресиверу с опциональным фильтром по level.
      if (receiverIds.isEmpty) {
        None
      } else {
        var nestedSubquery: QueryBuilder = QueryBuilders.termsQuery(EMReceivers.RCVRS_RECEIVER_ID_ESFN, receiverIds : _*)
        if (!levels.isEmpty) {
          val levelFilter = FilterBuilders.termsFilter(EMReceivers.RCVRS_SLS_PUB_ESFN, levels.map(_.toString) : _*)
          nestedSubquery = QueryBuilders.filteredQuery(nestedSubquery, levelFilter)
        }
        val qb = QueryBuilders.nestedQuery(EMReceivers.RECEIVERS_ESFN, nestedSubquery)
        Some(qb)
      }
    }.orElse[QueryBuilder] {
      // Сборка запроса по receiverId тоже не удалась. Просто ищем по уровням.
      if (levels.isEmpty) {
        None
      } else {
        val levelQuery = QueryBuilders.termsQuery(EMReceivers.SLS_PUB_ESFN, levels.map(_.toString) : _*)
        val qb = QueryBuilders.nestedQuery(EMReceivers.RECEIVERS_ESFN, levelQuery)
        Some(qb)
      }
    }.getOrElse[QueryBuilder] {
      // Сборка реквеста не удалась вообще: все параметры не заданы. Просто возвращаем все объявы в рамках индекса.
      QueryBuilders.matchAllQuery()
    }
  }

}


/** Интерфейс для передачи параметров поиска объявлений в индексе/типе. */
trait AdsSearchArgsT {
  // Ниже Seq брать нельзя, т.к. используется vararhs конструкция вида (x : _*), которая требует Seq[T].

  /** id "получателя" рекламы, т.е. id ТЦ, ресторана и просто поискового контекста. */
  def receiverIds: Seq[String]

  /** Необязательный id владельца рекламы. Полезно при поиске в рамках магазина. */
  def producerIds: Seq[String]

  /** Необязательный id категории */
  def catIds: Seq[String]

  /** Какого уровня требуются карточки. */
  def levels: Seq[AdShowLevel]

  /** Произвольный текстовый запрос, если есть. */
  def qOpt: Option[String]

  /** Макс.кол-во результатов. */
  def maxResults: Int

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int
}


/** Если нужно добавить в рекламную модель поиск по рекламным карточкам, то следует задействовать вот этот трейт. */
trait AdsSimpleSearchT extends EsModelMinimalStaticT {

  def searchAdsReqBuilder(adSearch: AdsSearchArgsT)(implicit ec:ExecutionContext, client: Client) = {
    val query = AdsSearch.prepareEsQuery(adSearch)
    // Запускаем собранный запрос.
    prepareSearch
      .setQuery(query)
      .setSize(adSearch.maxResults)
      .setFrom(adSearch.offset)
  }

  /** Постпроцессинг результатов поиска рекламных карточек. */
  private def postprocessSearchResults(adSearch: AdsSearchArgsT, resultFut: Future[Seq[T]])(implicit ec: ExecutionContext): Future[Seq[T]] = {
    // TODO Надо бы сделать так, что Seq могла быть и List
    if (adSearch.qOpt.isEmpty) {
      resultFut.map { rnd.shuffle(_) }
    } else {
      resultFut
    }
  }

  /**
   * Поиск карточек в ТЦ по критериям.
   * @return Список рекламных карточек, подходящих под требования.
   */
  def searchAds(adSearch: AdsSearchArgsT)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    val resultFut = searchAdsReqBuilder(adSearch)
      .execute()
      .map { searchResp2list }
    postprocessSearchResults(adSearch, resultFut)
  }

  def searchAdsRt(adSearch: AdsSearchArgsT)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    val resultFut = searchAdsReqBuilder(adSearch)
      .setNoFields()
      .execute()
      .flatMap { searchResp2RtMultiget }
    postprocessSearchResults(adSearch, resultFut)
  }

}
