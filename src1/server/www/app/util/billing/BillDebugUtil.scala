package util.billing

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.price.dsl.IPriceDslTerm
import io.suggest.bin.BinaryUtil
import io.suggest.mbill2.m.dbg.{MDbgKeys, MDebug, MDebugs}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.pick.PickleUtil
import models.mproj.ICommonDi

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.17 14:46
  * Description: Утиль для взаимодействия с отладкой биллинга.
  * Исходный Bill2Util сильно растолстел, поэтому debug-утиль будет размножаться тут.
  */
@Singleton
class BillDebugUtil @Inject() (
                                mDebugs          : MDebugs,
                                val mCommonDi    : ICommonDi
                              ) {

  import mCommonDi.slick.profile.api._


  /** Сохранение отладки с инстансом priceDsl.
    *
    * @param objectId id элемента биллинга.
    * @param priceTerm Терм priceDsl.
    * @return DB-экшен, возвращающий кол-во добавленных рядов.
    */
  def savePriceDslDebug(objectId: Gid_t, priceTerm: IPriceDslTerm): DBIOAction[Int, NoStream, Effect.Write] = {
    val key = MDbgKeys.PriceDsl
    val mdbg0 = MDebug(
      objectId = objectId,
      key      = key,
      vsn      = key.V_CURRENT,
      data     = BinaryUtil.byteBufToByteArray(
        PickleUtil.pickle(priceTerm)
      )
    )
    mDebugs.insertOne( mdbg0 )
  }

}
