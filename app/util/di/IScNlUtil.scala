package util.di

import util.showcase.ShowcaseNodeListUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 12:22
 * Description: Интерфейс для доступа к DI-инстансу [[util.showcase.ShowcaseNodeListUtil]].
 */
trait IScNlUtil {

  def scNlUtil: ShowcaseNodeListUtil

}
