package io.suggest.bill.cart.u

import io.suggest.dev.MSzMults
import io.suggest.jd.render.m.MJdRuntime
import io.suggest.jd.render.u.JdUtil
import io.suggest.jd.{MJdConf, MJdDoc}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.18 16:57
  * Description: Cart form - stuff.
  */

object CartUtil {

  /** jd-configuration for ads rendering. */
  val JD_CONF = MJdConf(
    isEdit = false,
    szMult = MSzMults.`0.25`,
    gridColumnsCount = 2
  )

  /** JD-render runtime data - maker function.
    *
    * @param templates jd templates for rendering.
    * @return JD Runtime data.
    */
  def mkJdRuntime(templates: LazyList[MJdDoc] = LazyList.empty): MJdRuntime = {
    JdUtil
      .prepareJdRuntime(JD_CONF)
      .docs(templates)
      .make
  }

}
