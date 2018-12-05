package io.suggest.lk.r

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.lk.m.SlideBlockClick
import io.suggest.react.ReactCommonUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCBf
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.17 10:35
  * Description: Реализация slide-block'ов на базе существующей lk-вёрстки с добавлением
  * [[https://codepen.io/adamaoc/pen/wBGGQv]], т.е. нативная анимация и всё такое.
  * DI-инжектируемая утиль для сборки slide-block'а, завязанного на react+diode.
  */
class SlideBlockR(
                  lkCss: LkCss,
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
      dispatchOnProxyScopeCBf($) { props: Props =>
        SlideBlockClick(
          key = props.value
            .flatMap(_.key)
            .get
        )
      }
    }

    def render(propsOptProxy: Props, children: PropsChildren): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        val CSS = lkCss.SlideBlock
        val openedCssTm: TagMod = ReactCommonUtil.maybe(props.expanded) {
          CSS.opened
        }

        <.div(
          CSS.outer,

          // TODO Хз, надо ли это тут. По идее key снаружи должен быть (задаваться до вызова через component.withKey()).
          props.key.whenDefined(^.key := _),

          // Кликабельный заголовок.
          <.div(
            CSS.title,
            openedCssTm,

            props.title,
            ^.onClick --> onTitleClick,

            <.a(
              CSS.titleBtn,
              openedCssTm
            )
          ),

          // Содержимое.
          <.div(
            CSS.bodyWrap,
            ReactCommonUtil.maybe(props.expanded) {
              CSS.bodyWrapExpanded
            },

            <.div(
              children
            )
          )
        )
      }
    }

  }

  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(propsOptProxy: Props)(children: VdomNode*) = component(propsOptProxy)(children: _*)

}
