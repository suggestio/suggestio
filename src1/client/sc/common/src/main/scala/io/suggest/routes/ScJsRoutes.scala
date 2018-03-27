package io.suggest.routes

import io.suggest.sc.ScConstants.JsRouter.NAME

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 15:40
 * Description: Доступ к роутеру запросов к серверу suggest.io.
 */

@js.native
@JSGlobal(NAME)
object ScJsRoutes extends IJsRouter

