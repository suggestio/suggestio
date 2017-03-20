package io.suggest.lk.nodes.form.u

import io.suggest.adn.edit.NodeEditConstants
import io.suggest.common.radio.BeaconUtil
import io.suggest.common.text.StringUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.17 14:49
  * Description: Утиль для react-формы узлов.
  */
object LknFormUtilR {

  def normalizeNodeName(name: String): String = {
    StringUtil.strLimitLen(
      str     = name,
      maxLen  = NodeEditConstants.Name.LEN_MAX,
      ellipsis = ""
    )
  }

  def isNameValid(name: String): Boolean = {
    name.length >= NodeEditConstants.Name.LEN_MIN
  }


  def normalizeBeaconId(id: String): String = {
    StringUtil.strLimitLen(
      str       = id.toLowerCase,
      maxLen    = BeaconUtil.EddyStone.NODE_ID_LEN,
      ellipsis  = ""
    )
  }

  def isBeaconIdValid(id: String): Boolean = {
    id.matches( BeaconUtil.EddyStone.EDDY_STONE_NODE_ID_RE_LC )
  }

}
