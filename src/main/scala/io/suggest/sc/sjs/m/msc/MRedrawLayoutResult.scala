package io.suggest.sc.sjs.m.msc

import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 12:06
 * Description:
 */
case class MRedrawLayoutResult(
  rootDiv   : HTMLDivElement,
  layoutDiv : HTMLDivElement
)
