package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 16:44
 * Description: Enum-список отображаемых типов узлов.
 */
object AdnShownTypes extends Enumeration {

  /**
   * Интанс этой модели.
   * @param name Название (обычно - однобуквенное).
   * @param ngls Список дефолтовых геоуровней.
   * @param showWithTown Флаг того, что нужно отображать в паре с городом. false для городов или выше.
   */
  protected sealed case class Val(
    name: String,
    ngls: List[NodeGeoLevel],
    showWithTown: Boolean = true
  ) extends super.Val(name) {
    /** Код локализованного названия в единственном числе. */
    val singular = "amt.of.type." + name

    /** Код локализованного названия во множественном числе. */
    val plural   = "amts.of.type." + name

    def pluralNoTown = plural
    def singularNoTown = singular
  }

  type AdnShownType = Val

  private val nglsBuilding = List(NodeGeoLevels.NGL_BUILDING)

  // Штатные (исторические) типы узлов
  val MART: AdnShownType              = Val( AdNetMemberTypes.MART.name, nglsBuilding )
  val SHOP: AdnShownType              = Val( AdNetMemberTypes.SHOP.name, nglsBuilding )
  val RESTAURANT: AdnShownType        = Val( AdNetMemberTypes.RESTAURANT.name, nglsBuilding )
  val RESTAURANT_SUP: AdnShownType    = Val( AdNetMemberTypes.RESTAURANT_SUP.name, Nil )

  // Пользовательские типы узлов. Для id'шников можно использовать идентификаторы, не использованные в вышеуказанных вещах.
  // При совпадении двух id будет ошибка после запуска при первом обращении к этой модели.

  /** Вокзалы, аэропорты и др. более-менее крупные транспортные узлы. */
  val TRANSPORT_NODE: AdnShownType    = Val("a", nglsBuilding)

  /** Станция метро. */
  val METRO_STATION: AdnShownType     = Val("b", nglsBuilding)

  /** Район города. */
  val TOWN_DISTRICT: AdnShownType     = new Val("c", List(NodeGeoLevels.NGL_TOWN_DISTRICT)) {
    override val singularNoTown = "District"
    override val pluralNoTown   = "Districts"
  }

  /** Город. */
  val TOWN: AdnShownType              = Val("d", List(NodeGeoLevels.NGL_TOWN), showWithTown = false)


  // При добавлении новых элементов, нужно добавлять в conf/messages.* соответствующие "amt.of.type.X" и "amts.of.type.X".
  def maybeWithName(n: String): Option[AdnShownType] = {
    try {
      Some(withName(n))
    } catch {
      case ex: Exception => None
    }
  }

  implicit def value2val(x: Value): AdnShownType = x.asInstanceOf[AdnShownType]

  implicit def adnInfo2val(adnInfo: AdNetMemberInfo): AdnShownType = shownTypeId2val(adnInfo.shownTypeId)
  implicit def shownTypeId2val(sti: String): AdnShownType = withName(sti)
}
