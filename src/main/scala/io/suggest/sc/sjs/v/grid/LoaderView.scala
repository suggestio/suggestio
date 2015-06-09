package io.suggest.sc.sjs.v.grid

import io.suggest.sc.ScConstants
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.05.15 9:50
 * Description: Управлением loader'ом сетки карточек.
 */
object LoaderView {

  def show(loaderDiv: SafeCssElT): Unit = {
    loaderDiv.removeClass( ScConstants.HIDDEN_CSS_CLASS )
  }

  def hide(loaderDiv: Node): Unit = {
    loaderDiv.parentNode.removeChild(loaderDiv)
  }

}
