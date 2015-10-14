package util.di

import util.stat.StatUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 12:09
 * Description: Интерфейс DI-поля для доступа к инжектируемому экземпляру [[util.stat.StatUtil]].
 */
trait IStatUtil {

  def statUtil: StatUtil

}
