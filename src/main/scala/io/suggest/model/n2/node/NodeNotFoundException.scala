package io.suggest.model.n2.node

import io.suggest.model.n2.N2Exception

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.16 15:08
  * Description: Модель исключения, когда узел не найден, но NSEE неуместен.
  */
class NodeNotFoundException(val nodeId: String, val reason: Throwable = null) extends N2Exception {

  override def getMessage: String = {
    s"Node not found: $nodeId"
  }

  override def getCause: Throwable = {
    if (reason != null) {
      reason
    } else {
      super.getCause
    }
  }

}
