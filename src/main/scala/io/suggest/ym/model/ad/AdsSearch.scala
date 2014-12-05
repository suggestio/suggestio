package io.suggest.ym.model.ad

import org.elasticsearch.index.query.{QueryBuilder, FilterBuilders, QueryBuilders}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.util.text.TextQueryV2Util
import io.suggest.ym.model.common.EMUserCatId.USER_CAT_ID_ESFN
import io.suggest.ym.model.common._
import io.suggest.util.SioConstants
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
  // TODO ES 1.4 выкидывает MVEL: нужно выкинуть это или же на clojure переделать, заодно починить.
  lazy val IDS_SCORE_MVEL = """
      |uid = doc["_uid"].value;
      |dInx = uid.indexOf('#');
      |id = uid.substring(dInx + 1);
      |if (ids contains id) { incr; } else { 1.0; }
      |""".stripMargin

}


/** Интерфейс для передачи параметров поиска объявлений в индексе/типе. */
trait AdsSearchArgsT extends DynSearchArgs with ReceiversDsa with ProducerIdsDsa {
  // Ниже Seq брать нельзя, т.к. используется vararhs конструкция вида (x : _*), которая требует Seq[T].

  /** Добавить exists или missing фильтр на выходе, который будет убеждаться, что в индексе есть или нет id ресиверов. */
  def anyReceiverId: Option[Boolean]

  /** Необязательный id категории */
  def catIds: Seq[String]

  /** Добавить exists/missing фильтр на выходе, который будет убеждаться, что уровни присуствуют или отсутствуют. */
  def anyLevel: Option[Boolean]

  /** Произвольный текстовый запрос, если есть. */
  def qOpt: Option[String]

  /** Форсировать указанные id в начало списка (через мощный скоринг). */
  def forceFirstIds: Seq[String]

  /** Отбрасывать документы, имеющие указанные id'шники. */
  def withoutIds: Seq[String]

  /** Значение Generation timestamp, генерится при первом обращении к выдаче и передаётся при последующих запросах выдачи. */
  def generationOpt: Option[Long]

  /** Генератор самого дефолтового запроса, когда toEsQueryOpt не смог ничего предложить.
    * Нужно отображать только карточки, которые опубликованы где-либо. */
  override def defaultEsQuery: QueryBuilder = {
    val q0 = super.defaultEsQuery
    val f = FilterBuilders.existsFilter(EMReceivers.RCVRS_SLS_ESFN)
    val nf = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, f)
    QueryBuilders.filteredQuery(q0, nf)
  }

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.orElse {
      qOpt.flatMap[QueryBuilder] { q =>
        // Собираем запрос текстового поиска.
        // TODO Для коротких запросов следует искать по receiverId и фильтровать по qStr (query-filter + match-query).
        TextQueryV2Util.queryStr2QueryMarket(q, s"${SioConstants.FIELD_ALL}")
          .map { _.q }
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
    }
  }

  /**
   * Скомпилировать из аргументов запроса сам ES-запрос со всеми фильтрами и т.д.
   * @return
   */
  override def toEsQuery: QueryBuilder = {
    // Собираем запрос в функциональном стиле, иначе получается многовато вложенных if-else.
    var query3: QueryBuilder = super.toEsQuery
    if (generationOpt.isDefined && qOpt.isEmpty) {
      // Можно и нужно сортировтать с учётом genTs. Точный скоринг не нужен, поэтому просто прикручиваем скипт для скоринга.
      val scoreFun = ScoreFunctionBuilders.randomFunction(generationOpt.get)
      query3 = QueryBuilders.functionScoreQuery(query3, scoreFun)
    }
    // Если указаны id-шники, которые должны быть в начале выдачи, то добавить обернуть всё в ипостась Custom Score Query.
    if (forceFirstIds.nonEmpty) {
      // Запрошено, чтобы указанные id были в начале списка результатов.
      val scoreFun = ScoreFunctionBuilders.scriptFunction(AdsSearch.IDS_SCORE_MVEL, "mvel")
        .param("ids", new ju.HashSet[String](forceFirstIds.size).addAll(forceFirstIds) )  // TODO Opt: использовать java.util.HashSet?
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
    // Если задан anyReceiverId, то нужно добавить exists/missing-фильтр для проверки состояния значений в rcvrs.id
    if (anyReceiverId.isDefined) {
      val fn = EMReceivers.RCVRS_RECEIVER_ID_ESFN
      val f = if (anyReceiverId.get) {
        FilterBuilders.existsFilter(fn)
      } else {
        FilterBuilders.missingFilter(fn)
      }
      val nf = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, f)
      query3 = QueryBuilders.filteredQuery(query3, nf)
    }
    // Если задан anyLevel, то нужно добавиль фильтр по аналогии с anyReceiverId.
    if (anyLevel.isDefined) {
      val fn = EMReceivers.RCVRS_SLS_ESFN
      val f = if(anyLevel.get) {
        FilterBuilders.existsFilter(fn)
      } else {
        FilterBuilders.missingFilter(fn)
      }
      val nf = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, f)
      query3 = QueryBuilders.filteredQuery(query3, nf)
    }
    // Возвращаем собранный запрос.
    query3
  }

}

/** Дефолтовые значения аргументов поиска рекламных карточек. */
trait AdsSearchArgsDflt extends AdsSearchArgsT with DynSearchArgsDflt with ReceiversDsaDflt with ProducerIdsDsaDflt {
  override def forceFirstIds  : Seq[String] = Nil
  override def withoutIds     : Seq[String] = Nil
  override def qOpt           : Option[String] = None
  override def generationOpt  : Option[Long] = None
  override def catIds         : Seq[String] = Nil
  override def anyReceiverId  : Option[Boolean] = None
  override def anyLevel       : Option[Boolean] = None
}

/** Враппер для аргументов поиска рекламных карточек. */
trait AdsSearchArgsWrapper extends AdsSearchArgsT with DynSearchArgsWrapper with ReceiversDsaWrapper with ProducerIdsDsaWrapper {
  override type WT <: AdsSearchArgsT

  override def forceFirstIds  = _dsArgsUnderlying.forceFirstIds
  override def withoutIds     = _dsArgsUnderlying.withoutIds
  override def qOpt           = _dsArgsUnderlying.qOpt
  override def generationOpt  = _dsArgsUnderlying.generationOpt
  override def catIds         = _dsArgsUnderlying.catIds
  override def anyReceiverId  = _dsArgsUnderlying.anyReceiverId
  override def anyLevel       = _dsArgsUnderlying.anyLevel
}


/** Если нужно добавить в рекламную модель поиск по рекламным карточкам, то следует задействовать вот этот трейт. */
trait AdsSimpleSearchT extends EsDynSearchStatic[AdsSearchArgsT] {

  /**
   * Посчитать кол-во рекламных карточек, подходящих под запрос.
   * @param adSearch Экземпляр, описывающий поисковый запрос.
   * @return Фьючерс с кол-вом совпадений.
   */
  override def dynCount(adSearch: AdsSearchArgsT)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    // Необходимо выкинуть из запроса ненужные части.
    val adSearch2 = new AdsSearchArgsWrapper {
      override type WT = AdsSearchArgsT
      override def _dsArgsUnderlying = adSearch
      override def generationOpt = None
      override def forceFirstIds = Nil
    }
    super.dynCount(adSearch2)
  }

}
