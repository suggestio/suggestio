package io.suggest.sys.mdr

import io.suggest.jd.MJdConf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.18 12:33
  * Description: Константы формы модерации.
  */
object SysMdrConst {

  /** id контейнера react-формы. */
  def FORM_ID = "smf"

  def JD_CONF = MJdConf.simpleMinimal

  /** Max steps right for skip previous nodes. */
  def MAX_OFFSET = 500

}
