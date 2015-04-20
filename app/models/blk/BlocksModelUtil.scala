package models.blk

import models.MAd

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:44
 * Description: Утиль для моделей blocks.
 */

/** Интерфейс, объединяющий целочисленные параметры блока, такие как ширина и длина. */
trait IntParam {
  def intValue: Int
}

/** Для экземпляра модели доступна мера относительного размера. */
trait RelSz {
  def relSz: Int
}


/** Интерфейс для целого ряда tpl-моделей. */
trait IMadAndArgs {
  /** Рекламная карточка. */
  def mad     : MAd
  /** Данные для block render. */
  def brArgs  : RenderArgs
}
