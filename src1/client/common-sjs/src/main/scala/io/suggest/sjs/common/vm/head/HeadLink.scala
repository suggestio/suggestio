package io.suggest.sjs.common.vm.head

import io.suggest.sjs.common.vm.attr.HrefAttr
import io.suggest.sjs.common.vm.of.{OfLink, OfNodeHtmlEl}
import org.scalajs.dom.raw.HTMLLinkElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.09.16 14:58
  * Description: vm для доступа к тегу link rel=canonical.
  * Получать инстансы можно с помощью [[HeadVm]].
  */
object HeadLink extends OfNodeHtmlEl with OfLink {

  override type T = HeadLink

  def REL_ATTR = "rel"
  def REL_CANONICAL_VAL = "canonical"

  override type Dom_t = HTMLLinkElement

}

import HeadLink._


/** Класс vm'ки link-rel-canonical. */
case class HeadLink(override val _underlying: Dom_t) extends HrefAttr {

  override type T = Dom_t

  def rel = getAttribute(REL_ATTR)

  def isCanonical = rel.exists(_.equalsIgnoreCase(REL_CANONICAL_VAL))

}
