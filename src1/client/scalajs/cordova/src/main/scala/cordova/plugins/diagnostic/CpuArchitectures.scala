package cordova.plugins.diagnostic

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 17:09
  */
@js.native
trait CpuArchitectures extends js.Object {

  val UNKNOWN, ARMv6, ARMv7, ARMv8, X86, X86_64: CpuArch_t = js.native

}
