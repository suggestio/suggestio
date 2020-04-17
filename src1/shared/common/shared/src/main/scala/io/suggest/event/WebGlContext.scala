package io.suggest.event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:24
  * Description: WebGL context constant event names.
  */
trait WebGlContext {

  def WEBGL_CONTEXT_LOSS      = "webglcontextlost"
  def WEBGL_CONTEXT_RESTORED  = "webglcontextrestored"

}
