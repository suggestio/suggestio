package io.suggest.ym.model.common

import io.suggest.model.EsModel
import io.suggest.model.common.EMPersonIds
import io.suggest.model.geo._
import io.suggest.util.SioConstants
import io.suggest.util.text.TextQueryV2Util
import io.suggest.ym.model.MAdnNodeGeo
import io.suggest.ym.model.common.AdnRights.AdnRight
import io.suggest.ym.model.common.AdnSinks.AdnSink
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.index.query.{FilterBuilder, FilterBuilders, QueryBuilders, QueryBuilder}
import org.elasticsearch.search.sort.{SortOrder, SortBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.08.14 14:26
 * Description: Аддон для модели MAdnNode, которая отвечает за сборку и исполнение поисковых запросов узлов рекламной
 * сети.
 */
object AdnNodesSearch {

  /** Скрипт для фильтрации по наличию значения в поле logo. */
  val LOGO_EXIST_MVEL = {
    val fn = EMLogoImg.LOGO_IMG_ESFN
    s"""_source.containsKey("$fn");"""
  }

  /** Добавить фильтр в запрос, выкидывающий объекты, расстояние до центра которых ближе, чем это допустимо. */
  private def innerUnshapeFilter(qb2: QueryBuilder, innerCircle: Option[CircleGs]): QueryBuilder = {
    // TODO Этот фильтр скорее всего не пашет, т.к. ни разу не тестировался и уже пережил перепиливание подсистемы географии.
    innerCircle.fold(qb2) { inCircle =>
      val innerFilter = FilterBuilders.geoDistanceFilter(EMAdnNodeGeo.GEO_POINT_ESFN)
        .point(inCircle.center.lat, inCircle.center.lon)
        .distance(inCircle.radius.distance, inCircle.radius.units)
      val notInner = FilterBuilders.notFilter(innerFilter)
      QueryBuilders.filteredQuery(qb2, notInner)
    }
  }

  /**
   * Сборка поискового ES-запроса под перечисленные в аргументах критерии.
   * @param args Критерии поиска.
   * @return Экземпляр QueryBuilder, пригодный для отправки в поисковом запросе.
   */
  def mkEsQuery(args: AdnNodesSearchArgsT): QueryBuilder = {
    var qb2: QueryBuilder = args.qStr.flatMap[QueryBuilder] { qStr =>
      TextQueryV2Util.queryStr2QueryMarket(qStr, args.ftsSearchFN)
        .map { _.q }

    // Отрабатываем companyId
    }.map[QueryBuilder] { qb =>
      if (args.companyIds.isEmpty) {
        qb
      } else {
        val cf = FilterBuilders.termsFilter(EMCompanyId.COMPANY_ID_ESFN, args.companyIds : _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb, cf)
      }
    }.orElse {
      if (args.companyIds.isEmpty) {
        None
      } else {
        val cq = QueryBuilders.termsQuery(EMCompanyId.COMPANY_ID_ESFN, args.companyIds : _*)
          .minimumMatch(1)
        Some(cq)
      }

    // Отрабатываем adnSupIds:
    }.map[QueryBuilder] { qb =>
      if (args.adnSupIds.isEmpty) {
        qb
      } else {
        val sf = FilterBuilders.termsFilter(AdNetMember.ADN_SUPERVISOR_ID_ESFN, args.adnSupIds : _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb, sf)
      }
    }.orElse[QueryBuilder] {
      if (args.adnSupIds.nonEmpty) {
        val sq = QueryBuilders.termsQuery(AdNetMember.ADN_SUPERVISOR_ID_ESFN, args.adnSupIds : _*)
          .minimumMatch(1)
        Some(sq)
      } else {
        None
      }

    // Дальше отрабатываем список возможных personIds.
    }.map[QueryBuilder] { qb =>
      if (args.anyOfPersonIds.isEmpty) {
        qb
      } else {
        val pf = FilterBuilders.termsFilter(EMPersonIds.PERSON_ID_ESFN, args.anyOfPersonIds : _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb, pf)
      }
    }.orElse[QueryBuilder] {
      if (args.anyOfPersonIds.isEmpty) {
        None
      } else {
        val pq = QueryBuilders.termsQuery(EMPersonIds.PERSON_ID_ESFN, args.anyOfPersonIds : _*)
          .minimumMatch(1)
        Some(pq)
      }

    // Отрабатываем id узлов adv-делегатов
    }.map[QueryBuilder] { qb =>
      if (args.advDelegateAdnIds.isEmpty) {
        qb
      } else {
        val af = FilterBuilders.termsFilter(AdNetMember.ADN_ADV_DELEGATE_ESFN, args.advDelegateAdnIds : _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb, af)
      }
    }.orElse[QueryBuilder] {
      if (args.advDelegateAdnIds.isEmpty) {
        None
      } else {
        val aq = QueryBuilders.termsQuery(AdNetMember.ADN_ADV_DELEGATE_ESFN, args.advDelegateAdnIds: _*)
          .minimumMatch(1)
        Some(aq)
      }

    // Отрабатываем прямых гео-родителей
    }.map[QueryBuilder] { qb =>
      if (args.withDirectGeoParents.nonEmpty) {
        val filter = FilterBuilders.termsFilter(EMAdnNodeGeo.GEO_DIRECT_PARENT_NODES_ESFN, args.withDirectGeoParents : _*)
        QueryBuilders.filteredQuery(qb, filter)
      } else {
        qb
      }
    }.orElse[QueryBuilder] {
      if (args.withDirectGeoParents.nonEmpty) {
        val qb = QueryBuilders.termsQuery(EMAdnNodeGeo.GEO_DIRECT_PARENT_NODES_ESFN, args.withDirectGeoParents: _*)
        Some(qb)
      } else {
        None
      }

    // Отрабатываем поиск по любым гео-родителям
    }.map[QueryBuilder] { qb =>
      if (args.withGeoParents.nonEmpty) {
        val filter = FilterBuilders.termsFilter(EMAdnNodeGeo.GEO_ALL_PARENT_NODES_ESFN, args.withGeoParents : _*)
        QueryBuilders.filteredQuery(qb, filter)
      } else {
        qb
      }
    }.orElse[QueryBuilder] {
      if (args.withGeoParents.nonEmpty) {
        val qb = QueryBuilders.termsQuery(EMAdnNodeGeo.GEO_ALL_PARENT_NODES_ESFN, args.withGeoParents : _*)
        Some(qb)
      } else {
        None
      }

    // Поиск/фильтрация по полю shown type id, хранящий id типа узла.
    }.map[QueryBuilder] { qb =>
      if (args.shownTypeIds.isEmpty) {
        qb
      } else {
        val stiFilter = FilterBuilders.termsFilter(AdNetMember.ADN_SHOWN_TYPE_ID, args.shownTypeIds : _*)
        QueryBuilders.filteredQuery(qb, stiFilter)
      }
    }.orElse[QueryBuilder] {
      if (args.shownTypeIds.isEmpty) {
        None
      } else {
        val stiQuery = QueryBuilders.termsQuery(AdNetMember.ADN_SHOWN_TYPE_ID, args.shownTypeIds : _*)
          .minimumMatch(1)  // может быть только один тип ведь у одного узла.
        Some(stiQuery)
      }

    // Отрабатываем geoDistance через geoShape'ы. Текущий запрос обнаружения описываем как круг.
    // Если задан внутренний вырез (minDistance), то используем другое поле location и distance filter, т.к. SR.WITHIN не работает.
    }.map[QueryBuilder] { qb =>
      // Вешаем фильтры GeoShape
      args.geoDistance.fold(qb) { gsqd =>
        // География узлов живёт в отдельной модели, которая доступна через has_child
        val gq = MAdnNodeGeo.geoQuery(gsqd.glevel, gsqd.gdq.outerCircle)
        val gsfOuter = FilterBuilders.hasChildFilter(MAdnNodeGeo.ES_TYPE_NAME, gq)
        val qb2 = QueryBuilders.filteredQuery(qb, gsfOuter)
        innerUnshapeFilter(qb2, gsqd.gdq.innerCircleOpt)
      }
    }.orElse[QueryBuilder] {
      args.geoDistance.map { gsqd =>
        // Создаём GeoShape query. query по внешнему контуру, и filter по внутреннему.
        val gq  = MAdnNodeGeo.geoQuery(gsqd.glevel, gsqd.gdq.outerCircle)
        val qb2 = QueryBuilders.hasChildQuery(MAdnNodeGeo.ES_TYPE_NAME, gq)
        innerUnshapeFilter(qb2, gsqd.gdq.innerCircleOpt)
      }

    // Отрабатываем поиск пересечения с другими узлами (с другими индексированными шейпами).
    }.map[QueryBuilder] { qb =>
      if (args.intersectsWithPreIndexed.isEmpty) {
        qb
      } else {
        val filters = args.intersectsWithPreIndexed
          .map { _.toGeoShapeFilter }
        val filter = if (filters.size == 1) {
          filters.head
        } else {
          FilterBuilders.orFilter(filters: _*)
        }
        QueryBuilders.filteredQuery(qb, filter)
      }
    }.orElse[QueryBuilder] {
      if (args.intersectsWithPreIndexed.isEmpty) {
        None
      } else if (args.intersectsWithPreIndexed.size == 1) {
        val qb = args.intersectsWithPreIndexed.head.toGeoShapeQuery
        Some(qb)
      } else {
        val qb = args.intersectsWithPreIndexed.foldLeft( QueryBuilders.boolQuery().minimumNumberShouldMatch(1) ) {
          (acc, gsi)  =>  acc.should( gsi.toGeoShapeQuery )
        }
        Some(qb)
      }

    // Отрабатываем возможный список прав узла.
    }.map[QueryBuilder] { qb =>
      if (args.withAdnRights.isEmpty) {
        qb
      } else {
        val rf = FilterBuilders.termsFilter(AdNetMember.ADN_RIGHTS_ESFN, args.withAdnRights.map(_.name) : _*)
          .execution("and")
        QueryBuilders.filteredQuery(qb, rf)
      }
    }.orElse[QueryBuilder] {
      if (args.withAdnRights.isEmpty) {
        None
      } else {
        val rq = QueryBuilders.termsQuery(AdNetMember.ADN_RIGHTS_ESFN, args.withAdnRights.map(_.name): _*)
          .minimumMatch(args.withAdnRights.size)
        Some(rq)
      }

    // Ищем/фильтруем по sink-флагам
    }.map[QueryBuilder] { qb =>
      if (args.onlyWithSinks.isEmpty) {
        qb
      } else {
        val sf = FilterBuilders.termsFilter(AdNetMember.ADN_SINKS_ESFN, args.onlyWithSinks.map(_.name) : _*)
        QueryBuilders.filteredQuery(qb, sf)
      }
    }.orElse[QueryBuilder] {
      if (args.onlyWithSinks.isEmpty) {
        None
      } else {
        val sq = QueryBuilders.termsQuery(AdNetMember.ADN_SINKS_ESFN, args.onlyWithSinks.map(_.name) : _*)
          .minimumMatch(args.onlyWithSinks.size)
        Some(sq)
      }

    // Отрабатываем флаг testNode.
    }.map[QueryBuilder] { qb =>
      args.testNode.fold(qb) { tnFlag =>
        var tnf: FilterBuilder = FilterBuilders.termFilter(AdNetMember.ADN_TEST_NODE_ESFN, tnFlag)
        if (!tnFlag) {
          val tmf = FilterBuilders.missingFilter(AdNetMember.ADN_TEST_NODE_ESFN)
          tnf = FilterBuilders.orFilter(tnf, tmf)
        }
        QueryBuilders.filteredQuery(qb, tnf)
      }
    }.orElse[QueryBuilder] {
      args.testNode.map { tnFlag =>
        // TODO Нужно добавить аналог missing filter для query и как-то объеденить через OR. Или пока так и пересохранять узлы с tn=false.
        QueryBuilders.termQuery(AdNetMember.ADN_TEST_NODE_ESFN, tnFlag)
      }

    }.getOrElse[QueryBuilder] {
      // Нет критерия для поиска по индексу - сдаёмся.
      QueryBuilders.matchAllQuery()
    }

    // Добавляем фильтр по id нежелательных результатов.
    if (args.withoutIds.nonEmpty) {
      val idsf = FilterBuilders.notFilter(
        FilterBuilders.idsFilter().ids(args.withoutIds : _*))
      qb2 = QueryBuilders.filteredQuery(qb2, idsf)
    }

    // Добавить фильтр по наличию логотипа. Т.к. поле не индексируется, то используется
    if (args.hasLogo.nonEmpty) {
      var ef: FilterBuilder = FilterBuilders.scriptFilter(LOGO_EXIST_MVEL).lang("mvel")
      if (!args.hasLogo.get) {
        ef = FilterBuilders.notFilter(ef)
      }
      qb2 = QueryBuilders.filteredQuery(qb2, ef)
    }

    // Сборка завершена, возвращаем собранную es query.
    qb2
  }

}


/** Интерфейс для описания критериев того, какие узлы надо найти. По этой спеки собирается ES-запрос. */
trait AdnNodesSearchArgsT extends DynSearchArgs {

  /** Поиск по слову/словам. */
  def qStr: Option[String]

  /** id компаний, под которые копаем узлы ADN. */
  def companyIds: Seq[String]

  /** Искать/фильтровать по id супервизора узла. */
  def adnSupIds: Seq[String]

  /** Искать/фильтровать по юзеру. */
  def anyOfPersonIds: Seq[String]

  /** Искать/фильтровать по id узла, которому была делегирована фунция модерации размещения рекламных карточек. */
  def advDelegateAdnIds: Seq[String]

  /** Искать по прямым гео-родителям. Нужно чтобы у узлов была проставлена инфа по геородителям. */
  def withDirectGeoParents: Seq[String]

  /** Искать по гео-родителям любого уровня. */
  def withGeoParents: Seq[String]

  /** Искать/фильтровать по shownTypeId узла. */
  def shownTypeIds: Seq[String]

  /** Права, которые должны быть у узла. */
  def withAdnRights: Seq[AdnRight]

  /** Искать/фильтровать по доступным sink'ам. */
  def onlyWithSinks: Seq[AdnSink]

  /** Искать/фильтровать по значению флага тестового узла. */
  def testNode: Option[Boolean]

  /** Отсеивать из результатов документы (узлы) с перечисленными id. */
  def withoutIds: Seq[String]

  /** Фильтровать по дистанции относительно какой-то точки. */
  def geoDistance: Option[GeoShapeQueryData]

  /** Пересечение с шейпом другого узла. Полезно для поиска узлов, географически входящих в указанный узел. */
  def intersectsWithPreIndexed: Seq[GeoShapeIndexed]

  /** Фильтровать по наличию/отсутсвию логотипа. */
  def hasLogo: Option[Boolean]

  /** + сортировка результатов по расстоянию до указанной точки. */
  def withGeoDistanceSort: Option[GeoPoint]

  /** Сортировать по названиям? */
  def withNameSort: Boolean

  /** Дополнительно задать ключ для роутинга. */
  def withRouting: Seq[String]

  /** По каким полям будем искать? */
  def ftsSearchFN: String = SioConstants.FIELD_ALL

  override def toEsQuery = AdnNodesSearch.mkEsQuery(this)

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    // Добавить сортировку по дистанции до указанной точки, если необходимо.
    withGeoDistanceSort.foreach { geoPoint =>
      val sb = SortBuilders.geoDistanceSort(EMAdnNodeGeo.GEO_POINT_ESFN)
        .point(geoPoint.lat, geoPoint.lon)
        .order(SortOrder.ASC)   // ASC - ближайшие сверху, далёкие внизу.
        .unit(DistanceUnit.KILOMETERS)
      srb1.addSort(sb)
    }
    if (withNameSort) {
      srb1.addSort(AdnMMetadata.NAME_ESFN, SortOrder.ASC)
    }
    // Заливаем ключи роутинга, если он задан.
    if (withRouting.nonEmpty)
      srb1.setRouting(withRouting : _*)
    srb1
  }

}



/** Реализация интерфейса AdnNodesSearchArgsT с пустыми (дефолтовыми) значениями всех полей. */
trait AdnNodesSearchArgs extends AdnNodesSearchArgsT {
  override def qStr: Option[String] = None
  override def companyIds: Seq[String] = Seq.empty
  override def advDelegateAdnIds: Seq[String] = Seq.empty
  override def adnSupIds: Seq[String] = Seq.empty
  override def withGeoDistanceSort: Option[GeoPoint] = None
  override def hasLogo: Option[Boolean] = None
  override def intersectsWithPreIndexed: Seq[GeoShapeIndexed] = Seq.empty
  override def shownTypeIds: Seq[String] = Seq.empty
  override def withDirectGeoParents: Seq[String] = Seq.empty
  override def anyOfPersonIds: Seq[String] = Seq.empty
  override def withRouting: Seq[String] = Seq.empty
  override def testNode: Option[Boolean] = None
  override def onlyWithSinks: Seq[AdnSink] = Seq.empty
  override def geoDistance: Option[GeoShapeQueryData] = None
  override def withGeoParents: Seq[String] = Seq.empty
  override def withoutIds: Seq[String] = Seq.empty
  override def withAdnRights: Seq[AdnRight] = Seq.empty
  override def withNameSort: Boolean = false
  override def maxResults: Int = EsModel.MAX_RESULTS_DFLT
  override def offset: Int = EsModel.OFFSET_DFLT
}


/** Аддон для static-моделей (модели MAdnNode), добавляющий динамический поиск в статическую модель. */
trait AdnNodesSearch extends EsDynSearchStatic[AdnNodesSearchArgsT]

