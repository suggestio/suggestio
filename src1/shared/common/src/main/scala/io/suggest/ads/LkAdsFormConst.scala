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

  /** Кол-во карточек (блоков) в одном ряду. */
  final def ADS_PER_ROW = 4

  final def ONE_ITEM_FULL_HEIGHT_PX = 250

  /** Сколько примерно от вершины страницы до начала плитки? */
  final def GRID_TOP_SCREEN_OFFSET_PX = 200

  /** Кол-во скролла вниз, побуждающий к подгрузке ещё карточек. */
  // Копипаст из TileConstants
  final def LOAD_MORE_SCROLL_DELTA_PX = 100

}
