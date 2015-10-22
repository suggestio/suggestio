package models

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.n2.extra.MAdnExtra
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 16:44
 * Description: Enum-список отображаемых типов узлов.
 */
object AdnShownTypes extends EnumMaybeWithName {
  
  /** Трейт экземпляра модели. */
  sealed trait ValT {
    def name: String
    
    /** Код локализованного названия в единственном числе. */
    def singular = "amt.of.type." + name

    /** Код локализованного названия во множественном числе. */
    def plural   = "amts.of.type." + name

    def pluralNoTown = plural
    def singularNoTown = singular

    /** Является ли тип узла внутренним районом какого-то населенного пункта? */
    def isTownDistrict: Boolean = false
    def isBuilding: Boolean = false
    def isTown: Boolean = false

    def ngls: List[NodeGeoLevel]
    def showWithTown: Boolean = true

    /** Имеет ли смысл пытаться искать гео-дочерние узлы у узла данного уровня. */
    def mayHaveGeoChildren: Boolean = true

    /** Является ли данный тип географически-верхним?
      * Изначально была иерархия: город-район-здание.
      * Значит города верхние, остальные -- нет. */
    def isTopLevel: Boolean = false
  }


  /**
   * Интанс этой модели.
   * @param name Название (обычно - однобуквенное).
   */
  protected[this] abstract class Val(val name: String)
    extends super.Val(name)
    with ValT


  /** Это район города. */
  sealed protected trait TownDistrictT extends ValT {
    override def isTownDistrict = true
    override def ngls = List(NodeGeoLevels.NGL_TOWN_DISTRICT)
  }
  private class TownDistrictVal(name: String) extends Val(name: String) with TownDistrictT
  
  /** Это здание. */
  sealed trait BuildingT extends ValT {
    override def isBuilding = true
    override def ngls = List(NodeGeoLevels.NGL_BUILDING)
    override def mayHaveGeoChildren = false
  }
  private class BuildingVal(name: String) extends Val(name) with BuildingT
  

  override type T = Val


  // Штатные (исторические) типы узлов
  val MART: T              = new BuildingVal("m")
  val SHOP: T              = new BuildingVal("s")
  val RESTAURANT: T        = new BuildingVal("r")
  @deprecated("Please remove, if unused", "2015.sep.25")
  val RESTAURANT_SUP: T    = new Val("R") {
    override def ngls = Nil
  }

  // Пользовательские типы узлов. Для id'шников можно использовать идентификаторы, не использованные в вышеуказанных вещах.
  // При совпадении двух id будет ошибка после запуска при первом обращении к этой модели.

  /** Вокзалы, аэропорты и др. более-менее крупные транспортные узлы. */
  val TRANSPORT_NODE: T = new BuildingVal("a")

  /** Станция метро. */
  val METRO_STATION: T = new BuildingVal("b")

  /** Район города. */
  val TOWN_DISTRICT: T = new TownDistrictVal("c") {
    override def singularNoTown = "District"
    override def pluralNoTown   = "Districts"
  }

  /** Город, населенный пункт. */
  val TOWN: T = new Val("d") {
    override def ngls = List(NodeGeoLevels.NGL_TOWN)
    override def showWithTown = false
    override def isTown = true
    override def isTopLevel = true
  }

  /** Спортивные объекты: фитнес, стадионы и т.д. */
  val SPORT: T = new BuildingVal("e")

  /** Округ мегаполиса. Логически находится над городскими районами, но занимает слой районов. */
  val CITY_DISTRICT: T = new TownDistrictVal("f") {
    override def pluralNoTown = "Areas"
    override def singularNoTown = "Area"
  }
  
  /** Другое здание. */
  val OTHER_BUILDING: T = new BuildingVal("g") {
    override def pluralNoTown = "Others"
    override def singularNoTown = "Other"
  }


  // При добавлении новых элементов, нужно добавлять в conf/messages.* соответствующие "amt.of.type.X" и "amts.of.type.X".


  implicit def adnInfo2val(adnInfo: MAdnExtra): T = {
    adnInfo.shownTypeIdOpt
      .map( shownTypeId2val )
      .getOrElse( default )
  }
  def node2val(mnode: MNode): T = {
    mnode.extras.adn
      .fold(default)(adnInfo2val)
  }

  implicit def shownTypeId2val(sti: String): T = withName(sti)


  /** Все типы районов. */
  def districts = Set(TOWN_DISTRICT, CITY_DISTRICT)

  /** Часто используется при сборке списков узлов. */
  val districtNames = districts.iterator.map(_.name).toSeq

  /** Дефолтовое значение. Используется когда очень нужно, а подходящего значения нет. */
  def default = SHOP


  /** Поддержка со стороны play router'а. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindable[T] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for (nameEith <- strB.bind(key, params)) yield {
          nameEith.right.flatMap { name =>
            maybeWithName(name).fold [Either[String, T]] {
              Left("shown.type.name.unknown")
            } { ast =>
              Right(ast)
            }
          }
        }
      }
      override def unbind(key: String, value: T): String = {
        strB.unbind(key, value.name)
      }
    }
  }

}
