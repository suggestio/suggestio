package io.suggest.sjs.common.util

import org.scalajs.jquery.JQuery

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.05.15 17:58
 * Description: Интерфейс для перечисления контейнеров.
 */

trait IContainers {
  /** Контейнеры с input'ами/полями управления изображениями, которые будут отработаны. */
  protected def _imgInputContainers: TraversableOnce[JQuery]
}
