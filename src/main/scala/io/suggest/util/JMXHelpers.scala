package io.suggest.util

import javax.management.ObjectName
import java.lang.management.ManagementFactory

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 10:35
 * Description: Разные хелперы и утиль для работы с JMX.
 */
object JMXHelpers {

  implicit def string2objectName(name:String):ObjectName = new ObjectName(name)

}
