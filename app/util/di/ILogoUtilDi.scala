package util.di

import util.img.LogoUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.15 14:59
  * Description: Интерфейс для DI-поля с инстансом утили для логотипов.
  */
trait ILogoUtilDi {

  def logoUtil: LogoUtil

}
