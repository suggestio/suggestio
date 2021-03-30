package io.suggest.sys.mdr.u

import io.suggest.jd.MJdDoc
import io.suggest.jd.render.m.MJdRuntime
import io.suggest.jd.render.u.JdUtil
import io.suggest.sys.mdr.SysMdrConst
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.2019 17:05
  * Description:
  */
object SysMdrUtil {

  /** jdConf всегда один: */
  val JD_CONF = SysMdrConst.JD_CONF

  /** Ленивая сборка jdCss на основе шаблонов. */
  def mkJdRuntime(docs: LazyList[MJdDoc],
                  jdRuntimeOpt: Option[MJdRuntime] = None): MJdRuntime = {
    val jdRuntime2 = JdUtil
      .prepareJdRuntime(JD_CONF)
      .docs(docs)
      .prev( jdRuntimeOpt )
      .make

    jdRuntimeOpt
      // Не пересобирать JdCss, если args не изменились.
      .filter { jdRuntime0 =>
        jdRuntime0.jdCss.jdCssArgs.data.jdTagsById ==* jdRuntime2.data.jdTagsById
      }
      .getOrElse( jdRuntime2 )
  }

}
