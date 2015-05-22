package io.suggest.sc.sjs.m.mv

import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 16:43
 * Description: Модель результата работы reDrawLayout().
 */
case class ReDrawLayoutResult(
  rootDiv: HTMLDivElement,
  layoutDiv: HTMLDivElement
)
