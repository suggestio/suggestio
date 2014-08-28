package io.suggest.ym.model.common

import io.suggest.model.common.EMPersonIds
import io.suggest.model.geo.{CircleGs, GeoDistanceQuery, Distance, GeoPoint}
import io.suggest.util.SioConstants
import io.suggest.util.text.TextQueryV2Util
import io.suggest.ym.model.common.AdnRights.AdnRight
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.common.geo.ShapeRelation
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

  /** Добавить фильтр в запрос, выкидывающий объекты, расстояние до центра которыйх ближе, чем это допустимо. */
  private def innerUnshapeFilter(qb2: QueryBuilder, innerCircle: Option[CircleGs]): QueryBuilder = {
    innerCircle.fold(qb2) { inCircle =>
      val innerFilter = FilterBuilders.geoDistanceFilter(EMAdnMMetadataStatic.META_LOCATION_ESFN)
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

    // Отрабатываем geoDistance через geoShape'ы. Текущий запрос обнаружения описываем как круг.
    // Если задан внутренний вырез (minDistance), то используем другое поле location и distance filter, т.к. SR.WITHIN не работает.
    }.map[QueryBuilder] { qb =>
      // Вешаем фильтры GeoShape
      args.geoDistance.fold(qb) { gd =>
        val gsfOuter = FilterBuilders.geoShapeFilter(AdnMMetadata.GEO_SHAPE_ESFN, gd.outerCircle.toEsShapeBuilder, ShapeRelation.INTERSECTS)
        val qb2 = QueryBuilders.filteredQuery(qb, gsfOuter)
        innerUnshapeFilter(qb2, gd.innerCircleOpt)
      }
    }.orElse[QueryBuilder] {
      args.geoDistance.map { gd =>
        // Создаём GeoShape query. query по внешнему контуру, и filter по внутреннему.
        val qb2 = QueryBuilders.geoShapeQuery(AdnMMetadata.GEO_SHAPE_ESFN, gd.outerCircle.toEsShapeBuilder)
        innerUnshapeFilter(qb2, gd.innerCircleOpt)
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
        val rq = QueryBuilders.termsQuery(AdNetMember.ADN_RIGHTS_ESFN, args.withAdnRights.map(_.name) : _*)
          .minimumMatch(args.withAdnRights.size)
        Some(rq)
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

    // Добавляем geo-фильтр по дистанции до точки, если необходимо.
    // Закоменчено, т.к. часть логики перевешана на GeoShape-контуры.
    /*if (args.geoDistance.isDefined) {
      val gd = args.geoDistance.get
      // Если задан distanceMin, то нужно использовать фильтр geo range. Иначе geo distance.
      val gdf = gd.distanceMin.fold [FilterBuilder] {
        // Фильтруем в радиусе указанной точки
        FilterBuilders.geoDistanceFilter(EMAdnMMetadataStatic.META_LOCATION_ESFN)
          .point(gd.center.lat, gd.center.lon)
          .distance(gd.distanceMax.distance, gd.distanceMax.units)
      } { distanceMin =>
        // Фильтруем между радиусами из одной точки.
        FilterBuilders.geoDistanceRangeFilter(EMAdnMMetadataStatic.META_LOCATION_ESFN)
          .point(gd.center.lat, gd.center.lon)
          .from( distanceMin.toString )
          .to( gd.distanceMax.toString )
      }
      qb2 = QueryBuilders.filteredQuery(qb2, gdf)
    }*/

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

  /** Права, которые должны быть у узла. */
  def withAdnRights: Seq[AdnRight]

  /** Искать/фильтровать по значению флага тестового узла. */
  def testNode: Option[Boolean]

  /** Отсеивать из результатов документы (узлы) с перечисленными id. */
  def withoutIds: Seq[String]

  /** Фильтровать по дистанции относительно какой-то точки. */
  def geoDistance: Option[GeoDistanceQuery]

  /** Фильтровать по наличию/отсутсвию логотипа. */
  def hasLogo: Option[Boolean]

  /** + сортировка результатов по расстоянию до указанной точки. */
  def withGeoDistanceSort: Option[GeoPoint]

  /** Сортировать по названиям? */
  def withNameSort: Boolean

  /** По каким полям будем искать? */
  def ftsSearchFN: String = SioConstants.FIELD_ALL

  override def toEsQuery = AdnNodesSearch.mkEsQuery(this)

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    // Добавить сортировку по дистанции до указанной точки, если необходимо.
    withGeoDistanceSort.foreach { geoPoint =>
      val sb = SortBuilders.geoDistanceSort(EMAdnMMetadataStatic.META_LOCATION_ESFN)
        .point(geoPoint.lat, geoPoint.lon)
        .order(SortOrder.ASC)   // ASC - ближайшие сверху, далёкие внизу.
        .unit(DistanceUnit.KILOMETERS)
      srb1.addSort(sb)
    }
    if (withNameSort) {
      srb1.addSort(AdnMMetadata.NAME_ESFN, SortOrder.ASC)
    }
    srb1
  }

}


/** Аддон для static-моделей (модели MAdnNode), добавляющий динамический поиск в статическую модель. */
trait AdnNodesSearch extends EsDynSearchStatic[AdnNodesSearchArgsT]

