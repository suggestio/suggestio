package controllers.sysctl.bill

import controllers.routes
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.balance.{MBalance, IMBalances}
import io.suggest.mbill2.m.contract.IMContracts
import models.req.INodeReq
import models.{MDailyTf, IMCalendars}
import models.msys.bill.{MTfDailyEditTplArgs, MForNodeTplArgs}
import play.api.data.Form
import play.api.mvc.Result
import util.{PlayMacroLogsImpl, PlayMacroLogsI}
import util.acl.IsSuNode
import util.billing.{IBill2UtilDi, ITfDailyUtilDi}
import views.html.sys1.bill._
import views.html.sys1.bill.daily._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 14:05
 * Description: Трейт с экшенами sys-биллинга узла второго поколения.
 */
trait SbNode
  extends IsSuNode
  with ITfDailyUtilDi
  with IBill2UtilDi
  with IMCalendars
  with PlayMacroLogsI
  with PlayMacroLogsImpl
  with IMContracts
  with IMBalances
{

  import mCommonDi._

  /**
   * Отображение страницы биллинга для узла.
   * @param nodeId id просматриваемого узла.
   */
  def forNode(nodeId: String) = IsSuNode(nodeId).async { implicit request =>
    val contractIdOpt = request.mnode.billing.contractId

    val mContractOptFut = FutureUtil.optFut2futOpt(contractIdOpt) { contractId =>
      val mContractOptAction = mContracts.getById(contractId)
      dbConfig.db.run(mContractOptAction)
    }

    val mBalancesFut = contractIdOpt.fold[Future[Seq[MBalance]]] {
      Future.successful(Nil)
    } { contractId =>
      val mBalancesAction = mBalances.findByContractId(contractId)
      dbConfig.db.run(mBalancesAction)
    }

    for {
      mContractOpt <- mContractOptFut
      mBalances    <- mBalancesFut
    } yield {
      // Поискать контракт, собрать аргументы для рендера, отрендерить forNodeTpl.
      val args = MForNodeTplArgs(
        mnode         = request.mnode,
        mContractOpt  = mContractOpt,
        mBalances     = mBalances
      )
      Ok( forNodeTpl(args) )
    }
  }

  /**
   * Страница редактирования посуточного тарифа узла.
   * @param nodeId id узла, для которого редактируется тариф.
   */
  def editNodeTfDaily(nodeId: String) = IsSuNodeGet(nodeId).async { implicit request =>
    // Вычисляем эффективный тариф узла.
    val realTfFut = tfDailyUtil.nodeTf(request.mnode)

    val formEmpty = tfDailyUtil.tfDailyForm
    val formFut = for (realTf <- realTfFut) yield {
      formEmpty.fill(realTf)
    }

    _editNodeTfDaily(formFut, Ok)
  }

  private def _editNodeTfDaily(formFut: Future[Form[MDailyTf]], rs: Status)
                              (implicit request: INodeReq[_]): Future[Result] = {
    // Собираем доступные календари.
    val mcalsFut = mCalendars.getAll()

    // Рендер результата, когда все исходные вданные будут собраны.
    for {
      mcals  <- mcalsFut
      form   <- formFut
    } yield {
      val args = MTfDailyEditTplArgs(
        mnode     = request.mnode,
        mcals     = mcals,
        tf        = form
      )
      val html = tfDailyEditTpl(args)
      rs(html)
    }
  }

  /**
   * Сабмит формы редактирования тарифа узла.
   * @param nodeId id редактируемого узла.
   * @return редирект на forNode().
   */
  def editNodeTfDailySubmit(nodeId: String) = IsSuNodePost(nodeId).async { implicit request =>
    tfDailyUtil.tfDailyForm.bindFromRequest().fold(
      {formWithErrors =>
        val respFut = _editNodeTfDaily(Future.successful(formWithErrors), NotAcceptable)
        LOGGER.debug(s"editNodeTfDailySubmit($nodeId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        respFut
      },
      {tf2 =>
        val saveFut = tfDailyUtil.updateNodeTf(request.mnode, Some(tf2))
        for (_ <- saveFut) yield {
          Redirect(routes.SysBilling.forNode(nodeId))
            .flashing(FLASH.SUCCESS -> "Сохранен посуточный тариф для узла")
        }
      }
    )
  }


  /**
   * Удаление текущего тарифа узла.
   * @param nodeId id редактируемого узла.
   * @return Редирект на forNode().
   */
  def deleteNodeTfDaily(nodeId: String) = IsSuNodePost(nodeId).async { implicit request =>
    // Запустить стирание посуточного тарифа узла.
    val saveFut = tfDailyUtil.updateNodeTf(request.mnode, newTf = None)
    // Отредиректить юзера на биллинг узла, когда всё будет готово.
    for (_ <- saveFut) yield {
      Redirect( routes.SysBilling.forNode(nodeId) )
        .flashing(FLASH.SUCCESS -> s"Сброшен тариф узла: ${request.mnode.guessDisplayNameOrId.orNull}")
    }
  }

}
