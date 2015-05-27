package io.suggest.sc.sjs.m.mdom

import io.suggest.sc.ScConstants.Rsc._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 10:06
 * Description: Доступ к контейнерам ресурсов в DOM.
 */
object MResContainers extends GetDivById {

  /** Контейнер для всех ресурсов. */
  def commonResDiv() = getDivById(COMMON_ID)

  /** Контейнер для ресурсов focused ads. */
  def focusedResDiv() = getDivById(FOCUSED_ID)

}
