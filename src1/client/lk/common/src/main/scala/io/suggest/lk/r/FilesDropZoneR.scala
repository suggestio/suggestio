package io.suggest.lk.r

import com.github.react.dnd.{DropTarget, DropTargetF, DropTargetMonitor, DropTargetSpec}
import com.github.react.dnd.backend.html5.{IItemFile, NativeTypes}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCBf
import io.suggest.spa.{DAction, FastEqUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import scalacss.internal.Literal
import io.suggest.ueq.UnivEqUtil._

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.2019 12:01
  * Description: Компонент-обёртка для сброса файлов через drag-n-drop.
  * Из-за особенностей legacy-компонентов react-dnd, тут два вложенных компонента с react-dnd-прослойкой:
  * - наружный scala-интерфейс с json props.
  * - react-dnd HOC, обёртывающий нижележащий компонент.
  * - внутренний рендер на основе props наружного интерфейса.
  */
class FilesDropZoneR {

  case class PropsVal(
                       mkActionF      : Seq[dom.File] => DAction,
                       cssClasses     : List[String]                = Nil,
                     )
  implicit object FilesDropZoneRFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.mkActionF eq b.mkActionF) &&
      (a.cssClasses ===* b.cssClasses)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  /** Обёртка для дропа файла перетаскиванием. */
  private val dropWrapComponent = ScalaComponent
    .builder[ImgEditBtnDropFiles]( getClass.getSimpleName + "In" )
    .stateless
    .render_PC { (props, children) =>
      props.dropConnectF.applyVdomEl(
        <.div(
          ^.`class` := Css.flat1( Css.Overflow.HIDDEN :: (props.cssClasses getOrElse Nil) ),
          ReactCommonUtil.maybe( props.isOver ) {
            ^.outline := Literal.Typed.dashed.value
          },
          children
        )
      )
    }
    .build
  private val dropWrapComponentJs = dropWrapComponent.toJsComponent


  class Backend($: BackendScope[Props, Props_t]) {

    private def _onFilesDropped(files: Seq[dom.File]): Callback = {
      dispatchOnProxyScopeCBf($) { props: Props =>
        props.value.mkActionF( files )
      }
    }

    def render(propsProxy: Props, propsChildren: PropsChildren): VdomElement = {
      val onDropF: js.Function3[js.Object, DropTargetMonitor, js.Any, js.UndefOr[js.Object]] = {
        (props, monitor, comp) =>
          val itemFile = monitor.getItem().asInstanceOf[IItemFile]
          val files = itemFile.files.toSeq
          Future {
            _onFilesDropped( files ).runNow()
          }
          js.undefined
      }

      val props = propsProxy.value

      val dropTargetRawComp = DropTarget(
        itemType = NativeTypes.FILE,
        spec = new DropTargetSpec {
          override val drop = onDropF
          // TODO hover
        },
        collect = {(connector, monitor) =>
          new ImgEditBtnDropFiles {
            override val isOver       = monitor.isOver()
            override val dropConnectF = connector.dropTarget()
            override val children     = propsChildren.raw
            override val cssClasses   = props.cssClasses
          }
        }
      )( dropWrapComponentJs.raw )

      val dropTargetComp = JsComponent[ImgEditBtnDropFiles, Children.None, Null]( dropTargetRawComp )

      dropTargetComp( new ImgEditBtnDropFiles {
        override val isOver = false
        override val cssClasses = props.cssClasses
      })
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( FastEqUtil.AnyRefFastEq ) )
    .build

}

/** Для дропа файлов на кнопку используется компонент-обёртка. */
trait ImgEditBtnDropFiles extends js.Object {
  val isOver         : Boolean
  val dropConnectF   : js.UndefOr[DropTargetF] = js.undefined
  val children       : js.UndefOr[raw.PropsChildren] = js.undefined
  val cssClasses     : js.UndefOr[List[String]] = js.undefined
}
