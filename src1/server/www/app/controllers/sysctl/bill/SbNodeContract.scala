package controllers.sysctl.bill

import controllers.{SioController, routes}
import io.suggest.mbill2.m.contract.{IMContracts, MContract}
import io.suggest.model.n2.node.IMNodes
import io.suggest.util.logs.IMacroLogs
import models.req.{INodeContractReq, INodeReq}
import play.api.data.Form
import play.api.mvc.Result
import util.acl.{IIsSuNodeContract, IsSuNodeNoContract}
import util.billing.{IBill2UtilDi, IContractUtilDi}
import views.html.sys1.bill.contract._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 11:42
 * Description: Трейт для sys billing контроллера для поддержки редактирования контрактов.
 */
trait SbNodeContract
  extends SioController
  with IIsSuNodeContract
  with IMacroLogs
  with IContractUtilDi
  with IMContracts
  with IMNodes
  with IBill2UtilDi
{

  import mCommonDi._

  val isSuNodeNoContract: IsSuNodeNoContract

  // ----------------------------- Создание контракта ----------------------------------

  /**
   * Рендер страницы с формой создания контракта.
   *
   * @param nodeId id узла, для которого создаётся контракт.
   */
  def createContract(nodeId: String) = csrf.AddToken {
    isSuNodeNoContract(nodeId).async { implicit request =>
      val form0 = contractUtil.contractForm

      val formDummy = form0.fill(
        MContract(
          suffix = Some( mContracts.SUFFIX_DFLT )
        )
      )

      _createContract(formDummy, Ok)
    }
  }

  private def _createContract(cf: Form[MContract], rs: Status)(implicit request: INodeReq[_]): Future[Result] = {
    val html = createTpl(
      mnode = request.mnode,
      cf    = cf
    )
    rs(html)
  }

  /**
   * Сабмит формы создания контракта.
   *
   * @param nodeId id узла, для которого создаётся контракт.
   */
  def createContractSubmit(nodeId: String) = csrf.Check {
    isSuNodeNoContract(nodeId).async { implicit request =>
      contractUtil.contractForm.bindFromRequest().fold(
        {formWithErrors =>
          val respFut = _createContract(formWithErrors, NotAcceptable)
          LOGGER.debug(s"createContractSubmit($nodeId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          respFut
        },
        {mc =>
          LOGGER.trace(s"Creating new contract for node $nodeId...")
          val mcAct = mContracts.insertOne(mc)
          val mcSaveFut = slick.db.run(mcAct)

          // Запустить сохранение нового id контракта в узел.
          val nodeSaveFut = mcSaveFut.flatMap { mc =>
            mNodes.tryUpdate(request.mnode) { mnode =>
              assert(mnode.billing.contractId.isEmpty)
              mnode.copy(
                billing = mnode.billing.copy(
                  contractId = mc.id
                )
              )
            }
          }

          // Если контракт создан, а узел не обновлён, то надо удалить свежесозданный контракт.
          for {
            mc2 <- mcSaveFut
            ex  <- nodeSaveFut.failed
          } {
            val contractId = mc2.id.get
            val deleteFut = slick.db.run {
              bill2Util.deleteContract( contractId )
            }
            LOGGER.warn("Rollbacking contract save because of node save error", ex)
            if (LOGGER.underlying.isTraceEnabled) {
              deleteFut.onComplete { result =>
                LOGGER.trace(s"Deleted recently created contract $contractId. res => $result")
              }
            }
          }

          for {
            _   <- nodeSaveFut
            mc  <- mcSaveFut
          } yield {
            Redirect( routes.SysBilling.forNode(nodeId) )
              .flashing(FLASH.SUCCESS -> s"Создан контракт ${mc.legalContractId} для узла ${request.mnode.guessDisplayNameOrIdOrEmpty}")
          }
        }
      )
    }
  }



  // ----------------------------- Редактирование контракта ----------------------------------

  /**
   * Экшен рендера страницы редактирования контракта узла.
   *
   * @param nodeId id изменяемого узла.
   */
  def editContract(nodeId: String) = csrf.AddToken {
    isSuNodeContract(nodeId).async { implicit request =>
      val cf = contractUtil.contractForm.fill( request.mcontract )
      _editContract(cf, Ok)
    }
  }

  private def _editContract(cf: Form[MContract], rs: Status)
                           (implicit request: INodeContractReq[_]): Future[Result] = {
    val html = editTpl(request.mnode, request.mcontract, cf)
    rs(html)
  }

  /**
   * Сабмит формы редактирования контракта.
   *
   * @param nodeId id узла.
   */
  def editContractSubmit(nodeId: String) = csrf.Check {
    isSuNodeContract(nodeId).async { implicit request =>
      contractUtil.contractForm.bindFromRequest().fold(
        {formWithErrors =>
          val fut = _editContract(formWithErrors, NotAcceptable)
          LOGGER.debug(s"editContractSubmit($nodeId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
          fut
        },
        {mc1 =>
          val mc2 = request.mcontract.copy(
            dateCreated = mc1.dateCreated,
            hiddenInfo  = mc1.hiddenInfo,
            suffix      = mc1.suffix
          )
          val updFut = slick.db.run(
            mContracts.updateOne(mc2)
          )

          LOGGER.trace(s"editContractSubmit($nodeId): Saving new contract: $mc2")

          for (rowsUpdated <- updFut if rowsUpdated == 1) yield {
            Redirect( routes.SysBilling.forNode(nodeId) )
              .flashing(FLASH.SUCCESS -> s"Сохранён контракт ${mc2.legalContractId}.")
          }
        }
      )
    }
  }


  // ----------------------------- Редактирование контракта ----------------------------------


  /**
   * Сабмит удаления контракта.
   *
   * @param nodeId id узла.
   * @return Редирект на forNode().
   */
  def deleteContractSubmit(nodeId: String) = csrf.Check {
    isSuNodeContract(nodeId).async { implicit request =>
      val contractId = request.mcontract.id.get
      val deleteFut = slick.db.run {
        bill2Util.deleteContract( contractId )
      }

      lazy val logPrefix = s"deleteContractSubmit($nodeId):"

      LOGGER.debug(s"$logPrefix Erasing #${request.mcontract.legalContractId}")

      val nodeSaveFut = deleteFut.flatMap { _ =>
        mNodes.tryUpdate(request.mnode) { mnode =>
          mnode.copy(
            billing = mnode.billing.copy(
              contractId = None
            )
          )
        }
      }

      // Отработать сценарии, когда возникает ошибка при сохранении узла.
      for (_ <- deleteFut) {
        for (ex <- nodeSaveFut) {
          val act2 = mContracts.insertOne( request.mcontract )
          val reInsFut = slick.db.run(act2)
          LOGGER.error(s"$logPrefix Re-inserting deleted contract after node update failure: ${request.mcontract}", ex)
          for (ex2 <- reInsFut.failed)
            LOGGER.error(s"$logPrefix Unable to re-insert $act2", ex2)
        }
      }

      for {
        _           <- nodeSaveFut
        rowsDeleted <- deleteFut
      } yield {
        val res = Redirect( routes.SysBilling.forNode(nodeId) )
        val flash = rowsDeleted match {
          case 1 =>
            FLASH.SUCCESS -> "Контракт удалён безвозратно."
          case 0 =>
            FLASH.ERROR   -> "Кажется, контракт уже был удалён."
        }
        res.flashing(flash)
      }
    }
  }

}
