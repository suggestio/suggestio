package io.suggest.sc.sjs.v.grid

import io.suggest.sc.ScConstants
import io.suggest.sjs.common.view.safe.css.SafeCssElT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.05.15 9:50
 * Description: Управлением loader'ом сетки карточек.
 */
object LoaderView {

  /**
   * Отобразить крутилку лоадера.
   * @param loaderDiv div лоадера.
   */
  def show(loaderDiv: SafeCssElT): Unit = {
    loaderDiv.removeClass( ScConstants.HIDDEN_CSS_CLASS )
  }

  /**
   * Скрыть крутилку лоадера. Не удаляем, т.к. при поиске лоадер придется вернуть.
   * @param loaderDiv div лоадера.
   */
  def hide(loaderDiv: SafeCssElT): Unit = {
    loaderDiv.addClasses( ScConstants.HIDDEN_CSS_CLASS )
  }

}
