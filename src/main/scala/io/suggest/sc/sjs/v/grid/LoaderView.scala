package io.suggest.sc.sjs.v.grid

import io.suggest.sc.ScConstants
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.05.15 9:50
 * Description: Управлением loader'ом сетки карточек.
 */
object LoaderView {

  type T = SafeCssElT { type T = HTMLDivElement }

  def show(loaderDiv: T): Unit = {
    loaderDiv.addClasses( ScConstants.HIDDEN_CSS_CLASS )
  }

  def hide(loaderDiv: T): Unit = {
    loaderDiv.removeClass( ScConstants.HIDDEN_CSS_CLASS )
  }

}
