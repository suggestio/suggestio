package io.suggest.sc.v.inx

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.media.IMediaInfo
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.ScConstants
import io.suggest.sc.index.MWcNameFgH
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.inx.{MScIndex, WcClick}
import io.suggest.sc.v.hdr.NodeNameR
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.spa.OptFastEq.Plain
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, React, ScalaComponent}
import OptionUtil.BoolOptOps
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 16:58
  * Description: wrap-компонент экрана приветствия.
  */
class WelcomeR(
                nodeNameR       : NodeNameR,
                scReactCtxP     : React.Context[MScReactCtx],
              ) {

  type Props_t = MScIndex
  type Props = ModelProxy[Props_t]


  /** @param visibileOptC None - не видим, Some(false) - fading out, Some(true) - видим нормально.
    */
  case class State(
                    visibileOptC              : ReactConnectProxy[Option[Boolean]],
                    bgImgOptC                 : ReactConnectProxy[Option[IMediaInfo]],
                    fgImgOptC                 : ReactConnectProxy[Option[IMediaInfo]],
                    nodeNameFgOptC            : ReactConnectProxy[MWcNameFgH],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onClick: Callback =
      dispatchOnProxyScopeCB( $, WcClick )


    def render(propsProxy: Props, s: State): VdomElement = {
      scReactCtxP.consume { scReactCtx =>
        val AnimCss = ScConstants.Welcome.Anim
        val scCss = scReactCtx.scCss
        val CSS = scCss.Welcome

        val tms = TagMod(
          scCss.bgColor,
          ScCssStatic.Welcome.welcome,
          ^.`class` := AnimCss.WILL_FADEOUT_CSS_CLASS,

          ^.onClick --> _onClick,

          // Рендер фонового изображения.
          s.bgImgOptC { bgImgOptProxy =>
            bgImgOptProxy.value.whenDefinedEl { bgImg =>
              <.img(
                // Подгонка фона wh под экран и центровка происходит в ScCss:
                CSS.Bg.bgImg, scCss.bgColor,
                ^.src := bgImg.url
              )
            }
          },

          // Рендер логотипа или картинки переднего плана.
          s.fgImgOptC { fgImgOptProxy =>
            fgImgOptProxy.value.whenDefinedEl { fgImg =>
              // Есть графический логотип, отрендерить его изображение:
              <.span(
                <.span(
                  CSS.Fg.helper
                ),

                <.img(
                  // Центровка лого происходит в ScCss.
                  CSS.Fg.fgImg,
                  ^.src := fgImg.url
                )
              )
            }
          },

          // Текстовый логотип-подпись, если доступно название узла.
          s.nodeNameFgOptC { wcNameFgHProxy =>
            val wcNameFgH = wcNameFgHProxy.value
            wcNameFgH.nodeName.whenDefinedEl { nodeName =>
              <.div(
                CSS.Fg.fgText,

                // Выровнять по вертикали с учётом картинки переднего плана:
                wcNameFgH.wcFgHeightPx.whenDefined { fgHeightPx =>
                  ^.marginTop := (fgHeightPx / 2).px
                },

                // Отобразить текстовый логотип, такой же как и в заголовке:
                nodeNameR.component(
                  nodeNameR.PropsVal(
                    nodeName = nodeName,
                    styled = true
                  )
                ),
              )
            }
          }
        )

        s.visibileOptC { visibleOptProxy =>
          val visibleOpt = visibleOptProxy.value
          val fadingOut = visibleOpt.getOrElseFalse

          <.div(
            ^.classSet1(
              AnimCss.WILL_FADEOUT_CSS_CLASS,
              AnimCss.TRANS_02_CSS_CLASS     -> fadingOut,
              AnimCss.FADEOUT_CSS_CLASS      -> fadingOut,
            ),

            if (visibleOpt.isEmpty) ^.display.none
            else ^.display.block,

            tms,
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        visibileOptC = propsProxy.connect { props =>
          props.welcome
            .flatMap { wcS =>
              OptionUtil.SomeBool( wcS.isHiding )
            }
        },

        bgImgOptC = propsProxy.connect { props =>
          props.respOpt
            .flatMap(_.welcome)
            .flatMap(_.bgImage)
        },

        fgImgOptC = propsProxy.connect { props =>
          props.respOpt
            .flatMap(_.welcome)
            .flatMap(_.fgImage)
        },

        nodeNameFgOptC = propsProxy.connect { props =>
          props.respOpt
            .fold(MWcNameFgH.empty)(_.wcNameFgH)
        },

      )
    }
    .renderBackend[Backend]
    .build


  def apply(propsValProxy: Props) = component(propsValProxy)

}
