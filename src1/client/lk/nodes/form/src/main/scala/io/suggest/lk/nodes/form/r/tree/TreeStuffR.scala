package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiLinearProgress, MuiLinearProgressClasses, MuiLinearProgressProps, MuiProgressVariants}
import io.suggest.lk.nodes.form.r.LkNodesFormCss
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.09.2020 17:02
  * Description: Всякие мелкие компоненты для нужд дерева узлов живут здесь.
  */
class TreeStuffR(
                  lkNodesFormCssP      : React.Context[LkNodesFormCss],
                ) {

  val LineProgress = ScalaComponent
    .builder.static( getClass.getSimpleName ) {
      lkNodesFormCssP.consume { lknCss =>
        val progressCss = new MuiLinearProgressClasses {
          override val root = lknCss.Node.linearProgress.htmlClass
        }
        MuiLinearProgress(
          new MuiLinearProgressProps {
            override val variant = MuiProgressVariants.indeterminate
            override val classes = progressCss
          }
        )
      }
    }
    .build




}
