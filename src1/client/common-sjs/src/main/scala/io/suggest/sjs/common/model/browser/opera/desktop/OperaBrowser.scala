package io.suggest.sjs.common.model.browser.opera.desktop

import io.suggest.sjs.common.model.browser.{IBrowserVsn, IBrowser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 12:03
 * Description: Проприетарная дырка от браузера первого и второго поколения, после того как Opera ASA переехала
 * с Presto на WebKit и сразу на Blink.
 */
case class OperaBrowser(
  override val vsnMajor: Int,
  override val vsnMinor: Int
) extends IBrowser with OperaCssPrefixing {

  /** Строковое название браузера: firefox, chrome, opera, etc. */
  override def name = "opera"
  override def toString = super.toString
}


object OperaBrowser {

  def apply(vsn: IBrowserVsn): OperaBrowser = {
    OperaBrowser(vsn.vsnMajor, vsnMinor = vsn.vsnMinor)
  }

}
