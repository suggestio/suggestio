package util.adv.direct

import com.google.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.cal.CalendarUtil
import util.n2u.N2NodesUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 22:27
  * Description: Утиль для биллинга прямых размещений.
  * Этот код пришел на смену хорошо-отлаженному MmpDailyBilling на базе биллинга v1.
  */
@Singleton
class AdvDirectBilling @Inject() (
  n2NodesUtil             : N2NodesUtil,
  calendarUtil            : CalendarUtil,
  mCommonDi               : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._

  // TODO Портануть код из MmpDailyBilling
  ???

}
