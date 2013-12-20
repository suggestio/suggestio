package util

import play.api.Play.current
import io.suggest.util._
import com.typesafe.config.Config

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 19:04
 * Description: Утиль для переключения между хранилищами (backend'ами моделей).
 * 20.dec.2013: Утиль вынесена в io.suggest.util.SioModelUtil и её смежные классы.
 */

object StorageUtil extends StorageTypeFromConfigT {

  protected def getConfig: Config = current.configuration.underlying
}
