package io.suggest.common.slide.block

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 17:00
 * Description: Константы для слайд-блоков, частоиспользуемых в личном кабинете.
 */
object SbConstants {

  // TODO СЕСТЬ И ПРОПИСАТЬ ЭТО ВСЁ В ШАБЛОНЫ, ИБО ТАМ ПОКА ВСЁ ЗАХАРДКОЖЕНО!

  /** Класс контейнера. */
  def CONTAINER_CLASS   = "slide-block"

  /** Префикс классов под-контейнеров. */
  def CLASS_PREFIX      = CONTAINER_CLASS + "_"

  /** Класс контейнера заголовка стайд-блока. */
  def TITLE_CLASS       = CLASS_PREFIX + "title"

  /** Класс контейнера тела слайд-блока. */
  def CONTENT_CLASS     = CLASS_PREFIX + "cnt"

}
