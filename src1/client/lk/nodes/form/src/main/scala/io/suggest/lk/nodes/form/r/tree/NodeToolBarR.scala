package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiListItem, MuiListItemClasses, MuiListItemProps}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.lk.nodes.MLknNode
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.lk.nodes.form.r.LkNodesFormCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.2020 16:14
  * Description: Строка-тулбар текущего узла.
  */
class NodeToolBarR(
                   diConfig             : NodesDiConf,
                   nodeScLinkR          : NodeScLinkR,
                   goToLkLinkR          : GoToLkLinkR,
                   deleteBtnR           : DeleteBtnR,
                   nameEditButtonR      : NameEditButtonR,
                   lkNodesFormCssP      : React.Context[LkNodesFormCss],
                  ) {

  type Props_t = MLknNode
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    def render(p: Props, s: Props_t): VdomElement = {
      val someFalseProps = p.resetZoom( OptionUtil.SomeBool.someFalse )

      val chs = List[VdomNode](

        // Кнопка редактирования названия узла:
        nameEditButtonR.component( someFalseProps ),

        // Кнопка удаления узла:
        ReactCommonUtil.maybeNode(
          (s.isAdmin contains[Boolean] true)
        ) {
          deleteBtnR.component( someFalseProps )
        },

        // Кнопка перехода в выдачу узла:
        ReactCommonUtil.maybeNode( s.ntype.exists(_.showScLink) )(
          nodeScLinkR.component( s.id )
        ),

        // Кнопка "Перейти..." в ЛК узла:
        ReactCommonUtil.maybeNode(
          diConfig.showLkLinks &&
          s.ntype.exists(_.showGoToLkLink)
        ) {
          goToLkLinkR.component( s.id )
        },

      )

      lkNodesFormCssP.consume { lkNodesFormCss =>
        MuiListItem {
          val css = new MuiListItemClasses {
            override val root = lkNodesFormCss.Node.toolBar.htmlClass
          }
          new MuiListItemProps {
            override val classes = css
          }
        } ( chs: _* )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( FastEqUtil.AnyRefFastEq ) )
    .build

}
