package io.suggest.ad.edit.v.edit.content

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.color.MColorData
import io.suggest.common.html.HtmlConstants
import io.suggest.css.CssR
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.internal.mutable.StyleSheet
import io.suggest.css.ScalaCssDefaults._
import io.suggest.img.MImgFormats
import io.suggest.pick.MimeConst
import io.suggest.react.ReactDiodeUtil
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.18 22:38
  * Description: Компонент для css-костылей поверх content-редактора.
  */
class ContentEditCssR {

  case class PropsVal(
                       bgColor: Option[MColorData]
                     )
  implicit object ContentEditCssRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.bgColor ===* b.bgColor
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  /** Stylesheet, который рендерится. */
  class ContentCss(propsVal: PropsVal) extends StyleSheet.Standalone {

    import dsl._
    import com.quilljs.quill.modules.QuillCssConst._

    QL_EDITOR_CSS_SEL - (
      maxHeight(200.px)
    )

    // Задать фон для содержимого контент-редактора.
    for (bgColor <- propsVal.bgColor) {
      QL_EDITOR_CSS_SEL - (
        backgroundColor( Color( bgColor.hexCode ) )
      )
    }

    // Заменить уголок ресайза.
    ".react-resizable-handle" - (
      backgroundImage :=  s"""url('${HtmlConstants.Proto.DATA_}${MImgFormats.SVG.mime};${MimeConst.Words.BASE64},PHN2ZyBpZD0i0KHQu9C+0LlfMSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB2aWV3Qm94PSIwIDAgNiA2Ij48c3R5bGU+LnN0MHtmaWxsOm5vbmV9PC9zdHlsZT48cGF0aCBkPSJNNiA2SDBWNC4yaDQuMlYwSDZ2NnoiLz48L3N2Zz4=')"""
    )

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .render_P { propsProxy =>
      <.styleTag(
        new ContentCss(propsProxy.value)
          .render[String]
      )
    }
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

  def apply(propsValProxy: Props): VdomElement = CssR.compProxied( propsValProxy.zoom(new ContentCss(_)) ) //component( propsValProxy )

}
