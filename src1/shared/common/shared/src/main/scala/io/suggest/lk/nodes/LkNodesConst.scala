package io.suggest.lk.nodes

import io.suggest.adn.edit.NodeEditConstants
import io.suggest.ble.BeaconUtil
import io.suggest.text.StringUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 17:26
  * Description: Константы формы управления узлами в личном кабинете.
  */
object LkNodesConst {

  def PREFIX = "lkn"

  /** id контейнера reat-формы.
    * Сюда будет отрендерена форма на клиенте. */
  def FORM_CONT_ID = PREFIX + "fc"


  def normalizeNodeName(name: String): String = {
    StringUtil.strLimitLen(
      str     = name,
      maxLen  = NodeEditConstants.Name.LEN_MAX,
      ellipsis = ""
    )
  }


  def isNameValid(name: String): Boolean =
    name.length >= NodeEditConstants.Name.LEN_MIN


  def normalizeBeaconId(id: String): String = {
    StringUtil.strLimitLen(
      str       = id.toLowerCase,
      maxLen    = BeaconUtil.EddyStone.NODE_ID_LEN,
      ellipsis  = ""
    )
  }


  def MAX_BEACONS_INFO_PER_REQ = 50

}
