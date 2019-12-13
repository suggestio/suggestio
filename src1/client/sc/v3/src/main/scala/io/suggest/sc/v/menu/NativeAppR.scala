package io.suggest.sc.v.menu

import com.materialui.{MuiListItem, MuiListItemProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.menu.{MMenuNativeApp, MenuAppOpenClose}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 10:09
  * Description: wrap-компонент пункта меню, содержащего данные для скачивания мобильного приложения.
  */
class NativeAppR(
                  commonReactCtx          : React.Context[MCommonReactCtx],
                ) {

  type Props_t = Option[MMenuNativeApp]
  type Props = ModelProxy[Props_t]

  case class State(
                    isVisibleSomeC          : ReactConnectProxy[Some[Boolean]],
                    isOpenedSomeC           : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onOpenCloseClick(e: ReactEvent): Callback = {
      $.props >>= { propsOptProxy: Props =>
        propsOptProxy.value.fold( Callback.empty ) { mnApp =>
          propsOptProxy.dispatchCB( MenuAppOpenClose(mnApp.opened) )
        }
      }
    }
    private lazy val _onOpenCloseClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onOpenCloseClick )


    def render(s: State): VdomElement = {
      lazy val rootItem = {
        // Основной пункт меню.
        val headLine = MuiListItem(
          new MuiListItemProps {
            override val disableGutters = true
            override val button         = true
            override val onClick        = _onOpenCloseClickCbF
          }
        )(
          commonReactCtx.consume { crCtx =>
            crCtx.messages( MsgCodes.`Application` )
          }
        )

        // Если раскрыт пункт, то показать данные для скачивания приложения.
        val content = MuiListItem(
          new MuiListItemProps {
            override val disableGutters = false
            override val button = false
          }
        )(
          // Нужно выдать ссылки для скачивания:
          
        )

        headLine
      }

      s.isVisibleSomeC { isVisibleSomeProxy =>
        ReactCommonUtil.maybeEl( isVisibleSomeProxy.value.value )( rootItem )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsOptProxy =>
      State(

        isVisibleSomeC = propsOptProxy.connect { propsOpt =>
          OptionUtil.SomeBool( propsOpt.nonEmpty )
        },

        isOpenedSomeC = propsOptProxy.connect { propsOpt =>
          propsOpt.fold [Some[Boolean]] ( OptionUtil.SomeBool.someFalse ) { props =>
            OptionUtil.SomeBool( props.opened )
          }
        },

      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
