package io.suggest.bill

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 11:38
 * Description: Константы страницы списка транзакций.
 */
object TxnsListConstants {

  /** id кнопки, которая должна подгружать ещё транзакции. */
  def GET_MORE_TXNS_BTN_ID  = "getTransactions"

  /** ID контейнера списка транзакций. */
  def TXNS_CONTAINER_ID     = "transactionsHistory"

  /** Название http-заголовка, который содержит подсказку о наличии */
  def HAS_MORE_TXNS_HTTP_HDR = "X-Has-More-Txns"

  /** id узла рекламной сети. Передается в экшены js router'а. */
  def ADN_ID_INPUT_ID = "adnId"

  /** Номер последней загруженной страницы. */
  def CURR_PAGE_NUM_INPUT_ID = "pageNumber"

}
