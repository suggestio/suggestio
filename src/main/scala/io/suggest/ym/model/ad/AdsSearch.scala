package io.suggest.ym.model.ad

import io.suggest.ym.model._
import org.elasticsearch.index.query.{FilterBuilder, QueryBuilder, FilterBuilders, QueryBuilders}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.util.text.TextQueryV2Util
import io.suggest.ym.model.common.EMProducerId.PRODUCER_ID_ESFN
import io.suggest.ym.model.common.EMUserCatId.USER_CAT_ID_ESFN
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.model.EsModelStaticT
import io.suggest.ym.model.common.{SlNameTokenStr, DynSearchArgs, EsDynSearchStatic, EMReceivers}
import io.suggest.util.SioConstants
import io.suggest.util.SioRandom.rnd
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import scala.collection.JavaConversions._
import java.{util => ju}

/** Статичная утиль для генерации поисковых ES-запросов. */
object AdsSearch {

  // TODO MVEL-скрипты выпиливаются в es-1.4.0. Нужно обновится до es-1.3.0 и заюзать groovy.

  /** MVEL-код для инкремента скора в incr раз, если id документа содержится в переданной коллекции ids.
    * Поле _id не доступно в хранимых MAd-документах, поэтому нужно извлекать его из _uid ручками.
    * o.es.index.mapper.Uid тоже не доступен, поэтому тупо вытаскиваем _id из _uid путём отрезания по символу #.
    * @see [[http://stackoverflow.com/a/15539093]]
    */
  // TODO Оно кажется не работает совсем.
  lazy val IDS_SCORE_MVEL =
    """
      |uid = doc["_uid"].value;
      |dInx = uid.indexOf('#');
      |id = uid.substring(dInx + 1);
      |if (ids contains id) { incr; } else { 1.0; }
      |""".stripMargin

  /**
   * MVEL-скрипт Generation-timestamp сортировщика, который организует посраничный вывод с внешне рандомной выдачей.
   * gents - таймштамп поколения исходной выдачи.
   */
  val GENTS_SORTER_MVEL = """(_score + 1) * (doc["_uid"].value.hashCode() / generation)"""

  /**
   * Скомпилировать из аргументов запроса сам ES-запрос со всеми фильтрами и т.д.
   * @param adSearch Аргументы запроса.
   * @return
   */
  def prepareEsQuery(adSearch: AdsSearchArgsT): QueryBuilder = {
    import adSearch._
    // Собираем запрос в функциональном стиле, иначе получается многовато вложенных if-else.
    var query3: QueryBuilder = adSearch.qOpt.flatMap[QueryBuilder] { q =>
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
      if (receiverIds.nonEmpty || levels.nonEmpty) {
        var nestedSubfilters: List[FilterBuilder] = Nil
        if (receiverIds.nonEmpty) {
          nestedSubfilters ::= FilterBuilders.termsFilter(EMReceivers.RCVRS_RECEIVER_ID_ESFN, receiverIds: _*)
        }
        if (levels.nonEmpty) {
          nestedSubfilters ::= FilterBuilders.termsFilter(EMReceivers.RCVRS_SLS_ESFN, levels.map(_.name) : _*)
        }
        // Если получилось несколько фильтров, то надо их объеденить.
        val finalNestedSubfilter: FilterBuilder = if (nestedSubfilters.tail.nonEmpty) {
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
        if (levels.nonEmpty) {
          val levelFilter = FilterBuilders.termsFilter(EMReceivers.RCVRS_SLS_ESFN, levels.map(_.name) : _*)
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
        val levelQuery = QueryBuilders.termsQuery(EMReceivers.SLS_ESFN, levels.map(_.name) : _*)
        val qb = QueryBuilders.nestedQuery(EMReceivers.RECEIVERS_ESFN, levelQuery)
        Some(qb)
      }
    }.getOrElse[QueryBuilder] {
      // Сборка реквеста не удалась вообще: все параметры не заданы. Просто возвращаем все объявы в рамках индекса.
      // Нужно фильтровать только отображаемые где-либо.
      val q0 = QueryBuilders.matchAllQuery()
      val f = FilterBuilders.existsFilter(EMReceivers.RCVRS_SLS_ESFN)
      val nf = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, f)
      QueryBuilders.filteredQuery(q0, nf)
    }
    if (adSearch.generation.isDefined && adSearch.qOpt.isEmpty) {
      // Можно и нужно сортировтать с учётом genTs. Точный скоринг не нужен, поэтому просто прикручиваем скипт для скоринга.
      val scoreFun = ScoreFunctionBuilders.scriptFunction(GENTS_SORTER_MVEL, "mvel")
        .param("generation", java.lang.Long.valueOf( Math.abs(adSearch.generation.get) ))
      query3 = QueryBuilders.functionScoreQuery(query3, scoreFun)
    }
    // Если указаны id-шники, которые должны быть в начале выдачи, то добавить обернуть всё в ипостась Custom Score Query.
    if (adSearch.forceFirstIds.nonEmpty) {
      // Запрошено, чтобы указанные id были в начале списка результатов.
      val scoreFun = ScoreFunctionBuilders.scriptFunction(IDS_SCORE_MVEL, "mvel")
        .param("ids", new ju.HashSet[String](adSearch.forceFirstIds.size).addAll(adSearch.forceFirstIds) )  // TODO Opt: использовать java.util.HashSet?
        .param("incr", 100)
      query3 = QueryBuilders.functionScoreQuery(query3, scoreFun)
    }
    // Если включен withoutIds, то нужно обернуть query3 в соответствующий not(ids filter).
    if (withoutIds.nonEmpty) {
      val idsFilter = FilterBuilders.notFilter(
        FilterBuilders.idsFilter().addIds(withoutIds : _*)
      )
      query3 = QueryBuilders.filteredQuery(query3, idsFilter)
    }
    // Возвращаем собранный запрос.
    query3
  }

}


/** Интерфейс для передачи параметров поиска объявлений в индексе/типе. */
trait AdsSearchArgsT extends DynSearchArgs {
  // Ниже Seq брать нельзя, т.к. используется vararhs конструкция вида (x : _*), которая требует Seq[T].

  /** id "получателя" рекламы, т.е. id ТЦ, ресторана и просто поискового контекста. */
  def receiverIds: Seq[String]

  /** Необязательный id владельца рекламы. Полезно при поиске в рамках магазина. */
  def producerIds: Seq[String]

  /** Необязательный id категории */
  def catIds: Seq[String]

  /** Какого уровня требуются карточки. */
  def levels: Seq[SlNameTokenStr]

  /** Произвольный текстовый запрос, если есть. */
  def qOpt: Option[String]

  /** Форсировать указанные id в начало списка (через мощный скоринг). */
  def forceFirstIds: Seq[String]

  /** Отбрасывать документы, имеющие указанные id'шники. */
  def withoutIds: Seq[String]

  /** Значение Generation timestamp, генерится при первом обращении к выдаче и передаётся при последующих запросах выдачи. */
  def generation: Option[Long]

  override def toEsQuery = AdsSearch.prepareEsQuery(this)
}


/** Трейт враппера над экземпляром [[AdsSearchArgsT]]. */
trait AdsSearchArgsWrapperT extends AdsSearchArgsT {
  def underlying: AdsSearchArgsT
  override def producerIds = underlying.producerIds
  override def maxResults  = underlying.maxResults
  override def forceFirstIds = underlying.forceFirstIds
  override def levels = underlying.levels
  override def offset = underlying.offset
  override def receiverIds = underlying.receiverIds
  override def qOpt = underlying.qOpt
  override def generation = underlying.generation
  override def catIds = underlying.catIds
  override def withoutIds = underlying.withoutIds
}


/** Если нужно добавить в рекламную модель поиск по рекламным карточкам, то следует задействовать вот этот трейт. */
trait AdsSimpleSearchT extends EsDynSearchStatic[AdsSearchArgsT] {

  /** Постпроцессинг результатов поиска рекламных карточек. */
  private def postprocessSearchResults(args: AdsSearchArgsT, resultFut: Future[Seq[T]])(implicit ec: ExecutionContext): Future[Seq[T]] = {
    // TODO Надо бы сделать так, что Seq могла быть и List
    if (args.qOpt.isEmpty) {
      resultFut.map { rnd.shuffle(_) }
    } else {
      resultFut
    }
  }

  /**
   * Поиск карточек в ТЦ по критериям.
   * @return Список рекламных карточек, подходящих под требования.
   */
  override def dynSearch(adSearch: AdsSearchArgsT)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    val resultFut = super.dynSearch(adSearch)
    postprocessSearchResults(adSearch, resultFut)
  }

  /**
   * Посчитать кол-во рекламных карточек, подходящих под запрос.
   * @param adSearch Экземпляр, описывающий поисковый запрос.
   * @return Фьючерс с кол-вом совпадений.
   */
  override def dynCount(adSearch: AdsSearchArgsT)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    // Необходимо выкинуть из запроса ненужные части.
    val adSearch2 = new AdsSearchArgsWrapperT {
      override def underlying = adSearch
      override def generation = None
      override def forceFirstIds = Nil
    }
    super.dynCount(adSearch2)
  }

  override def dynSearchRt(adSearch: AdsSearchArgsT)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    val resultFut = super.dynSearchRt(adSearch)
    postprocessSearchResults(adSearch, resultFut)
  }

}
