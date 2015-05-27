package io.suggest.sc.sjs.v.grid

import io.suggest.sc.ScConstants
import io.suggest.sc.sjs.m.mgrid.ICwCm
import io.suggest.sc.sjs.m.mgrid.MGridDom._
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:15
 * Description: view плитки рекламный карточек. Он получает команды от контроллеров или других view'ов,
 * поддерживая тем самым состояние отображаемой плитки карточек.
 */
object GridView {

  /**
   * Выставить новый размер контейнера сетки.
   * @param sz Данные по новому размеру.
   */
  def setContainerSz(sz: ICwCm): Unit = {
    val width = sz.cw.toString + "px"

    containerDiv().foreach { cont =>
      cont.style.width    = width
      cont.style.left     = (sz.cm/2).toString + "px"
      cont.style.opacity  = "1"
    }

    loaderDiv().foreach { loader =>
      loader.style.width  = width
    }
  }


  /** Управлением loader'ом. */
  object Loader {

    type T = SafeCssElT { type T = HTMLDivElement }

    def show(loaderDiv: T): Unit = {
      loaderDiv.addClasses( ScConstants.HIDDEN_CSS_CLASS )
    }

    def hide(loaderDiv: T): Unit = {
      loaderDiv.removeClass( ScConstants.HIDDEN_CSS_CLASS )
    }

  }

}
