package io.suggest.ads

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 18:57
  * Description: Константы для формы управления карточками узла.
  */
object LkAdsFormConst {

  final def FORM_CONT_ID = "lkadsc"

  /** Кол-во карточек за один реквест к серверу.
    * Может быть и больше, и меньше этого кол-ва.
    */
  final def GET_ADS_COUNT_PER_REQUEST = 16

}
