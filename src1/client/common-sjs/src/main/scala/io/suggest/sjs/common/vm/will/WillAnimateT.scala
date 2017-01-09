package io.suggest.sjs.common.vm.will

import io.suggest.sc.ScConstants
import io.suggest.sjs.common.vm.css.CssClassT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.15 15:44
 * Description: Аддон для vm'ок для добавления public-API подготовки к анимации через will-change и css-классы.
 */
trait WillAnimateT extends CssClassT {

  /** Какой css-класс нужно добавлять/удалять для подготовки к анимации. */
  protected def WILL_ANIMATE_CLASS: String

  /** Активация will-change при подготовке к CSS3 анимации. */
  def willAnimate(): Unit = {
    addClasses(WILL_ANIMATE_CLASS)
  }

  /** Деактивация подготовки к CSS3-анимации. */
  def wontAnimate(): Unit = {
    removeClass(WILL_ANIMATE_CLASS)
  }

}


/** Обычно используется translate3d-анимация, поэтому тут реализация [[WillAnimateT]],
  * выставляющая соответствующий css-класс. */
trait WillTranslate3d extends WillAnimateT {
  override protected def WILL_ANIMATE_CLASS = ScConstants.CLASS_WILL_TRANSLATE3D
}