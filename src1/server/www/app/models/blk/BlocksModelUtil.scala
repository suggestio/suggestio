package models.blk

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
trait IBrArgs {
  /** Данные для block render. */
  def brArgs: RenderArgs
}
