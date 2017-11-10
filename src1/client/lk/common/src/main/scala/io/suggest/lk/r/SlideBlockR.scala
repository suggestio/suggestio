package io.suggest.lk.r

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.lk.m.SlideBlockClick
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCBf

import scalacss.internal.mutable.StyleSheet
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.17 10:35
  * Description: Реализация slide-block'ов на базе существующей lk-вёрстки с добавлением
  * [[https://codepen.io/adamaoc/pen/wBGGQv]], т.е. нативная анимация и всё такое.
  */

trait SlideBlockCss extends StyleSheet.Inline {

  import dsl._

  /** Непосредственно стили slide-блоков, которые можно перезаписывать при необходимости. */
  protected trait SlideBlockStylesT {

    import Css.Lk.SlideBlocks._

    val outer = style(
      addClassName( OUTER ),
      minWidth( 350.px )
    )

    val title = style(
      addClassName( TITLE )
    )

    val titleExpanded = style(
      addClassName( TITLE_OPENED )
    )

    val titleBtn = style(
      addClassName( TITLE_BTN )
    )

    val bodyWrap = {
      val T = Css.Anim.Transition
      style(
        addClassName( Css.Lk.SlideBlocks.BODY ),
        height(0.px),
        background := "none",
        overflow.hidden,
        transition := T.all(0.2, T.TimingFuns.EASE_IN)
      )
    }

    val bodyWrapExpanded = style(
      height.auto
    )

  }

  val SlideBlock = new SlideBlockStylesT {}

}


/** DI-инжектируемая утиль для сборки slide-block'а, завязанного на react+diode. */
class SlideBlockR(
                  slideBlockCss: SlideBlockCss
                ) {

  case class PropsVal(
                       title      : String,
                       expanded   : Boolean,
                       key        : Option[String]
                     )
  implicit object SlideBlockPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.title ===* b.title &&
        a.expanded ==* b.expanded &&
        a.key ===* b.key
    }
  }

  type Props = ModelProxy[Option[PropsVal]]

  class Backend($: BackendScope[Props, Unit]) {

    private def onTitleClick: Callback = {
      dispatchOnProxyScopeCBf($) { props =>
        SlideBlockClick(
          key = props.value
            .flatMap(_.key)
            .get
        )
      }
    }

    def render(propsOptProxy: Props, children: PropsChildren): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        val CSS = slideBlockCss.SlideBlock
        <.div(
          CSS.outer,

          // TODO Хз, надо ли это тут. По идее key снаружи должен быть (задаваться до вызова через component.withKey()).
          props.key.whenDefined(^.key := _),

          // Кликабельный заголовок.
          <.div(
            CSS.title,
            if (props.expanded ) {
              CSS.titleExpanded
            } else {
              EmptyVdom
            },

            props.title,
            ^.onClick --> onTitleClick,

            <.a(
              CSS.titleBtn
            )
          ),

          // Содержимое.
          <.div(
            CSS.bodyWrap,
            if (props.expanded) {
              CSS.bodyWrapExpanded
            } else {
              EmptyVdom
            },

            <.div(
              children
            )
          )
        )
      }
    }

  }

  val component = ScalaComponent.builder[Props]("Slide")
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(propsOptProxy: Props)(children: VdomNode*) = component(propsOptProxy)(children: _*)

}
