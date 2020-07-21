package io.suggest.sc.v.hdr

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.ScConstants
import io.suggest.sc.m.{MScReactCtx, MScRoot}
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 17:49
  * Description: Компонент заголовка выдачи.
  */

class HeaderR(
               val logoR                : LogoR,
               val hdrProgressR         : HdrProgressR,
               val goBackR              : GoBackR,
               menuBtnR                 : MenuBtnR,
               searchBtnR               : SearchBtnR,
               scReactCtxP              : React.Context[MScReactCtx],
             ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    isColoredSomeC            : ReactConnectProxy[Some[Boolean]],
                    hdrLogoOptC               : ReactConnectProxy[logoR.Props_t],
                    hdrOnGridBtnColorOptC     : ReactConnectProxy[Option[MColorData]],
                    hdrProgressC              : ReactConnectProxy[hdrProgressR.Props_t],
                    goBackC                   : ReactConnectProxy[goBackR.Props_t],
                  )


  /** Рендерер. */
  protected class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      val hdrLogo       = s.hdrLogoOptC { logoR.apply }
      val hdrMenuBtn    = s.hdrOnGridBtnColorOptC { menuBtnR.apply }
      val hdrSearchBtn  = s.hdrOnGridBtnColorOptC { searchBtnR.apply }
      val hdrProgress   = s.hdrProgressC { hdrProgressR.apply }
      val hdrGoBack     = s.goBackC { goBackR.apply }

      scReactCtxP.consume { scReactCtx =>
        val scCss = scReactCtx.scCss
        val tm0 = <.div(
          ScCssStatic.Header.header,
          scCss.Header.header,
          // -- Кнопки заголовка в зависимости от состояния выдачи --
          // Кнопки при нахождении в обычной выдаче без посторонних вещей:

          // Слева:
          hdrGoBack,
          hdrMenuBtn,

          // По центру:
          hdrLogo,

          // Справа:
          hdrSearchBtn,
          hdrProgress,
        )
        s.isColoredSomeC { isColoredSomeProxy =>
          tm0(
            ReactCommonUtil.maybe( isColoredSomeProxy.value.value ) {
              TagMod(
                scCss.fgColorBorder,
                scCss.bgColor,
                ScCssStatic.Header.border,
              )
            },
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isColoredSomeC = propsProxy.connect { props =>
          val ic = props.index.scCss.Header.isColored
          OptionUtil.SomeBool( ic )
        },

        hdrLogoOptC = propsProxy.connect { mroot =>
          for {
            mnode <- mroot.index.resp.toOption
          } yield {
            logoR.PropsVal(
              logoOpt     = mnode.logoOpt,
              nodeNameOpt = mnode.name,
              styled      = true,
            )
          }
        }( OptFastEq.Wrapped(logoR.LogoPropsValFastEq) ),

        hdrOnGridBtnColorOptC = propsProxy.connect { mroot =>
          OptionUtil.maybe( !mroot.index.isAnyPanelOpened ) {
            mroot.index.resp
              .toOption
              .flatMap( _.colors.fg )
              .getOrElse( MColorData(ScConstants.Defaults.FG_COLOR) )
          }
        }( OptFastEq.Plain ),

        hdrProgressC = propsProxy.connect { mroot =>
          val r =
            mroot.index.resp.isPending ||
            mroot.grid.core.adsHasPending
          Some(r)
        }( OptFastEq.OptValueEq ),

        goBackC = propsProxy.connect { mroot =>
          OptionUtil.maybeOpt( !mroot.index.isAnyPanelOpened ) {
            mroot.index.state.prevNodeOpt
          }
        }( OptFastEq.Plain ),

      )
    }
    .renderBackend[Backend]
    .build

}
