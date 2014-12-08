package io.suggest.ym.model.common

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModelStaticMutAkvT, EsModel, EsModelPlayJsonT}
import io.suggest.model.geo.{GeoShapeIndexed, CircleGs, GeoShapeQueryData, GeoPoint}
import io.suggest.util.SioEsUtil._
import io.suggest.ym.model.MAdnNodeGeo
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders, QueryBuilder}
import org.elasticsearch.search.sort.{SortBuilders, SortOrder}
import play.api.libs.json._
import java.{util => ju, lang => jl}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.14 11:01
 * Description: Поле, содержащее контейнер для геоинформации узла.
 */
object EMAdnNodeGeo {

  val GEO_ESFN = "geo"

  private def fullFN(fn: String) = GEO_ESFN + "." + fn

  def GEO_POINT_ESFN                = fullFN( AdnNodeGeodata.POINT_ESFN )
  def GEO_DIRECT_PARENT_NODES_ESFN  = fullFN( AdnNodeGeodata.DIRECT_PARENT_NODES_ESFN )
  def GEO_ALL_PARENT_NODES_ESFN     = fullFN( AdnNodeGeodata.ALL_PARENT_NODES_ESFN )

}


import EMAdnNodeGeo._


/** Статический аддон для модели [[io.suggest.ym.model.MAdnNode]] (или иной), который добавляет обработку
  * geo-контейнера с инфой по узлу. */
trait EMAdnNodeGeoStatic extends EsModelStaticMutAkvT {
  override type T <: EMAdnNodeGeoMut

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (GEO_ESFN, geoRaw) =>
        acc.geo = AdnNodeGeodata.deserialize(geoRaw)
    }
  }

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(GEO_ESFN, enabled = true, properties = AdnNodeGeodata.generateMappingProps) ::
      super.generateMappingProps
  }
}


/** Голый интерфейс просто для доступа к полю geo. */
trait IEMAdnNodeGeo {
  def geo: AdnNodeGeodata
}

/** Аддон для immutable-поддержки поля geo в динамической части модели. */
trait EMAdnNodeGeo extends EsModelPlayJsonT with IEMAdnNodeGeo {
  override type T <: EMAdnNodeGeo

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (geo.nonEmpty)
      GEO_ESFN -> geo.toPlayJson :: acc0
    else
      acc0
  }
}

/** Аддон для mutable-поддержки поля geo в динамической части модели. */
trait EMAdnNodeGeoMut extends EMAdnNodeGeo {
  override type T <: EMAdnNodeGeoMut
  var geo: AdnNodeGeodata
}



/** Статическая утиль для экземпляра контейнера geo-поля. */
object AdnNodeGeodata {

  val POINT_ESFN                  = "pt"
  val DIRECT_PARENT_NODES_ESFN    = "pnd"
  val ALL_PARENT_NODES_ESFN       = "pna"

  /**
   * Внутренний экстрактор String-множеств из полей карты парсера es.
   * @param fn Название экстрагируемого поля.
   * @param jmap Карта.
   * @return Set[String] на основе значений указанного поля. Или пустой Set, если поле пусто или отсутствует.
   */
  private def extractStringSet(fn: String, jmap: ju.Map[_, _]): Set[String] = {
    Option(jmap get fn).fold (Set.empty[String]) {
      case raws: jl.Iterable[_]  =>
        raws.iterator()
          .map(EsModel.stringParser)
          .toSet
    }
  }

  /** Десериализация экземпляра [[AdnNodeGeodata]] из карты. */
  val deserialize: PartialFunction[Any, AdnNodeGeodata] = {
    case jmap: ju.Map[_, _] =>
      AdnNodeGeodata(
        point             = Option(jmap.get(POINT_ESFN)).flatMap(GeoPoint.deserializeOpt),
        directParentIds   = extractStringSet(DIRECT_PARENT_NODES_ESFN, jmap),
        allParentIds      = extractStringSet(ALL_PARENT_NODES_ESFN, jmap)
      )
  }

  /** Сгенерить маппинг на уровне geo-объекта. */
  def generateMappingProps: List[DocField] = {
    List(
      FieldGeoPoint(POINT_ESFN, latLon = true,
        geohash = true, geohashPrefix = true,  geohashPrecision = "8",
        fieldData = GeoPointFieldData(format = GeoPointFieldDataFormats.compressed, precision = "3m")
      ),
      FieldString(DIRECT_PARENT_NODES_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = false)
    )
  }

  val empty = AdnNodeGeodata()


  private def strIds2jsStrArray(strs: Iterable[String]): JsArray = {
    JsArray(strs.iterator.map(JsString.apply).toSeq)
  }

}


import AdnNodeGeodata._


/**
 * Контейнер геоданных рекламного узла (или иной модели).
 * @param point Точка на карте, характеризующая узел.
 * @param directParentIds Список id объектов этой же модели, которые географически являются родительскими по отношению
 *                        к текущему экземпляру.
 * @param allParentIds Список родительских id объектов на всех уровнях. Обычно генерится чем-то внешним на основе
 *                     directParentIds путём рекурсивного аккамулирования родительских значений allParentIds.
 *
 */
case class AdnNodeGeodata(
  // Все поля должны быть immmutable внутри и снаружи. Иначе AdnNodeGeodata.empty придётся заменить с val на def.
  point: Option[GeoPoint] = None,
  directParentIds: Set[String] = Set.empty,
  allParentIds: Set[String] = Set.empty
) {

  def nonEmpty: Boolean = {
    productIterator.exists {
      case opt: Option[_]             => opt.isDefined
      case coll: TraversableOnce[_]   => coll.nonEmpty
      case _                          => true
    }
  }

  def isEmpty = !nonEmpty

  def toPlayJson: JsObject = {
    var acc: FieldsJsonAcc = Nil
    if (point.isDefined)
      acc ::= POINT_ESFN -> point.get.toPlayGeoJson
    if (directParentIds.nonEmpty)
      acc ::= DIRECT_PARENT_NODES_ESFN -> strIds2jsStrArray(directParentIds)
    if (allParentIds.nonEmpty)
      acc ::= ALL_PARENT_NODES_ESFN -> strIds2jsStrArray(allParentIds)
    JsObject(acc)
  }

}


// Аддоны для dyn-search по полям этой модели.
/** Аддон для поиска по прямым гео-родителями. */
trait DirectGeoParentsDsa extends DynSearchArgs {

  /** Искать по прямым гео-родителям. Нужно чтобы у узлов была проставлена инфа по геородителям. */
  def withDirectGeoParents: Seq[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Отрабатываем прямых гео-родителей
      if (withDirectGeoParents.nonEmpty) {
        val filter = FilterBuilders.termsFilter(GEO_DIRECT_PARENT_NODES_ESFN, withDirectGeoParents : _*)
        QueryBuilders.filteredQuery(qb, filter)
      } else {
        qb
      }
    }.orElse[QueryBuilder] {
      if (withDirectGeoParents.nonEmpty) {
        val qb = QueryBuilders.termsQuery(GEO_DIRECT_PARENT_NODES_ESFN, withDirectGeoParents: _*)
        Some(qb)
      } else {
        None
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(withDirectGeoParents, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withDirectGeoParents", withDirectGeoParents, super.toStringBuilder)
  }
}

trait DirectGeoParentsDsaDflt extends DirectGeoParentsDsa {
  override def withDirectGeoParents: Seq[String] = Seq.empty
}

trait DirectGeoParentsDsaWrapper extends DirectGeoParentsDsa with DynSearchArgsWrapper {
  override type WT <: DirectGeoParentsDsa
  override def withDirectGeoParents = _dsArgsUnderlying.withDirectGeoParents
}



/** Аддон для dyn-search для поддержки поиска/фильтрации по withGeoParents. */
trait GeoParentsDsa extends DynSearchArgs {

  /** Искать по гео-родителям любого уровня. */
  def withGeoParents: Seq[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Отрабатываем поиск по любым гео-родителям
      if (withGeoParents.nonEmpty) {
        val filter = FilterBuilders.termsFilter(GEO_ALL_PARENT_NODES_ESFN, withGeoParents : _*)
        QueryBuilders.filteredQuery(qb, filter)
      } else {
        qb
      }
    }.orElse[QueryBuilder] {
      if (withGeoParents.nonEmpty) {
        val qb = QueryBuilders.termsQuery(GEO_ALL_PARENT_NODES_ESFN, withGeoParents : _*)
        Some(qb)
      } else {
        None
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(withGeoParents, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withGeoParents", withGeoParents, super.toStringBuilder)
  }

}

trait GeoParentsDsaDflt extends GeoParentsDsa {
  override def withGeoParents: Seq[String] = Seq.empty
}

trait GeoParentsDsaWrapper extends GeoParentsDsa with DynSearchArgsWrapper {
  override type WT <: GeoParentsDsa
  override def withGeoParents = _dsArgsUnderlying.withGeoParents
}



/** DynSearch-аддон для поддержки поля geoDistance. */
trait GeoDistanceDsa extends DynSearchArgs {

  /** Фильтровать по дистанции относительно какой-то точки. */
  def geoDistance: Option[GeoShapeQueryData]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Отрабатываем geoDistance через geoShape'ы. Текущий запрос обнаружения описываем как круг.
      // Если задан внутренний вырез (minDistance), то используем другое поле location и distance filter, т.к. SR.WITHIN не работает.
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

    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(geoDistance, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("geoDistance", geoDistance, super.toStringBuilder)
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

}

trait GeoDistanceDsaDflt extends GeoDistanceDsa {
  override def geoDistance: Option[GeoShapeQueryData] = None
}

trait GeoDistanceDsaWrapper extends GeoDistanceDsa with DynSearchArgsWrapper {
  override type WT <: GeoDistanceDsa
  override def geoDistance = _dsArgsUnderlying.geoDistance
}



/** DynSearch-аддон для поиска по уже проиндексированным шейпам. Этот аддон не привязан к модели EMAdnNodeGeo. */
trait GeoIntersectsWithPreIndexedDsa extends DynSearchArgs {

  /** Пересечение с шейпом другого узла. Полезно для поиска узлов, географически входящих в указанный узел. */
  def intersectsWithPreIndexed: Seq[GeoShapeIndexed]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Отрабатываем поиск пересечения с другими узлами (с другими индексированными шейпами).
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
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(intersectsWithPreIndexed, super.sbInitSize, 10)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("intersectsWithPreIndexed", intersectsWithPreIndexed, super.toStringBuilder)
  }
}

trait GeoIntersectsWithPreIndexedDsaDftl extends GeoIntersectsWithPreIndexedDsa {
  override def intersectsWithPreIndexed: Seq[GeoShapeIndexed] = Seq.empty
}

trait GeoIntersectsWithPreIndexedDsaWrapper extends GeoIntersectsWithPreIndexedDsa with DynSearchArgsWrapper {
  override type WT <: GeoIntersectsWithPreIndexedDsa
  override def intersectsWithPreIndexed = _dsArgsUnderlying.intersectsWithPreIndexed
}



/** DynSearch-Аддон для сортировки по географическом удалению от указанной точки. */
trait GeoDistanceSortDsa extends DynSearchArgs {

  /** + сортировка результатов по расстоянию до указанной точки. */
  def withGeoDistanceSort: Option[GeoPoint]

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    // Добавить сортировку по дистанции до указанной точки, если необходимо.
    withGeoDistanceSort.foreach { geoPoint =>
      val sb = SortBuilders.geoDistanceSort(GEO_POINT_ESFN)
        .point(geoPoint.lat, geoPoint.lon)
        .order(SortOrder.ASC)   // ASC - ближайшие сверху, далёкие внизу.
        .unit(DistanceUnit.KILOMETERS)
      srb1.addSort(sb)
    }
    srb1
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(withGeoDistanceSort, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withGeoDistanceSort", withGeoDistanceSort, super.toStringBuilder)
  }
}

trait GeoDistanceSortDsaDflt extends GeoDistanceSortDsa {
  override def withGeoDistanceSort: Option[GeoPoint] = None
}

trait GeoDistanceSortDsaWrapper extends GeoDistanceSortDsa with DynSearchArgsWrapper {
  override type WT <: GeoDistanceSortDsa
  override def withGeoDistanceSort = _dsArgsUnderlying.withGeoDistanceSort
}

