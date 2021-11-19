package io.suggest.sc.ssr

import boopickle.Default._

/** SSR-render showcase arguments container.
  * This model is compatible with Boopickle, but no boopickle currently in /shared/common.
  * All picklers for showcase SSR-needs must be defined here.
  */
object ScSsrUtil {

  /** API call-names container. */
  object Manifest {
    /** Primary sync.rendering JS-function name after processing a single action. */
    final val RenderActionSync = "renderActionSync"
  }

}


/** Arguments container for renderUniApiResp() call. */
case class MScSsrRenderArgs(
                             action: IScSsrAction,
                           )

object MScSsrRenderArgs {

  import io.suggest.CommonBooUtil._

  implicit val scSsrActionP: Pickler[IScSsrAction] = generatePickler
  implicit val scSsrRenderArgsP: Pickler[MScSsrRenderArgs] = generatePickler

}
