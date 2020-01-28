package io.suggest.n2.edge.edit.v.inputs.act

import com.materialui.{Mui, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogContentText, MuiDialogProps, MuiDialogTitle, MuiFab, MuiFabProps, MuiFabVariants, MuiList, MuiListItem, MuiListItemIcon, MuiListItemText}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MMessage, MsgCodes}
import io.suggest.lk.m.{ErrorPopupCloseClick, MErrorPopupS}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.01.2020 16:02
  * Description: Диалог рендера ошибки.
  */
class ErrorDiaR(
                 crCtxProv: React.Context[MCommonReactCtx],
               ) {

  type Props_t = Option[MErrorPopupS]
  type Props = ModelProxy[Props_t]


  case class State(
                    isShownSomeC        : ReactConnectProxy[Some[Boolean]],
                    errorMsgsC          : ReactConnectProxy[List[MMessage]],
                    exceptionOptC       : ReactConnectProxy[Option[Throwable]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private lazy val _onCloseClick = ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ErrorPopupCloseClick )
    }

    def render(s: State): VdomElement = {
      val children = crCtxProv.consume { crCtx =>
        lazy val errorIcon = MuiListItemIcon()(
          Mui.SvgIcons.Error()()
        )

        React.Fragment(

          MuiDialogTitle()(
            crCtx.messages( MsgCodes.`Something.gone.wrong` ),
          ),

          MuiDialogContent()(
            MuiDialogContentText()(
              s.errorMsgsC { errorMsgsProxy =>
                MuiList()(
                  errorMsgsProxy
                    .value
                    .toVdomArray { errorMsg =>
                      MuiListItem()(
                        errorIcon,
                        MuiListItemText()(
                          crCtx.messages( errorMsg ),
                        ),
                      )
                    }
                )
              },

              <.br,
              <.br,

              s.exceptionOptC { exceptionOptProxy =>
                exceptionOptProxy.value.whenDefinedEl { ex =>
                  <.pre(
                    ex.toString,
                  )
                }
              },

            ),
          ),

          MuiDialogActions()(
            MuiFab(
              new MuiFabProps {
                override val onClick = _onCloseClick
                override val variant = MuiFabVariants.extended
              }
            )(
              Mui.SvgIcons.Close()(),
              crCtx.messages( MsgCodes.`Close` ),
            ),
          ),

        )
      }

      s.isShownSomeC { isShownSomeProxy =>
        MuiDialog(
          new MuiDialogProps {
            override val open = isShownSomeProxy.value.value
          }
        )( children )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isShownSomeC = propsProxy.connect { p =>
          OptionUtil.SomeBool( p.nonEmpty )
        },
        errorMsgsC = propsProxy.connect { p =>
          p.fold(List.empty[MMessage])(_.messages)
        },
        exceptionOptC = propsProxy.connect { p =>
          p.flatMap(_.exception)
        },
      )
    }
    .renderBackend[Backend]
    .build

}
