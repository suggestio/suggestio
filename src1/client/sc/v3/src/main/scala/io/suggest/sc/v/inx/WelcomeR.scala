package io.suggest.sc.v.inx

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.ScConstants
import io.suggest.sc.index.MWelcomeInfo
import io.suggest.sc.m.inx.{MWelcomeState, WcClick}
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.v.hdr.NodeNameR
import io.suggest.spa.OptFastEq.Plain
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 16:58
  * Description: React-компонент экрана приветствия.
  */
class WelcomeR(
                nodeNameR: NodeNameR,
                getScCssF: GetScCssF
              ) {

  /** Props-модель данного компонента. */
  case class PropsVal(
                       wcInfo     : MWelcomeInfo,
                       nodeName   : Option[String],
                       state      : MWelcomeState
                     )

  /** Поддержка FastEq прямо на объекте-компаньоне. */
  implicit object WelcomeRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.wcInfo     ===* b.wcInfo) &&
        (a.nodeName ===* b.nodeName) &&
        (a.state    ===* b.state)
    }
  }

  /** Модель внутреннего состояния компонента приветствия.
    *
    * @param nodeNameC Коннекшен до названия узла.
    */
  protected[this] case class State(
                                    nodeNameC: ReactConnectProxy[Option[String]]
                                  )

  type Props = ModelProxy[Option[PropsVal]]


  class Backend( $: BackendScope[Props, State] ) {

    private def _onClick: Callback = {
      dispatchOnProxyScopeCB( $, WcClick )
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      propsProxy().whenDefinedEl { p =>
        val AnimCss = ScConstants.Welcome.Anim
        val fadingOutNow = p.state.isHiding

        val scCss = getScCssF()
        val CSS = scCss.Welcome

        <.div(
          CSS.welcome,
          ^.classSet1(
            AnimCss.WILL_FADEOUT_CSS_CLASS,
            AnimCss.TRANS_02_CSS_CLASS     -> fadingOutNow,
            AnimCss.FADEOUT_CSS_CLASS      -> fadingOutNow
          ),

          ^.onClick --> _onClick,

          // Рендер фонового изображения.
          p.wcInfo.bgImage.whenDefined { bgImg =>
            <.img(
              // Подгонка фона wh под экран и центровка происходит в ScCss:
              CSS.Bg.bgImg,
              ^.src := bgImg.url
            )
          },

          // Рендер логотипа или картинки переднего плана.
          p.wcInfo.fgImage.whenDefined { fgImg =>
            // Есть графический логотип, отрендерить его изображение:
            TagMod(
              <.span(
                CSS.Fg.helper
              ),

              <.img(
                // Центровка лого происходит в ScCss.
                CSS.Fg.fgImg,
                ^.src := fgImg.url
              )
            )
          },

          // Текстовый логотип-подпись, если доступно название узла.
          p.nodeName.whenDefined { _ =>
            <.div(
              CSS.Fg.fgText,
              // Выровнять по вертикали с учётом картинки переднего плана:
              p.wcInfo
                .fgImage
                .flatMap(_.whPx)
                .whenDefined { whPx =>
                  ^.marginTop := (whPx.height / 2).px
                },
              // Отобразить текстовый логотип, такой же как и в заголовке:
              s.nodeNameC { nodeNameR.apply }
            )
          }

        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("Welcome")
    .initialStateFromProps { propsProxy =>
      State(
        nodeNameC = propsProxy.connect(_.flatMap(_.nodeName))
      )
    }
    .renderBackend[Backend]
    .build


  def apply(propsValProxy: Props) = component(propsValProxy)

}