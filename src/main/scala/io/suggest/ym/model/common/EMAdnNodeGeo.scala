package io.suggest.ym.model.common

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModelStaticMutAkvT, EsModel, EsModelPlayJsonT}
import io.suggest.model.geo.GeoPoint
import io.suggest.util.SioEsUtil._
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
