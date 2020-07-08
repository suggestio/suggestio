package models.madn

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.{EnumeratumJvmUtil, EnumeratumUtil}
import io.suggest.geo.{MNodeGeoLevel, MNodeGeoLevels}
import io.suggest.n2.extra.MAdnExtra
import io.suggest.n2.node.MNode
import japgolly.univeq.UnivEq
import play.api.libs.json.Format
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 16:44
 * Description: Enum-список отображаемых типов узлов.
 */
object AdnShownTypes extends StringEnum[AdnShownType] {

  // Штатные (исторические) типы узлов
  case object MART extends AdnShownType.Building("m")
  case object SHOP extends AdnShownType.Building("s")
  case object RESTAURANT extends AdnShownType.Building("r")

  // Пользовательские типы узлов. Для id'шников можно использовать идентификаторы, не использованные в вышеуказанных вещах.
  // При совпадении двух id будет ошибка после запуска при первом обращении к этой модели.

  /** Вокзалы, аэропорты и др. более-менее крупные транспортные узлы. */
  case object TRANSPORT_NODE extends AdnShownType.Building("a")

  /** Станция метро. */
  case object METRO_STATION extends AdnShownType.Building("b")

  /** Район города. */
  case object TOWN_DISTRICT extends AdnShownType.TownDistrict("c") {
    override def singularNoTown = "District"
    override def pluralNoTown   = singularNoTown + "s"
  }

  /** Город, населенный пункт. */
  case object TOWN extends AdnShownType("d") {
    override def ngls = List(MNodeGeoLevels.NGL_TOWN)
    override def showWithTown = false
    override def isTown = true
    override def isTopLevel = true
  }

  /** Спортивные объекты: фитнес, стадионы и т.д. */
  case object SPORT extends AdnShownType.Building("e")

  /** Округ мегаполиса. Логически находится над городскими районами, но занимает слой районов. */
  case object CITY_DISTRICT extends AdnShownType.TownDistrict("f") {
    override def pluralNoTown = "Areas"
    override def singularNoTown = "Area"
  }

  /** Другое здание. */
  case object OTHER_BUILDING extends AdnShownType.TownDistrict("g") {
    override def pluralNoTown = "Others"
    override def singularNoTown = "Other"
  }


  // При добавлении новых элементов, нужно добавлять в conf/messages.* соответствующие "amt.of.type.X" и "amts.of.type.X".

  override def values = findValues

  // Три метода, тут неуместные...
  def adnInfo2val(adnInfo: MAdnExtra): AdnShownType = {
    adnInfo.shownTypeIdOpt
      .flatMap( withValueOpt )
      .getOrElse( default )
  }
  def node2valOpt(mnode: MNode): Option[AdnShownType] = {
    mnode.extras.adn
      .map( adnInfo2val )
  }
  def node2val(mnode: MNode): AdnShownType = {
    node2valOpt(mnode)
      .getOrElse(default)
  }


  /** Все типы районов. */
  def districts: Set[AdnShownType] =
    Set.empty[AdnShownType] + TOWN_DISTRICT + CITY_DISTRICT

  /** Часто используется при сборке списков узлов. */
  def districtNames = districts.iterator.map(_.value).toSeq

  /** Дефолтовое значение. Используется когда очень нужно, а подходящего значения нет. */
  def default = SHOP

}


sealed abstract class AdnShownType(override val value: String) extends StringEnumEntry {

  /** Код локализованного названия в единственном числе. */
  def singular = "amt.of.type." + value

  /** Код локализованного названия во множественном числе. */
  def plural   = "amts.of.type." + value

  def pluralNoTown = plural
  def singularNoTown = singular

  /** Является ли тип узла внутренним районом какого-то населенного пункта? */
  def isTownDistrict: Boolean = false
  def isBuilding: Boolean = false
  def isTown: Boolean = false

  def ngls: List[MNodeGeoLevel]
  def showWithTown: Boolean = true

  /** Имеет ли смысл пытаться искать гео-дочерние узлы у узла данного уровня. */
  def mayHaveGeoChildren: Boolean = true

  /** Является ли данный тип географически-верхним?
    * Изначально была иерархия: город-район-здание.
    * Значит города верхние, остальные -- нет. */
  def isTopLevel: Boolean = false

}

object AdnShownType {

  implicit def adnShownTypeFormat: Format[AdnShownType] =
    EnumeratumUtil.valueEnumEntryFormat( AdnShownTypes )

  @inline implicit def univEq: UnivEq[AdnShownType] = UnivEq.derive

  implicit def adnShownTypeQsb: QueryStringBindable[AdnShownType] =
    EnumeratumJvmUtil.valueEnumQsb( AdnShownTypes )


  sealed abstract class TownDistrict(value: String) extends AdnShownType(value) {
    override def isTownDistrict = true
    override def ngls = List(MNodeGeoLevels.NGL_TOWN_DISTRICT)
  }

  sealed abstract class Building(value: String) extends AdnShownType(value) {
    override def isBuilding = true
    override def ngls = List(MNodeGeoLevels.NGL_BUILDING)
    override def mayHaveGeoChildren = false
  }

}
