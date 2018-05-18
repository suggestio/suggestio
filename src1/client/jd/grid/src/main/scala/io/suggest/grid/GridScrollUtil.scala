package io.suggest.grid

import io.suggest.text.StringUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.18 11:20
  * Description: Утиль для организации скроллинга в плитке.
  */
object GridScrollUtil {

  /** Рандомный id для grid-wrapper.  */
  // TODO Если используем рандомные id, то надо избежать комбинаций букв, приводящих к срабатываниям блокировщиков рекламы.
  lazy val SCROLL_CONTAINER_ID = StringUtil.randomIdLatLc()

}
