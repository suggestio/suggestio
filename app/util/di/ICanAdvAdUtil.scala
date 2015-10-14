package util.di

import util.acl.CanAdvertiseAdUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 15:49
 * Description: Интерфейс DI-поля для доступа к инстансу утили [[util.acl.CanAdvertiseAdUtil]].
 */
trait ICanAdvAdUtil {

  def canAdvAdUtil: CanAdvertiseAdUtil

}
