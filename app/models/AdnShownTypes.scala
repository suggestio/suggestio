package models

import io.suggest.model.n2.extra.MAdnExtra

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 16:44
 * Description: Enum-список отображаемых типов узлов.
 */
object AdnShownTypes extends Enumeration {
  
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
  protected[this] abstract sealed class Val(val name: String)
    extends super.Val(name)
    with ValT


  /** Это район города. */
  sealed trait TownDistrictT extends ValT {
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
  

  type AdnShownType = Val


  // Штатные (исторические) типы узлов
  val MART: AdnShownType              = new BuildingVal("m")
  val SHOP: AdnShownType              = new BuildingVal("s")
  val RESTAURANT: AdnShownType        = new BuildingVal("r")
  @deprecated("Please remove, if unused", "2015.sep.25")
  val RESTAURANT_SUP: AdnShownType    = new Val("R") {
    override def ngls = Nil
  }

  // Пользовательские типы узлов. Для id'шников можно использовать идентификаторы, не использованные в вышеуказанных вещах.
  // При совпадении двух id будет ошибка после запуска при первом обращении к этой модели.

  /** Вокзалы, аэропорты и др. более-менее крупные транспортные узлы. */
  val TRANSPORT_NODE: AdnShownType = new BuildingVal("a")

  /** Станция метро. */
  val METRO_STATION: AdnShownType = new BuildingVal("b")

  /** Район города. */
  val TOWN_DISTRICT: AdnShownType = new TownDistrictVal("c") {
    override def singularNoTown = "District"
    override def pluralNoTown   = "Districts"
  }

  /** Город, населенный пункт. */
  val TOWN: AdnShownType = new Val("d") {
    override def ngls = List(NodeGeoLevels.NGL_TOWN)
    override def showWithTown = false
    override def isTown = true
    override def isTopLevel = true
  }

  /** Спортивные объекты: фитнес, стадионы и т.д. */
  val SPORT: AdnShownType = new BuildingVal("e")

  /** Округ мегаполиса. Логически находится над городскими районами, но занимает слой районов. */
  val CITY_DISTRICT: AdnShownType = new TownDistrictVal("f") {
    override def pluralNoTown = "Areas"
    override def singularNoTown = "Area"
  }
  
  /** Другое здание. */
  val OTHER_BUILDING: AdnShownType = new BuildingVal("g") {
    override def pluralNoTown = "Others"
    override def singularNoTown = "Other"
  }


  // При добавлении новых элементов, нужно добавлять в conf/messages.* соответствующие "amt.of.type.X" и "amts.of.type.X".

  /** Опциональный поиск в этом множестве. */
  def maybeWithName(n: String): Option[AdnShownType] = {
    values
      .find(_.name == n)
      .asInstanceOf[Option[AdnShownType]]
  }

  implicit def value2val(x: Value): AdnShownType = x.asInstanceOf[AdnShownType]

  implicit def adnInfo2val(adnInfo: MAdnExtra): AdnShownType = {
    adnInfo.shownTypeIdOpt
      .map( shownTypeId2val )
      .getOrElse( default )
  }
  def node2val(mnode: MNode): AdnShownType = {
    mnode.extras.adn
      .fold(default)(adnInfo2val)
  }

  implicit def shownTypeId2val(sti: String): AdnShownType = withName(sti)


  /** Все типы районов. */
  def districts = Set(TOWN_DISTRICT, CITY_DISTRICT)

  /** Часто используется при сборке списков узлов. */
  val districtNames = districts.iterator.map(_.name).toSeq

  def default = SHOP

}
