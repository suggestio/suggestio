package models.mext.tw

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 18:26
 * Description: Поддерживаемые типы twitter-карточек.
 */
object CardTypes extends Enumeration {

  /** Экземпляр модели. */
  sealed protected class Val(name: String) extends super.Val(name)

  type T = Val

  val Photo: T = new Val("photo")

}
