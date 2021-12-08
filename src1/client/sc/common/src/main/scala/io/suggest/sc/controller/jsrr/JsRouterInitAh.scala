package io.suggest.sc.controller.jsrr

import diode._
import diode.Implicits._
import io.suggest.sc.model._
import io.suggest.sc.model.in.MJsRouterS
import io.suggest.sc.view.jsrouter.{GlobalScRouterSafe, JsRouterTag}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.doc.SafeBody
import io.suggest.spa.DiodeUtil.Implicits._
import scala.concurrent.duration._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:53
  * Description: Обработчик событий готовности js-роутера к работе.
  */
class JsRouterInitAh[M <: AnyRef](
                                   modelRW: ModelRW[M, MJsRouterS]
                                 )
  extends ActionHandler( modelRW )
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Action: ensure/initialize js-router.
    case m: JsRouterInit =>
      val v0 = value

      if (m.status.isEmpty && !m.status.isFailed) {
        if (v0.jsRoutes.isReady) {
          // Already done, nothing to do.
          noChange

        } else {
          // Let's initialize js-router:
          val fx = Effect.action {
            val routesStaticTry =
              Try( GlobalScRouterSafe.jsRoutes )
                .flatMap( m => Try(m.get) )

            // If nothing loaded yet, try to find js-router tag in html page.
            // SSR: no html-page may be here, because of missing document.
            // TODO Move this piece of script-tag code into views?
            if (routesStaticTry.isFailure) Try {
              if (JsRouterTag.find().isEmpty) {
                // Inject router tag into DOM. Usually, this is never called.
                val tag = JsRouterTag()
                SafeBody.append(tag)
              }
            }

            JsRouterInit.status.modify( _ withTry routesStaticTry )(m)
          }

          val v2 = MJsRouterS.jsRouter
            .replace( m.status.pending() )(v0)

          updatedSilent( v2, fx )
        }

      } else if (m.status.isFailed || m.status.isUnavailable) {
        // Keep error in pending state, because BootAh monitoring checks isPending() for readyness.
        val v2 = (MJsRouterS.jsRouter replace m.status.pending())(v0)
        val fx = Effect.action {
          JsRouterInit()
        }
          .after( 300.millis )
        updatedSilent(v2, fx)

      } else {
        // Now, jsRouter is ready. Non-silent update, so subscribers can feel model changes ASAP.
        val v2 = (MJsRouterS.jsRouter replace m.status)(v0)
        updated( v2 )
      }

  }

}
