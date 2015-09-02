package io.suggest.sc.sjs.vm.layout

import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.Layout.FS_LOADER_ID
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.display.ShowHideDisplayEl
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.15 13:31
 * Description: VM'ка для доступа к полноэкранному Loader'у.
 */
object FsLoader extends FindDiv {
  override type T       = FsLoader
  override def DOM_ID   = FS_LOADER_ID
}


trait FsLoaderT extends SafeElT with ShowHideDisplayEl {
  override type T = HTMLDivElement
}


case class FsLoader(
  override val _underlying: HTMLDivElement
)
  extends FsLoaderT
