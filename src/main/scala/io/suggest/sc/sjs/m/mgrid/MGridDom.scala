package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.ScConstants.Grid._
import io.suggest.sc.sjs.vm.util.domvm.get.GetDivById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 10:22
 * Description: Доступ к DOM плитки карточек.
 */
trait MGridDomT extends GetDivById {

  /** Найти div grid-контейнера. */
  def containerDiv    = getDivById(CONTAINER_DIV_ID)

  /** Найти div отображения загрузчика. */
  def loaderDiv       = getDivById(LOADER_DIV_ID)

  /** Найти grid wrapper div. */
  def wrapperDiv      = getDivById(WRAPPER_DIV_ID)

  /** Найти grid div id. */
  def rootDiv         = getDivById(ROOT_DIV_ID)

  /** Найти content div. */
  def contentDiv      = getDivById(CONTENT_DIV_ID)

}

object MGridDom extends MGridDomT
