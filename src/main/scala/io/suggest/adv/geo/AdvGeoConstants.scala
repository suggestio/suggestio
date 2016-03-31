package io.suggest.adv.geo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 17:43
  * Description: Константы страницы/формы размещения карточки в гео-тегах.
  */
object AdvGeoConstants {

  def PREFIX = "ag"

  /** id формы размещения в гео-месте. */
  def FORM_ID = "f" + PREFIX

  object OnMainScreen {

    def FN = "oms"

    def ID = FORM_ID + "." + FN

  }

}
