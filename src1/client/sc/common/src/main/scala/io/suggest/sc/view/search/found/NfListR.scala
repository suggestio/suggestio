package io.suggest.sc.view.search.found

import com.materialui.{MuiGrid, MuiGridClasses, MuiGridProps}
import io.suggest.css.Css
import io.suggest.log.Log
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.view.styl.ScCssStatic
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.07.18 18:28
  * Description: wrap-компонент, отвечающий за рендер списка найденных узлов: тегов и гео-размещенных узлов.
  */
final class NfListR
  extends Log
{

  type Props = PropsVal


  /** Пропертисы, содержащие ModelProxy[] внутри себя.
    * Это wrap-компонент, поэтому пропертисы содержат статичные поля, обрабатываемые лишь единожды.
    *
    * @param onTouchStartF Опциональная реакция на touchstart, определяемая на верхнем уровне.
    */
  case class PropsVal(
                       isScrollable     : Boolean = false,
                       onTouchStartF    : Option[ReactUIEventFromHtml => Callback]      = None,
                     )


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props, children: PropsChildren): VdomElement = {
      val NodesCSS = ScCssStatic.Search.NodesFound

      MuiGrid {
        val listClass =
          if (p.isScrollable) NodesCSS.nodesListScrollable
          else ScCssStatic.Body.ovh

        val listClasses = new MuiGridClasses {
          override val root = Css.flat( NodesCSS.listDiv.htmlClass, listClass.htmlClass )
        }

        val onTouchMoveUndF: js.UndefOr[js.Function1[ReactTouchEventFromInput, Unit]] =
          p.onTouchStartF
            .map { f =>
              ReactCommonUtil.cbFun1ToJsCb { e: ReactTouchEventFromInput => f(e) }
            }
            .toUndef

        new MuiGridProps {
          override val container = true
          override val classes = listClasses
          override val onTouchMove = onTouchMoveUndF
        }
      } (
        // Рендер нормального списка найденных узлов.
        children,
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

}
