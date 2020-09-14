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
      // Накапливаем кнопки тулбара (в обратном порядке):
      var chsAcc = List.empty[VdomNode]

      // TODO Для маячков, которые можно добавлять в свои узлы, нужна кнопка добавления:

      // Кнопка "Перейти..." в ЛК узла:
      if (
        diConfig.showLkLinks() &&
        s.ntype.exists(_.showGoToLkLink)
      ) {
        chsAcc ::= goToLkLinkR.component.withKey(4)( s.id )
      }

      // Кнопка перехода в выдачу узла:
      if (s.ntype.exists(_.showScLink)) {
        chsAcc ::= nodeScLinkR.component.withKey(3)( s.id )
      }

      if (s.isAdmin contains true) {
        val someFalseProps = p.resetZoom( OptionUtil.SomeBool.someFalse )

        // Кнопка удаления узла:
        chsAcc ::= deleteBtnR.component.withKey(2)( someFalseProps )

        // Кнопка редактирования названия узла, доступна для узлов, на которые есть административные права:
        chsAcc ::= nameEditButtonR.component.withKey(1)( someFalseProps )
      }

      // Если есть хотя бы одна кнопка, то рендерим ряд-тулбар:
      ReactCommonUtil.maybeEl( chsAcc.nonEmpty ) {
        lkNodesFormCssP.consume { lkNodesFormCss =>
          MuiListItem {
            val css = new MuiListItemClasses {
              override val root = lkNodesFormCss.Node.toolBar.htmlClass
            }
            new MuiListItemProps {
              override val classes = css
            }
          } ( chsAcc: _* )
        }
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
