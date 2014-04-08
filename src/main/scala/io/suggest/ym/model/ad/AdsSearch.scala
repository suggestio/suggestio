package io.suggest.ym.model.ad

import io.suggest.ym.model.MShop._
import io.suggest.ym.model._
import org.elasticsearch.action.search.SearchResponse
import io.suggest.model.inx2.MInxT
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilders, QueryBuilders}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.util.text.TextQueryV2Util
import io.suggest.ym.model.common.EMProducerId.PRODUCER_ID_ESFN
import io.suggest.ym.model.common.EMUserCatId.USER_CAT_ID_ESFN
import io.suggest.ym.model.common.EMShowLevels.SHOW_LEVELS_ESFN
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.model.{EsModelMinimalT, EsModelMinimalStaticT, EsModelStaticT}

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
    adSearch.qOpt.flatMap { q =>
      TextQueryV2Util.queryStr2QueryMarket(q)
    } map { qstrQB =>
      // Если shopId задан, то навешиваем фильтр.
      producerIdOpt.fold(qstrQB) { producerId =>
        val shopIdFilter = FilterBuilders.termFilter(PRODUCER_ID_ESFN, producerId)
        QueryBuilders.filteredQuery(qstrQB, shopIdFilter)
      }
    } orElse {
      // Не удалось собрать текстовый запрос. Если задан shopId, то собираем query по магазину.
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
      // Добавить фильтрацию по уровню, если он указан.
      levelOpt.fold(qb) { level =>
        val levelFilter = FilterBuilders.termFilter(SHOW_LEVELS_ESFN, level.toString)
        QueryBuilders.filteredQuery(qb, levelFilter)
      }
    } orElse {
      // Сборка запроса по catId тоже не удалась. Просто ищем по уровню:
      levelOpt map { level =>
        QueryBuilders.termQuery(SHOW_LEVELS_ESFN, level.toString)
      }
    } getOrElse {
      // Сборка реквеста не удалась: все параметры не заданы. Просто возвращаем все объявы в рамках индекса.
      QueryBuilders.matchAllQuery()
    } 
  }

}


/** Интерфейс для передачи параметров поиска объявлений в индексе/типе. */
trait AdsSearchArgsT {
  /** Необязательный id магазина. */
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


/** Если нужно добавить в рекламную inx2-модель поиск по рекламным карточкам,
  * то следует задействовать вот этот трейт. */
@deprecated("AdsSearch trait used only by deprecated models. Should be removed after cleanup.", "2014.apr.07")
trait AdsSearchT[T, InxT <: MInxT] {
  
  def searchResp2list(searchResp: SearchResponse, inx2: InxT): Seq[T]
  
  /**
   * Поиск карточек в ТЦ по критериям.
   * @param inx2 Метаданные об индексе ТЦ.
   * @return Список рекламных карточек, подходящих под требования.
   */
  def searchAds(inx2: InxT, adSearch: AdsSearchArgsT)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    val query = AdsSearch.prepareEsQuery(adSearch)
    // Запускаем собранный запрос.
    inx2.prepareSearchIn
      .setQuery(query)
      .setSize(adSearch.maxResults)
      .setFrom(adSearch.offset)
      .execute()
      .map { searchResp2list(_, inx2) }
  }
  
}



/** Если нужно добавить в рекламную inx2-модель поиск по рекламным карточкам,
  * то следует задействовать вот этот трейт. */
trait AdsSimpleSearchT[T <: EsModelMinimalT[T]] extends EsModelMinimalStaticT[T] {

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