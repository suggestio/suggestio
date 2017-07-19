package io.suggest.sc.sjs.m.msrv.index

import io.suggest.geo.MLocEnvJs
import io.suggest.sc.sjs.m.msrv.ToJsonWithApiVsnT
import io.suggest.sc.ScConstants.ReqArgs._
import io.suggest.sc.index.MScIndexArgs

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 15:24
 * Description: Client-side версия серверной qs-модели m.sc.ScReqArgs.
 */

object MScIndexArgsJs {

  /** Сериализация в JSON. */
  def toJson(o: MScIndexArgs): Dictionary[Any] = {
    val d = ToJsonWithApiVsnT.setApiVsn()

    for (nodeId <- o.nodeId)
      d(NODE_ID_FN) = nodeId

    val _le = o.locEnv
    if ( _le.nonEmpty )
      d(LOC_ENV_FN) = MLocEnvJs.toJson(_le)

    for (scr <- o.screen)
      d(SCREEN_FN) = scr.toQsValue

    d(WITH_WELCOME_FN) = o.withWelcome

    d
  }

}
