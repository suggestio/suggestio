package io.suggest.n2.edge.edit.v

import com.materialui.MuiPaper
import diode.react.ModelProxy
import io.suggest.i18n.MCommonReactCtx
import io.suggest.n2.edge.edit.m.MEdgeEditRoot
import io.suggest.n2.edge.edit.v.inputs.PredicateEditR
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 10:43
  * Description: Компонент формы заливки файла.
  */
class EdgeEditFormR(
                     predicateEditR       : PredicateEditR,
                     crCtxProv            : React.Context[MCommonReactCtx],
                   ) {

  type Props_t = MEdgeEditRoot
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      crCtxProv.provide( MCommonReactCtx.default )(
        MuiPaper()(

          // Предикат:
          p.wrap( _.edge.predicate )( predicateEditR.component.apply ),

        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
