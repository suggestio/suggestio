package io.suggest.sc.ssr

import boopickle.Default._
import io.suggest.dev.MScreen

/** SSR-render showcase arguments container.
  * This model is compatible with Boopickle, but no boopickle currently in /shared/common.
  * All picklers for showcase SSR-needs must be defined here.
  */
object ScSsrProto {

  /** API call-names container. */
  object Manifest {
    /** Primary sync.rendering JS-function name after processing a single action. */
    final val RenderActionSync = "renderActionSync"
  }

  /** Screen sizes, used for server-side rendering. */
  def defaultScreen: MScreen = {
    // Width is minimally-standard for expected desktops, not too big.
    // TODO By now, screen height should be bigger than expected screen, so styled grid-container will occupy
    //      at least 100% screen from top to bottom. Possibly, in future this should be resolved in CSS-level.
    MScreen.defaulted(
      height = 2000,
    )
  }

}


/** Arguments container for renderActionSync() call. */
case class MScSsrArgs(
                       action: IScSsrAction,
                     )

object MScSsrArgs {

  import io.suggest.CommonBooUtil._

  implicit val scSsrRenderArgsP: Pickler[MScSsrArgs] = generatePickler

}
