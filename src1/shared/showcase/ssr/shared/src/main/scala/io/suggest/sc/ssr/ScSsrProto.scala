package io.suggest.sc.ssr

import boopickle.Default._

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

}


/** Arguments container for renderActionSync() call. */
case class MScSsrArgs(
                       action: IScSsrAction,
                     )

object MScSsrArgs {

  import io.suggest.CommonBooUtil._

  implicit val scSsrRenderArgsP: Pickler[MScSsrArgs] = generatePickler

}
