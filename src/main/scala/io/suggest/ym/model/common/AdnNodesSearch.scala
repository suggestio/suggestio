package io.suggest.ym.model.common

import io.suggest.model.EsModel
import io.suggest.model.common._
import io.suggest.model.geo._
import io.suggest.util.SioConstants
import io.suggest.ym.model.MAdnNodeGeo
import io.suggest.ym.model.ad._
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
  def LOGO_EXIST_MVEL = {
    val fn = EMLogoImg.LOGO_IMG_ESFN
    s"""_source.containsKey("$fn");"""
  }

}


/** Интерфейс для описания критериев того, какие узлы надо найти. По этой спеки собирается ES-запрос. */
trait AdnNodesSearchArgsT extends TextQueryDsa with WithIdsDsa with CompanyIdsDsa with AdnSupIdsDsa
with AnyOfPersonIdsDsa with AdvDelegateAdnIdsDsa with DirectGeoParentsDsa with GeoParentsDsa
{

  /** Искать/фильтровать по shownTypeId узла. */
  def shownTypeIds: Seq[String]

  /** Права, которые должны быть у узла. */
  def withAdnRights: Seq[AdnRight]

  /** Искать/фильтровать по доступным sink'ам. */
  def onlyWithSinks: Seq[AdnSink]

  /** Искать/фильтровать по значению флага тестового узла. */
  def testNode: Option[Boolean]

  /** искать/фильтровать по флагу отображения в списке узлов поисковой выдачи. */
  def showInScNodeList: Option[Boolean]

  /** Искать/фильтровать по галочки активности узла. */
  def isEnabled: Option[Boolean]

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


  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Поиск/фильтрация по полю shown type id, хранящий id типа узла.
      if (shownTypeIds.isEmpty) {
        qb
      } else {
        val stiFilter = FilterBuilders.termsFilter(AdNetMember.ADN_SHOWN_TYPE_ID, shownTypeIds : _*)
        QueryBuilders.filteredQuery(qb, stiFilter)
      }
    }.orElse[QueryBuilder] {
      if (shownTypeIds.isEmpty) {
        None
      } else {
        val stiQuery = QueryBuilders.termsQuery(AdNetMember.ADN_SHOWN_TYPE_ID, shownTypeIds : _*)
          .minimumMatch(1)  // может быть только один тип ведь у одного узла.
        Some(stiQuery)
      }

    // Отрабатываем geoDistance через geoShape'ы. Текущий запрос обнаружения описываем как круг.
    // Если задан внутренний вырез (minDistance), то используем другое поле location и distance filter, т.к. SR.WITHIN не работает.
    }.map[QueryBuilder] { qb =>
      // Вешаем фильтры GeoShape
      geoDistance.fold(qb) { gsqd =>
        // География узлов живёт в отдельной модели, которая доступна через has_child
        val gq = MAdnNodeGeo.geoQuery(gsqd.glevel, gsqd.gdq.outerCircle)
        val gsfOuter = FilterBuilders.hasChildFilter(MAdnNodeGeo.ES_TYPE_NAME, gq)
        val qb2 = QueryBuilders.filteredQuery(qb, gsfOuter)
        innerUnshapeFilter(qb2, gsqd.gdq.innerCircleOpt)
      }
    }.orElse[QueryBuilder] {
      geoDistance.map { gsqd =>
        // Создаём GeoShape query. query по внешнему контуру, и filter по внутреннему.
        val gq  = MAdnNodeGeo.geoQuery(gsqd.glevel, gsqd.gdq.outerCircle)
        val qb2 = QueryBuilders.hasChildQuery(MAdnNodeGeo.ES_TYPE_NAME, gq)
        innerUnshapeFilter(qb2, gsqd.gdq.innerCircleOpt)
      }

    // Отрабатываем поиск пересечения с другими узлами (с другими индексированными шейпами).
    }.map[QueryBuilder] { qb =>
      if (intersectsWithPreIndexed.isEmpty) {
        qb
      } else {
        val filters = intersectsWithPreIndexed
          .map { _.toGeoShapeFilter }
        val filter = if (filters.size == 1) {
          filters.head
        } else {
          FilterBuilders.orFilter(filters: _*)
        }
        QueryBuilders.filteredQuery(qb, filter)
      }
    }.orElse[QueryBuilder] {
      if (intersectsWithPreIndexed.isEmpty) {
        None
      } else if (intersectsWithPreIndexed.size == 1) {
        val qb = intersectsWithPreIndexed.head.toGeoShapeQuery
        Some(qb)
      } else {
        val qb = intersectsWithPreIndexed.foldLeft( QueryBuilders.boolQuery().minimumNumberShouldMatch(1) ) {
          (acc, gsi)  =>  acc.should( gsi.toGeoShapeQuery )
        }
        Some(qb)
      }

    // Отрабатываем возможный список прав узла.
    }.map[QueryBuilder] { qb =>
      if (withAdnRights.isEmpty) {
        qb
      } else {
        val rf = FilterBuilders.termsFilter(AdNetMember.ADN_RIGHTS_ESFN, withAdnRights.map(_.name) : _*)
          .execution("and")
        QueryBuilders.filteredQuery(qb, rf)
      }
    }.orElse[QueryBuilder] {
      if (withAdnRights.isEmpty) {
        None
      } else {
        val rq = QueryBuilders.termsQuery(AdNetMember.ADN_RIGHTS_ESFN, withAdnRights.map(_.name): _*)
          .minimumMatch(withAdnRights.size)
        Some(rq)
      }

    // Ищем/фильтруем по sink-флагам
    }.map[QueryBuilder] { qb =>
      if (onlyWithSinks.isEmpty) {
        qb
      } else {
        val sf = FilterBuilders.termsFilter(AdNetMember.ADN_SINKS_ESFN, onlyWithSinks.map(_.name) : _*)
        QueryBuilders.filteredQuery(qb, sf)
      }
    }.orElse[QueryBuilder] {
      if (onlyWithSinks.isEmpty) {
        None
      } else {
        val sq = QueryBuilders.termsQuery(AdNetMember.ADN_SINKS_ESFN, onlyWithSinks.map(_.name) : _*)
          .minimumMatch(onlyWithSinks.size)
        Some(sq)
      }

    // Отрабатываем флаг testNode.
    }.map[QueryBuilder] { qb =>
      testNode.fold(qb) { tnFlag =>
        var tnf: FilterBuilder = FilterBuilders.termFilter(AdNetMember.ADN_TEST_NODE_ESFN, tnFlag)
        if (!tnFlag) {
          val tmf = FilterBuilders.missingFilter(AdNetMember.ADN_TEST_NODE_ESFN)
          tnf = FilterBuilders.orFilter(tnf, tmf)
        }
        QueryBuilders.filteredQuery(qb, tnf)
      }
    }.orElse[QueryBuilder] {
     testNode.map { tnFlag =>
        // TODO Нужно добавить аналог missing filter для query и как-то объеденить через OR. Или пока так и пересохранять узлы с tn=false.
        QueryBuilders.termQuery(AdNetMember.ADN_TEST_NODE_ESFN, tnFlag)
      }

    // Отрабатываем флаг conf.showInScNodeList
    }.map[QueryBuilder] { qb =>
      showInScNodeList.fold(qb) { sscFlag =>
        val sscf = FilterBuilders.termFilter(EMNodeConf.CONF_SHOW_IN_SC_NODES_LIST_ESFN, sscFlag)
        QueryBuilders.filteredQuery(qb, sscf)
      }
    }.orElse[QueryBuilder] {
      showInScNodeList.map { sscFlag =>
        QueryBuilders.termQuery(EMNodeConf.CONF_SHOW_IN_SC_NODES_LIST_ESFN, sscFlag)
      }

    // Ищем/фильтруем по флагу включённости узла.
    }.map[QueryBuilder] { qb =>
      isEnabled.fold(qb) { isEnabled =>
        val ief = FilterBuilders.termFilter(AdNetMember.ADN_IS_ENABLED_ESFN, isEnabled)
        QueryBuilders.filteredQuery(qb, ief)
      }
    }.orElse[QueryBuilder] {
      isEnabled.map { isEnabled =>
        QueryBuilders.termQuery(AdNetMember.ADN_IS_ENABLED_ESFN, isEnabled)
      }

    // Нет критерия для поиска по индексу - сдаёмся.
    }
  }

  /**
   * Сборка поискового ES-запроса под перечисленные в аргументах критерии.
   * @return Экземпляр QueryBuilder, пригодный для отправки в поисковом запросе.
   */
  override def toEsQuery: QueryBuilder = {
    var qb2 = super.toEsQuery

    // Добавляем фильтр по id нежелательных результатов.
    if (withoutIds.nonEmpty) {
      val idsf = FilterBuilders.notFilter(
        FilterBuilders.idsFilter().ids(withoutIds : _*))
      qb2 = QueryBuilders.filteredQuery(qb2, idsf)
    }

    // Добавить фильтр по наличию логотипа. Т.к. поле не индексируется, то используется
    if (hasLogo.nonEmpty) {
      var ef: FilterBuilder = FilterBuilders.scriptFilter(AdnNodesSearch.LOGO_EXIST_MVEL).lang("mvel")
      if (!hasLogo.get) {
        ef = FilterBuilders.notFilter(ef)
      }
      qb2 = QueryBuilders.filteredQuery(qb2, ef)
    }

    // Сборка завершена, возвращаем собранную es query.
    qb2
  }

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
      val sob = SortBuilders.fieldSort(EMAdnMMetadataStatic.META_NAME_SHORT_NOTOK_ESFN)
        .order(SortOrder.ASC)
        .ignoreUnmapped(true)
      srb1 addSort sob
    }
    // Заливаем ключи роутинга, если он задан.
    if (withRouting.nonEmpty)
      srb1.setRouting(withRouting : _*)
    srb1
  }


  override def toStringBuilder: StringBuilder = {
    val sb = super.toStringBuilder
    fmtColl2sb("shownTypeIds", shownTypeIds, sb)
    fmtColl2sb("withAdnRights", withAdnRights, sb)
    fmtColl2sb("onlyWithSinks", onlyWithSinks, sb)
    fmtColl2sb("testNode", testNode, sb)
    fmtColl2sb("showInScNodeList", showInScNodeList, sb)
    fmtColl2sb("withoutIds", withoutIds, sb)
    fmtColl2sb("geoDistance", geoDistance, sb)
    fmtColl2sb("intersectsWithPreIndexed", intersectsWithPreIndexed, sb)
    fmtColl2sb("hasLogo", hasLogo, sb)
    fmtColl2sb("withGeoDistanceSort", withGeoDistanceSort, sb)
    if (withNameSort)
      sb.append("\n  withNameSort = true")
    fmtColl2sb("withRouting", withRouting, sb)
    if (qOptField != SioConstants.FIELD_ALL)
      fmtColl2sb("ftsSearchFN", qOptField, sb)
    sb
  }

}



/** Реализация интерфейса AdnNodesSearchArgsT с пустыми (дефолтовыми) значениями всех полей. */
trait AdnNodesSearchArgs extends AdnNodesSearchArgsT with TextQueryDsaDflt with WithIdsDsaDflt with CompanyIdsDsaDflt
with AdnSupIdsDsaDflt with AnyOfPersonIdsDsaDflt with AdvDelegateAdnIdsDsaDflt with DirectGeoParentsDsaDflt
with GeoParentsDsaDflt
{
  override def withGeoDistanceSort: Option[GeoPoint] = None
  override def hasLogo: Option[Boolean] = None
  override def intersectsWithPreIndexed: Seq[GeoShapeIndexed] = Seq.empty
  override def shownTypeIds: Seq[String] = Seq.empty
  override def withRouting: Seq[String] = Seq.empty
  override def testNode: Option[Boolean] = None
  override def showInScNodeList: Option[Boolean] = None
  override def isEnabled: Option[Boolean] = None
  override def onlyWithSinks: Seq[AdnSink] = Seq.empty
  override def geoDistance: Option[GeoShapeQueryData] = None
  override def withoutIds: Seq[String] = Seq.empty
  override def withAdnRights: Seq[AdnRight] = Seq.empty
  override def withNameSort: Boolean = false
  override def maxResults: Int = EsModel.MAX_RESULTS_DFLT
  override def offset: Int = EsModel.OFFSET_DFLT
}


/** Враппер над аргументами поиска узлов, переданными в underlying. */
trait AdnNodesSearchArgsWrapper extends AdnNodesSearchArgsT with TextQueryDsaWrapper with WithIdsDsaWrapper
with CompanyIdsDsaWrapper with AdnSupIdsDsaWrapper with AnyOfPersonIdsDsaWrapper with AdvDelegateAdnIdsDsaWrapper
with DirectGeoParentsDsaWrapper with GeoParentsDsaWrapper
{

  override type WT <: AdnNodesSearchArgsT

  override def withGeoDistanceSort = _dsArgsUnderlying.withGeoDistanceSort
  override def hasLogo = _dsArgsUnderlying.hasLogo
  override def intersectsWithPreIndexed = _dsArgsUnderlying.intersectsWithPreIndexed
  override def shownTypeIds = _dsArgsUnderlying.shownTypeIds
  override def withRouting = _dsArgsUnderlying.withRouting
  override def testNode = _dsArgsUnderlying.testNode
  override def showInScNodeList = _dsArgsUnderlying.showInScNodeList
  override def isEnabled = _dsArgsUnderlying.isEnabled
  override def onlyWithSinks = _dsArgsUnderlying.onlyWithSinks
  override def geoDistance = _dsArgsUnderlying.geoDistance
  override def withoutIds = _dsArgsUnderlying.withoutIds
  override def withAdnRights = _dsArgsUnderlying.withAdnRights
  override def withNameSort = _dsArgsUnderlying.withNameSort
  override def maxResults = _dsArgsUnderlying.maxResults
  override def offset = _dsArgsUnderlying.offset
}


/** Аддон для static-моделей (модели MAdnNode), добавляющий динамический поиск в статическую модель. */
trait AdnNodesSearch extends EsDynSearchStatic[AdnNodesSearchArgsT]

