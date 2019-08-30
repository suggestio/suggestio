package io.suggest.jd.edit

import com.softwaremill.macwire._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.2019 10:48
  * Description:
  */
class JdEditModule {

  import io.suggest.jd.render.JdRenderModule._
  import io.suggest.lk.LkCommonModule._

  lazy val jdEditR = wire[JdEditR]

}
