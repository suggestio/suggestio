package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 16:44
 * Description: Enum-список отображаемых типов узлов.
 */
object AdnShownTypes extends Enumeration {

  protected sealed case class Val(name: String, showWithTown: Boolean = true) extends super.Val(name) {
    /** Код локализованного названия в единственном числе. */
    val singular = "amt.of.type." + name

    /** Код локализованного названия во множественном числе. */
    val plural   = "amts.of.type." + name

    def pluralNoTown = plural
    def singularNoTown = singular
  }

  type AdnShownType = Val

  // Штатные (исторические) типы узлов
  val MART: AdnShownType              = Val( AdNetMemberTypes.MART.name )
  val SHOP: AdnShownType              = Val( AdNetMemberTypes.SHOP.name )
  val RESTAURANT: AdnShownType        = Val( AdNetMemberTypes.RESTAURANT.name )
  val RESTAURANT_SUP: AdnShownType    = Val( AdNetMemberTypes.RESTAURANT_SUP.name )

  // Пользовательские типы узлов. Для id'шников можно использовать идентификаторы, не использованные в вышеуказанных вещах.
  // При совпадении двух id будет ошибка после запуска при первом обращении к этой модели.
  val TRANSPORT_NODE: AdnShownType    = Val("a")    // Вокзалы, аэропорты и др. более-менее крупные транспортные узлы.
  val METRO_STATION: AdnShownType     = Val("b")    // Станция метро
  // Район города
  val TOWN_DISTRICT: AdnShownType     = new Val("c") {
    override val singularNoTown = "District"
    override val pluralNoTown   = "Districts"
  }
  val TOWN: AdnShownType              = Val("d", showWithTown = false)    // Город

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
