package util.di

import io.suggest.event.SioNotifierStaticClientI

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 10:37
 * Description: Дефолтовая реализация для DI-поля SioNotifier на время переходного периода.
 */
trait ISioNotifier extends io.suggest.di.ISioNotifier {
   implicit def sn: SioNotifierStaticClientI = util.event.SiowebNotifier.Implicts.sn
 }
