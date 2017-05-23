package util.billing

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.price.dsl.IPriceDslTerm
import io.suggest.bin.BinaryUtil
import io.suggest.mbill2.m.dbg.{MDbgKeys, MDebug, MDebugs}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.util.effect.WT
import io.suggest.pick.PickleUtil
import io.suggest.util.logs.MacroLogsImpl
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
                                mItems           : MItems,
                                val mCommonDi    : ICommonDi
                              )
  extends MacroLogsImpl
{

  import mCommonDi.ec
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


  /** В *Adv*BillUtil.addToOrder() обычно требуется добавить item и алгоритм рассчёта его цены.
    * Тут метод, реализующий эти два дела.
    *
    * @param mitem Item для insert'а, без id обычно.
    * @param priceTerm Терм рассчёта цены.
    * @return DB-экшен, возвращающий сохранённый инстанс MItem.
    */
  def insertItemWithPriceDebug(mitem: MItem, priceTerm: IPriceDslTerm): DBIOAction[MItem, NoStream, WT] = {
    for {
      mItem2      <- mItems.insertOne(mitem)
      dbgCount    <- savePriceDslDebug( mItem2.id.get, priceTerm )
    } yield {
      LOGGER.trace(s"insertItemWithPriceDebug(): Item $mItem2 inserted with $dbgCount debugs")
      mItem2
    }
  }
  /** Кортежная реализация insertItemWithPriceDebug/2, которая обычно и нужна. */
  def insertItemWithPriceDebug(mitemTerm: (MItem, IPriceDslTerm)): DBIOAction[MItem, NoStream, WT] = {
    val (mitem, priceTerm) = mitemTerm
    insertItemWithPriceDebug(mitem, priceTerm)
  }

}
