package io.suggest.n2.edge.edit.v

import com.materialui.{MuiFormControl, MuiFormControlProps, MuiFormGroup}
import diode.react.ModelProxy
import io.suggest.css.CssR
import io.suggest.i18n.MCommonReactCtx
import io.suggest.n2.edge.edit.m.MEdgeEditRoot
import io.suggest.n2.edge.edit.v.inputs.{InfoFlagR, NodeIdsR, PredicateR}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 10:43
  * Description: Компонент формы заливки файла.
  */
class EdgeEditFormR(
                     predicateEditR       : PredicateR,
                     nodeIdsR             : NodeIdsR,
                     infoFlagR            : InfoFlagR,
                     crCtxProv            : React.Context[MCommonReactCtx],
                   ) {

  type Props_t = MEdgeEditRoot
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      val css = p.wrap(_ => EdgeEditCss)( CssR.apply )

      crCtxProv.provide( MCommonReactCtx.default )(
        MuiFormControl(
          new MuiFormControlProps {
            override val component = js.defined( <.fieldset.name )
          }
        )(
          css,

          MuiFormGroup()(

            // Предикат:
            p.wrap( _.edge.predicate )( predicateEditR.component.apply ),

            // id узлов:
            p.wrap( _.edit.nodeIds )( nodeIdsR.component.apply ),

            // legacy-флаг эджа:
            p.wrap( _.edge.info.flag )( infoFlagR.component.apply ),

          )

        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
