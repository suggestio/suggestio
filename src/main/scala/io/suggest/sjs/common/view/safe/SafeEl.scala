package io.suggest.sjs.common.view.safe

import io.suggest.sjs.common.view.safe.attr.SafeAttrElT
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 10:31
 * Description: Комбинация из нескольких safe-аддонов в одном флаконе.
 */
trait SafeElT
  extends SafeEventTargetT
  with SafeCssElT
  with SafeAttrElT


/** Дефолтовая реализация [[SafeElT]]. */
case class SafeEl[T1 <: Node](
  override val _underlying: T1
)
  extends SafeElT
{
  override type T = T1
}
