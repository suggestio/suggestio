package com.github.react.dnd

import io.suggest.common.empty.OptionUtil
import japgolly.scalajs.react.Ref
import japgolly.scalajs.react.raw.React.RefHandle
import org.scalajs.dom.html

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2019 16:36
  */
object ReactDnd {

  def useDrag(spec: DragSpec): UseDragRes = {
    val useDragResRaw = useDragJs( spec )
    UseDragRes(
      collectedProps    = useDragResRaw(0),
      dragSourceRef     = Ref.fromJs( useDragResRaw(1).asInstanceOf[RefHandle[html.Element]] ),
      dragPreviewRef    = OptionUtil.maybe( useDragResRaw.length > 2 ) {
        Ref.fromJs( useDragResRaw(2).asInstanceOf[RefHandle[html.Element]] )
      },
    )
  }


  def useDrop(spec: DropSpec): UseDropRes = {
    val useDropResRaw = useDropJs( spec )
    UseDropRes(
      collectedProps = useDropResRaw(0),
      dropTargetRef  = Ref.fromJs( useDropResRaw(1).asInstanceOf[RefHandle[html.Element]] ),
    )
  }


  def useDragLayer(spec: DragLayerSpec): js.Object =
    useDragLayerJs( spec )

}


@js.native
trait IDndBackend extends js.Function2[js.Object, js.Object, js.Object] {
  override def apply(manager: js.Object, context: js.Object): js.Object = js.native
}
