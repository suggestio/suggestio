package util.di

import util.img.WelcomeUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 11:35
 * Description: Интерфейс для DI-поля для доступа к инжектируемому [[util.img.WelcomeUtil]].
 */
trait IWelcomeUtil {

  def welcomeUtil: WelcomeUtil

}
