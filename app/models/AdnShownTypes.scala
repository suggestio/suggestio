package models

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
  }

  /** Это район города. */
  sealed trait TownDistrictT extends ValT {
    override def isTownDistrict = true
    override def ngls = nglsDistrict
  }
  private class TownDistrictVal(name: String) extends Val(name: String) with TownDistrictT
  
  /** Это здание. */
  sealed trait BuildingT extends ValT {
    override def isBuilding = true
    override def ngls = nglsBuilding
  }
  private class BuildingVal(name: String) extends Val(name) with BuildingT
  
  
  /**
   * Интанс этой модели.
   * @param name Название (обычно - однобуквенное).
   */
  protected[this] abstract sealed class Val(val name: String)
    extends super.Val(name)
    with ValT

  type AdnShownType = Val

  /** Расшаренные между экземплярами коллекции слоёв лежат тут для экономии RAM. */
  private val nglsBuilding = List(NodeGeoLevels.NGL_BUILDING)
  private val nglsDistrict = List(NodeGeoLevels.NGL_TOWN_DISTRICT)
  private val nglsTown     = List(NodeGeoLevels.NGL_TOWN)

  
  // Штатные (исторические) типы узлов
  val MART: AdnShownType              = new BuildingVal( AdNetMemberTypes.MART.name )
  val SHOP: AdnShownType              = new BuildingVal( AdNetMemberTypes.SHOP.name )
  val RESTAURANT: AdnShownType        = new BuildingVal( AdNetMemberTypes.RESTAURANT.name )
  val RESTAURANT_SUP: AdnShownType    = new Val( AdNetMemberTypes.RESTAURANT_SUP.name ) {
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

  /** Город. */
  val TOWN: AdnShownType = new Val("d") {
    override def ngls = nglsTown
    override def showWithTown = false
    override def isTown = true
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

  implicit def adnInfo2val(adnInfo: AdNetMemberInfo): AdnShownType = shownTypeId2val(adnInfo.shownTypeId)
  implicit def shownTypeId2val(sti: String): AdnShownType = withName(sti)


  /** Все типы районов. */
  def districts = Set(TOWN_DISTRICT, CITY_DISTRICT)
  val districtNames = districts.iterator.map(_.name).toSeq
}
