package io.suggest.grid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 13:03
 * Description: Константы плитки выдачи suggest.io.
 */
object GridConst {

  // TODO Распихать обе константы внутрь по обоим элементам BlockWidths.
  def CELL300_COLUMNS_MAX = 4
  def CELL140_COLUMNS_MAX = CELL300_COLUMNS_MAX * 2

  /** Сдвиг контейнера плитки по вертикали.
    * Состоит из высоты заголовка + 20 px. szMult не влияет. */
  def CONTAINER_OFFSET_TOP = 70

  /** Сколько места запасти под карточками. */
  def CONTAINER_OFFSET_BOTTOM = 20

  /** Кол-во скролла вниз, побуждающий к подгрузке ещё карточек. */
  def LOAD_MORE_SCROLL_DELTA_PX = 100

}
