package io.suggest.ad.edit.v.edit.content

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.color.MColorData
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.internal.mutable.StyleSheet
import io.suggest.css.ScalaCssDefaults._
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

  def apply(propsValProxy: Props) = component( propsValProxy )

}
