package io.suggest.sjs.common.view.safe.nav

import io.suggest.sjs.common.view.safe.nav.ua.SafeNavUa
import org.scalajs.dom
import org.scalajs.dom.Navigator

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:22
 * Description: Враппер для dom.navigator для безопасного доступа к некоторым полям.
 */
case class SafeNavigator(
  override val _underlying: Navigator = dom.navigator
)
  extends SafeNavUa
{
  override type T = Navigator
}
