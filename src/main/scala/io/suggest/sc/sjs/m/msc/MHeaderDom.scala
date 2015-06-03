package io.suggest.sc.sjs.m.msc

import io.suggest.sc.ScConstants.Header._
import io.suggest.sc.sjs.m.mdom.GetDivById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.15 10:16
 * Description: Доступ к DOM строки заголовка.
 */
trait MHeaderDomT extends GetDivById {

  def rootDiv       = getDivById(ROOT_DIV_ID)

  def showIndexBtn  = getDivById(SHOW_INDEX_BTN_ID)

}

/** Дефолтовая реализация модели доступа к DOM строки заголовка. */
object MHeaderDom extends MHeaderDomT
