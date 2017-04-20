package io.suggest.sjs.common.controller

import _router.Controllers

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import io.suggest.js.JsRoutesConst.GLOBAL_NAME

/**
 * play javascript routes.
 * js-роуты генерятся силами play для генерации ссылок на клиенте.
 */
@JSGlobal( GLOBAL_NAME )
@js.native
object routes extends js.Object {

  /** Все экспортированные контроллеры. */
  def controllers: Controllers = js.native

}
