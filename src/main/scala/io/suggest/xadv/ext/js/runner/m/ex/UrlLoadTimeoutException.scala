package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 16:09
 * Description: Если возникает таймаут загрузки ссылки, то необходимо причесать его к ошибке.
 */
case class UrlLoadTimeoutException(url: String, timeoutMs: Int) extends RuntimeException with MErrorInfoT {
  override def msg = "e.url.load.failed"
  override def args = Seq(url, timeoutMs.toString)
  override def info = None
}
