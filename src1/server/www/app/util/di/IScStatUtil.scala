package util.di

import util.showcase.ScStatUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 11:50
 * Description: Интерфейс DI-поля для доступа к [[util.showcase.ScStatUtil]].
 */
trait IScStatUtil {

  val scStatUtil: ScStatUtil

}
