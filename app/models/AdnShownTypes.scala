package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 16:44
 * Description: Enum-список отображаемых типов узлов.
 */
object AdnShownTypes extends Enumeration {

  type AdnShownType = Value

  // Штатные (исторические) типы узлов
  val MART: AdnShownType              = Value( AdNetMemberTypes.MART.name )
  val SHOP: AdnShownType              = Value( AdNetMemberTypes.SHOP.name )
  val RESTAURANT: AdnShownType        = Value( AdNetMemberTypes.RESTAURANT.name )
  val RESTAURANT_SUP: AdnShownType    = Value( AdNetMemberTypes.RESTAURANT_SUP.name )

  // Пользовательские типы узлов. Для id'шников можно использовать идентификаторы, не использованные в вышеуказанных вещах.
  // При совпадении двух id будет ошибка после запуска при первом обращении к этой модели.
  val TRANSPORT_NODE: AdnShownType    = Value("a")    // Вокзалы, аэропорты и др. более-менее крупные транспортные узлы.
  val METRO_STATION: AdnShownType     = Value("b")    // Станция метро

  // При добавлении новых элементов, нужно добавлять в conf/messages.* соответствующие "amt.of.type.X" и "amts.of.type.X".
  def maybeWithName(n: String) = {
    try {
      Some(withName(n))
    } catch {
      case ex: Exception => None
    }
  }

}
