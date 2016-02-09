package io.suggest.mbill2.m.txn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.16 19:22
  * Description: Интерфейс для поля с DI-инстансом [[MTxns]].
  */
trait IMTxns {

  def mTxns: MTxns

}
