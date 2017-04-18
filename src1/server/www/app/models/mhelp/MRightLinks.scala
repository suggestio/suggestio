package models.mhelp

import enumeratum._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 16:44
  * Description: Модель ссылок правой панели навигации в разделе ЛК/Помощь.
  */

sealed class MRightLink extends EnumEntry

object MRightLinks extends Enum[MRightLink] {

  /** Страница запроса в техподдержку. */
  case object Support extends MRightLink

  /** Страница "О компании". */
  case object CompanyAbout extends MRightLink


  override def values = findValues

}
