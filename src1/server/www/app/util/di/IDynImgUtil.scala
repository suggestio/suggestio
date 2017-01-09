package util.di

import util.img.DynImgUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 12:34
 * Description: Интерфейс для доступа к DI-полю с утилью для DynImg.
 */
trait IDynImgUtil {
  def dynImgUtil: DynImgUtil
}
