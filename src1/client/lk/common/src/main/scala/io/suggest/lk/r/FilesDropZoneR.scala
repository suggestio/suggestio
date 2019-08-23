package io.suggest.lk.r

import com.github.react.dnd.{DropTarget, DropTargetF, DropTargetMonitor, DropTargetSpec}
import com.github.react.dnd.backend.html5.{IItemFile, NativeTypes}
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

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.2019 12:01
  * Description: Компонент-обёртка для
  */
class FilesDropZoneR {

  type Props_t = Seq[dom.File] => DAction
  type Props = ModelProxy[Props_t]


  /** Обёртка для дропа файла перетаскиванием. */
  private val dropWrapComponent = ScalaComponent
    .builder[ImgEditBtnDropFiles](getClass.getSimpleName + "In" )
    .stateless
    .render_PC { (props, children) =>
      println( children )
      props.dropConnectF.applyVdomEl(
        <.div(
          ^.className := Css.Overflow.HIDDEN,
          ReactCommonUtil.maybe( props.isOver ) {
            ^.outline := Literal.Typed.dashed.value
          },
          children
          //component( props.proxy )
          // TODO
        )
      )
    }
    .build
  private val dropWrapComponentJs = dropWrapComponent.toJsComponent


  class Backend($: BackendScope[Props, Props_t]) {

    private def _onFilesDropped(files: Seq[dom.File]): Callback = {
      dispatchOnProxyScopeCBf($) { props: Props =>
        props.value( files )
      }
    }

    def render(propsChildren: PropsChildren): VdomElement = {
      val onDropF: js.Function3[js.Object, DropTargetMonitor, js.Any, js.UndefOr[js.Object]] = {
        (props, monitor, comp) =>
          val itemFile = monitor.getItem().asInstanceOf[IItemFile]
          val files = itemFile.files.toSeq
          Future {
            _onFilesDropped( files ).runNow()
          }
          js.undefined
      }
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
          }
        }
      )( dropWrapComponentJs.raw )

      val dropTargetComp = JsComponent[ImgEditBtnDropFiles, Children.None, Null]( dropTargetRawComp )

      dropTargetComp( new ImgEditBtnDropFiles {
        override val isOver = false
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
}
