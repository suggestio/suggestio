package controllers

import com.google.inject.Inject
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.adn.LkAdnMapFormUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:22
  * Description: Контроллер личного кабинета для связывания узла с точкой/местом на карте.
  * На карте в точках размещаются узлы ADN, и это делается за денежки.
  */
class LkAdnMap @Inject() (
  lkAdnMapFormUtil              : LkAdnMapFormUtil,
  override val mCommonDi        : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
{

  import LOGGER._


  def forNode(nodeId: String) = ???

}
