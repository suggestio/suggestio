package controllers

import com.google.inject.Inject
import controllers.sysctl.bill._
import io.suggest.mbill2.m.balance.MBalances
import io.suggest.mbill2.m.contract.MContracts
import io.suggest.mbill2.m.gid.GidUtil
import io.suggest.mbill2.m.txn.MTxns
import models.mcal.MCalendars
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.billing.{Bill2Util, ContractUtil, TfDailyUtil}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 23:01
 * Description: Контроллер sys-биллинга второго поколения.
 * Второй биллинг имеет тарифы внутри узлов и контракты-ордеры-item'ы в РСУБД.
 */
class SysBilling @Inject() (
  override val tfDailyUtil          : TfDailyUtil,
  override val mCalendars           : MCalendars,
  override val mContracts           : MContracts,
  override val mBalances            : MBalances,
  override val mTxns                : MTxns,
  override val contractUtil         : ContractUtil,
  override val bill2Util            : Bill2Util,
  override val gidUtil              : GidUtil,
  override val mCommonDi            : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with SbNode
  with SbNodeTfDaily
  with SbNodeContract
  with SbPayment
  with SbOverview
