package controllers

import com.google.inject.Inject
import controllers.sysctl.bill.SbNode
import io.suggest.mbill2.m.balance.MBalances
import io.suggest.mbill2.m.contract.MContracts
import models.MCalendars
import models.mproj.ICommonDi
import util.billing.{Bill2Util, TfDailyUtil}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 23:01
 * Description: Контроллер sys-биллинга второго поколения.
 * Второй биллинг имеет тарифы внутри узлов и контракты-ордеры-item'ы в РСУБД.
 */
class SysBilling @Inject() (
  override val tfDailyUtil          : TfDailyUtil,
  override val bill2Util            : Bill2Util,
  override val mCalendars           : MCalendars,
  override val mContracts           : MContracts,
  override val mBalances            : MBalances,
  override val mCommonDi            : ICommonDi
)
  extends SioControllerImpl
  with SbNode
