package io.suggest.jd.render.v

import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.css.CssR
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 20:51
  * Description: React-компонент для рендера JdCss в style-тэг.
  * 2018-08-02 CssR умеет это всё. Тут просто враппер, потому что есть проблема с компиляцией (см.комменты в CssR).
  */
class JdCssR {

  type Props_t = JdCss
  type Props = ModelProxy[Props_t]


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { CssR(_) }
    .build


  private def _apply(jdCssArgsProxy: Props) = component( jdCssArgsProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
