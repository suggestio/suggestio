package util

import models._
import play.api.Play.current
import play.api.db.DB

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 12:09
 * Description: Утиль для работы с биллингом, с финансовыми транзакциями.
 */
object Billing {

  /**
   * Обработать платеж внутри sql-транзакции.
   * @param txn Экземпляр ещё не сохранённой платежной транзакции [[models.MBillTxn]].
   * @return Обновлённые и сохранённые данные по финансовой операции.
   */
  def addPayment(txn: MBillTxn): AddPaymentResult = {
    DB.withTransaction { implicit c =>
      val contract = MBillContract.getById(txn.contractId).get
      val balance0 = MBillBalance.getByAdnId(contract.adnId) getOrElse {
        MBillBalance(contract.adnId, 0F).save
      }
      if (txn.currencyCode != balance0.currencyCode)
        throw new UnsupportedOperationException(s"Currency convertion (${txn.currencyCode} -> ${balance0.currencyCode}) not yet implemented.")
      val txnSaved = txn.save
      val newBalance = balance0.updateAmount(txn.amount)
      AddPaymentResult(txnSaved, newBalance, contract)
    }
  }

  case class AddPaymentResult(txnSaved: MBillTxn, newBalance: MBillBalance, contract: MBillContract)
}

