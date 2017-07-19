package io.suggest.sc.inx.v.wc

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.dev.MScreen
import io.suggest.sc.index.MWelcomeInfo
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.ScConstants
import io.suggest.sc.hdr.v.NodeNameR
import io.suggest.sc.inx.m.{MWelcomeShowingState, MWelcomeStates}
import io.suggest.sc.styl.ScCss.scCss
import io.suggest.sjs.common.spa.OptFastEq.Plain

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 16:58
  * Description: React-компонент экрана приветствия.
  */
object WelcomeR {

  /** Props-модель данного компонента. */
  case class PropsVal(
                       wcInfo     : MWelcomeInfo,
                       screen     : MScreen,
                       nodeName   : Option[String],
                       state      : MWelcomeShowingState
                     )

  /** Модель внутреннего состояния компонента приветствия.
    *
    * @param nodeNameC Коннекшен до названия узла.
    */
  protected[this] case class State(
                                    nodeNameC: ReactConnectProxy[Option[String]]
                                  )

  /** Поддержка FastEq прямо на объекте-компаньоне. */
  implicit object PropsVal extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.wcInfo eq b.wcInfo) &&
        (a.screen eq b.screen) &&
        (a.nodeName eq b.nodeName) &&
        (a.state eq b.state)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  class Backend( $: BackendScope[Props, State] ) {

    private def _whMarginMod(wh: ISize2di, margin: ISize2di): TagMod = {
      TagMod(
        ^.height      := wh.height.px,
        ^.width       := wh.width.px,
        ^.marginTop   := margin.height.px,
        ^.marginLeft  := margin.width.px
      )
    }

    def render(propsProxy: Props, s: State): VdomElement = {
      propsProxy().whenDefinedEl { p =>
        val AnimCss = ScConstants.Welcome.Anim
        val fadingOutNow = p.state == MWelcomeStates.HidingNow

        <.div(
          scCss.Welcome.welcome,
          ^.classSet1(
            AnimCss.WILL_FADEOUT_CSS_CLASS,
            AnimCss.TRANS_02_CSS_CLASS     -> fadingOutNow,
            AnimCss.FADEOUT_CSS_CLASS      -> fadingOutNow
          ),

          // TODO onClick

          // Рендер фонового изображения.
          p.wcInfo.bgImage.whenDefined { bgImg =>
            <.img(
              scCss.Welcome.Bg.bgImg,
              ^.src := bgImg.url,

              // Рендер параметров изображения: подгонка фона wh под экран и центровка.
              bgImg.whPx.whenDefined { wh0 =>
                val wh2 = if (wh0.whRatio < p.screen.whRatio) {
                  val w = p.screen.width
                  MSize2di(
                    width  = w,
                    height = w * wh0.height / wh0.width
                  )
                } else {
                  val h = p.screen.height
                  MSize2di(
                    width  = h * wh0.width / wh0.height,
                    height = h
                  )
                }
                val margin2 = wh2 / (-2)
                _whMarginMod( wh2, margin2 )
              }
            )
          },

          // Рендер логотипа или картинки переднего плана.
          p.wcInfo.fgImage.whenDefined { fgImg =>
            // Есть графический логотип, отрендерить его изображение:
            TagMod(
              <.span(
                scCss.Welcome.Fg.helper
              ),

              <.img(
                scCss.Welcome.Fg.fgImg,
                ^.src := fgImg.url,

                // Центровка логотипа под экран
                fgImg.whPx.whenDefined { wh0 =>
                  val wh2 = wh0 / 2
                  val margin0 = wh2 / (-2)
                  val margin2 = margin0.withHeight( margin0.height + 25 )
                  _whMarginMod( wh2, margin2 )
                }
              )
            )
          },

          // Текстовый логотип-подпись.
          // TODO XXX marginTop = (fgImgSz.height / 2).px
          s.nodeNameC { NodeNameR.apply }

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
