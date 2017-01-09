package io.suggest.di

import io.suggest.event.SioNotifierStaticClientI

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 10:37
 * Description: Интерфейс для DI-поля SioNotifier.
 */
trait ISioNotifier {
  implicit def sn: SioNotifierStaticClientI
}
