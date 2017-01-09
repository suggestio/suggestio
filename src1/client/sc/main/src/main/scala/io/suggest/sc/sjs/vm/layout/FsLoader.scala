package io.suggest.sc.sjs.vm.layout

import io.suggest.sc.ScConstants.Layout.FS_LOADER_ID
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
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


trait FsLoaderT extends VmT with ShowHideDisplayT {
  override type T = HTMLDivElement
}


case class FsLoader(
  override val _underlying: HTMLDivElement
)
  extends FsLoaderT
