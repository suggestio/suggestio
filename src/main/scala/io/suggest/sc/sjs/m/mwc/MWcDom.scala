package io.suggest.sc.sjs.m.mwc

import io.suggest.sc.ScConstants.Welcome._
import io.suggest.sc.sjs.vm.util.domvm.get.{GetImgById, GetDivById}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 10:26
 * Description: Доступ к DOM-элементам карточки приветствия.
 */
object MWcDom extends GetDivById with GetImgById {

  /** Найти div-контейнер карточки приветствия. */
  def rootDiv()  = getDivById(ROOT_ID)

  def bgImg()    = getImgById(BG_IMG_ID)

  def fgImg()    = getImgById(FG_IMG_ID)

  def fgInfo()   = getDivById(FG_INFO_DIV_ID)

}
