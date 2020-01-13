package io.suggest.n2.node

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.16 15:08
  * Description: Модель исключения, когда узел не найден, но NSEE неуместен.
  */
class NodeNotFoundException(val nodeId: String, val reason: Throwable = null) extends RuntimeException {

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
