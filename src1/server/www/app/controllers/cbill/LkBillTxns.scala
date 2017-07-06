package controllers.cbill

import controllers.SioController
import io.suggest.bill.TxnsListConstants
import io.suggest.init.routed.MJsiTgs
import io.suggest.mbill2.m.txn.{IMTxns, MTxn}
import models.mbill.{MLkTxnsListTplArgs, MLkTxnsPageTplArgs}
import play.twirl.api.Html
import util.acl.IIsNodeAdmin
import views.html.lk.billing.txns._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.16 19:18
  * Description: Аддон для контроллера [[controllers.LkBill2]] для работы со списком транзакций.
  */
trait LkBillTxns
  extends SioController
  with IIsNodeAdmin
  with IMTxns
{

  import mCommonDi._

  private def TXNS_PER_PAGE = 10


  /** Подгрузка страницы из списка транзакций. */
  def txnsList(nodeId: String, page: Int, inline: Boolean) = csrf.AddToken {
    isNodeAdmin(nodeId, U.Lk).async { implicit request =>
      // Наврядли стоит нагружать систему работой с какими-то далёкими страницами.
      if (page > 100)
        throw new IllegalArgumentException(s"txnsList($nodeId, $page, $inline): page number too big.")

      // Получить доступ к списку балансов юзера, который уже должен бы запрашиваться в фоне.
      val balancesFut = request.user.mBalancesFut

      // Построить множество id'шников балансов юзера.
      val balanceIdsFut = for (balances <- balancesFut) yield {
        balances.iterator
          .flatMap(_.id)
          .toSet
      }

      val tpp = TXNS_PER_PAGE
      val offset = page * tpp

      // Прочитать из базы список транзакций по балансам юзера.
      val txnsFut: Future[Seq[MTxn]] = {
        balanceIdsFut.flatMap { balanceIds =>
          slick.db.run {
            mTxns.findLatestTxns(balanceIds, limit = tpp, offset = offset)
          }
        }
      }

      // Построить карту балансов юзера, где id -- это ключ.
      val balancesMapFut = for (balances <- balancesFut) yield {
        val iter = for {
          b         <- balances.iterator
          balanceId <- b.id
        } yield {
          balanceId -> b
        }
        iter.toMap
      }

      // Отрендерить результат юзеру
      for {
        ctxData0      <- request.user.lkCtxDataFut
        txns          <- txnsFut
        balancesMap   <- balancesMapFut
      } yield {

        // Подготовить данные контекста
        implicit val ctxData = ctxData0.withJsiTgs(
          MJsiTgs.BillTxnsList :: ctxData0.jsiTgs
        )

        // Подготовить тело ответа.
        val render: Html = if (inline) {
          // Запрошен рендер только одного куска списка, без всей остальной страницы (js-подгрузка).
          val args = MLkTxnsListTplArgs(txns, balancesMap)
          _TxnsListTpl(args)

        } else {
          // Запрошен рендер всей страницы.
          val args = MLkTxnsPageTplArgs(request.mnode, txns, balancesMap, currPage = page, txnsPerPage = tpp)
          TxnsPageTpl(args)
        }

        // Вернуть http-ответ
        val res = Ok(render)

        // Для работы lk-sjs требуется выставить хидер-подсказку. Он сообщает о (не)возможности запрашивания следующего куска списка транзакций.
        if (inline) {
          res.withHeaders(
            TxnsListConstants.HAS_MORE_TXNS_HTTP_HDR -> (txns.size >= tpp).toString
          )
        } else {
          res
        }
      }
    }
  }

}
