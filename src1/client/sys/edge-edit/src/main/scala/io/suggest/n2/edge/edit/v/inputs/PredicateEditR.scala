package io.suggest.n2.edge.edit.v.inputs

import com.materialui.{MuiMenuItem, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.{MPredicate, MPredicates}
import io.suggest.n2.edge.edit.m.PredicateChanged
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react.{React, _}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 7:53
  * Description: Компонент редактирования предиката эджа.
  */
class PredicateEditR(
                      crCtxProv: React.Context[MCommonReactCtx],
                    ) {

  type Props_t = MPredicate
  type Props = ModelProxy[Props_t]

  case class State(
                    predicateC      : ReactConnectProxy[MPredicate],
                  )

  class Backend($: BackendScope[Props, State]) {

    private lazy val _onPredicateChangeCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val pred = MPredicates.withValue( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, PredicateChanged(pred) )
    }

    def render(s: State): VdomElement = {
      crCtxProv.consume { crCtx =>
        val _label = crCtx.messages( MsgCodes.`Predicate` )

        // TODO Отрендерить список предикатов:
        val _children = for {
          pred <- MPredicates.values
        } yield {
          MuiMenuItem()(
            HtmlConstants.NBSP_STR * pred.parents.size,
            crCtx.messages( pred.singular ),
          ): VdomElement
        }

        s.predicateC { predicateProxy =>
          MuiTextField(
            new MuiTextFieldProps {
              override val select   = true
              override val label    = _label.rawNode
              override val value    = predicateProxy.value.value
              override val onChange = _onPredicateChangeCbF
            }
          )(
            _children: _*
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        predicateC = propsProxy.connect( identity ),
      )
    }
    .renderBackend[Backend]
    .build

}
