package io.suggest.sc.v.inx

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.media.MMediaInfo
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.ScConstants
import io.suggest.sc.index.MWcNameFgH
import io.suggest.sc.m.inx.{MScIndex, WcClick}
import io.suggest.sc.v.hdr.NodeNameR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
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
                scCssP          : React.Context[ScCss],
              ) {

  type Props_t = MScIndex
  type Props = ModelProxy[Props_t]


  /** @param visibileOptC None - не видим, Some(false) - fading out, Some(true) - видим нормально.
    */
  case class State(
                    visibileOptC              : ReactConnectProxy[Option[Boolean]],
                    bgImgOptC                 : ReactConnectProxy[Option[MMediaInfo]],
                    fgImgOptC                 : ReactConnectProxy[Option[MMediaInfo]],
                    nodeNameFgOptC            : ReactConnectProxy[MWcNameFgH],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onClick: Callback =
      dispatchOnProxyScopeCB( $, WcClick )


    def render(propsProxy: Props, s: State): VdomElement = {
      // Необходимо, чтобы все коннекшены жили за пределами scCss context.consume():
      // Обновление контекста в момент node switch может вызывать ненужный пере-монтирование всего welcome-поддерева,
      // из-за сброса контекста при index-switch или повороте/изменении экрана.
      val AnimCss = ScConstants.Welcome.Anim

      // Рендер фонового изображения.
      val bgImg = s.bgImgOptC { bgImgOptProxy =>
        bgImgOptProxy.value.whenDefinedEl { bgImg =>
          val bgImgSrc =
            ^.src := bgImg.url
          scCssP.consume { scCss =>
            <.img(
              // Подгонка фона wh под экран и центровка происходит в ScCss:
              scCss.Welcome.Bg.bgImg,
              scCss.bgColor,
              bgImgSrc,
            )
          }
        }
      }

      // Рендер логотипа или картинки переднего плана.
      val fgImg = s.fgImgOptC { fgImgOptProxy =>
        fgImgOptProxy.value.whenDefinedEl { fgImg =>
          val fgImgSrc =
            ^.src := fgImg.url

          // Есть графический логотип, отрендерить его изображение:
          scCssP.consume { scCss =>
            <.span(
              <.span(
                scCss.Welcome.Fg.helper
              ),

              <.img(
                // Центровка лого происходит в ScCss.
                scCss.Welcome.Fg.fgImg,
                fgImgSrc,
              )
            )
          }
        }
      }

      // Текстовый логотип-подпись, если доступно название узла.
      val nodeName = s.nodeNameFgOptC { wcNameFgHProxy =>
        wcNameFgHProxy
          .value
          .nodeName
          .whenDefinedEl { nodeName =>
            // Отобразить текстовый логотип, такой же как и в заголовке:
            val nodeNameInner = nodeNameR.component(
              nodeNameR.PropsVal(
                nodeName = nodeName,
                styled = true,
              )
            ): TagMod
            scCssP.consume { scCss =>
              <.div(
                scCss.Welcome.Fg.fgText,
                nodeNameInner,
              )
            }
          }
      }

      val div0 = <.div(
        ScCssStatic.Welcome.welcome,
        ^.`class` := AnimCss.WILL_FADEOUT_CSS_CLASS,

        ^.onClick --> _onClick,
        bgImg,
        fgImg,
        nodeName,
      )

      s.visibileOptC { visibleOptProxy =>
        val visibleOpt = visibleOptProxy.value
        val fadingOut = visibleOpt.getOrElseFalse

        val tagMods = TagMod(
          ^.classSet(
            AnimCss.TRANS_02_CSS_CLASS     -> fadingOut,
            AnimCss.FADEOUT_CSS_CLASS      -> fadingOut,
          ),

          if (visibleOpt.isEmpty) ^.display.none
          else ^.display.block,
        )

        scCssP.consume { scCss =>
          div0(
            scCss.bgColor,
            tagMods,
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
