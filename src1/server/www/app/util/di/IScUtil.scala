package util.di

import util.showcase.ShowcaseUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 13:31
 * Description: Интерфейс для доступа к DI-инстансу [[util.showcase.ShowcaseUtil]].
 */
trait IScUtil {

  def scUtil: ShowcaseUtil

}
