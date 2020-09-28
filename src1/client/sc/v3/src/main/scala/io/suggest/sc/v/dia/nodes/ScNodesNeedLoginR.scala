package io.suggest.sc.v.dia.nodes

import com.materialui.{MuiListItemText, MuiListItemTextProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.ScLoginFormShowHide
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.09.2020 10:21
  * Description: Плашечка о необходимости залогинится внутри LkNodes-формы.
  * Пробрасывается в LkNodes через NodesDiConfig.
  */
class ScNodesNeedLoginR(
                         crCtxP: React.Context[MCommonReactCtx],
                       ) {

  type Props = ModelProxy[_]

  class Backend( $: BackendScope[Props, Unit] ) {

    /** Реакция на клик по форме логина. */
    private val _onLoginRowClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      // Не ясно, надо ли скрывать sc-nodes-диалог? Частый сценарий: юзер открыт логин, глянул форму и скрыл.
      //ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesShowHide(false) ) >>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScLoginFormShowHide(true) )
    }

    def render(children: PropsChildren) = {
      crCtxP.consume { crCtx =>
        MuiListItemText(
          new MuiListItemTextProps {
            override val secondary = crCtx.messages( MsgCodes.`To.control.beacons.need.login` )
            override val onClick = _onLoginRowClick
          }
        )( children )
      }
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

}
