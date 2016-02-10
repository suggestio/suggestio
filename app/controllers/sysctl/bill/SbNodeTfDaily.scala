package controllers.sysctl.bill

import controllers.routes
import models.mcal.IMCalendars
import models.msys.bill.MTfDailyEditTplArgs
import models.req.INodeReq
import models.MDailyTf
import play.api.data.Form
import play.api.mvc.Result
import util.PlayMacroLogsI
import util.acl.IsSuNode
import util.billing.ITfDailyUtilDi
import views.html.sys1.bill.daily._

import scala.concurrent.Future


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 11:38
 * Description: Трейт с экшенами контроллера sys billing редактирования полуточных тарифов.
 */
trait SbNodeTfDaily
  extends IsSuNode
  with PlayMacroLogsI
  with ITfDailyUtilDi
  with IMCalendars
{

  import mCommonDi._

  /**
   * Страница редактирования посуточного тарифа узла.
 *
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
 *
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
 *
   * @param nodeId id редактируемого узла.
   * @return Редирект на forNode().
   */
  def deleteNodeTfDaily(nodeId: String) = IsSuNodePost(nodeId).async { implicit request =>
    // Запустить стирание посуточного тарифа узла.
    val saveFut = tfDailyUtil.updateNodeTf(request.mnode, newTf = None)

    LOGGER.trace(s"deleteNodeTfDaily($nodeId): erasing tf...")

    // Отредиректить юзера на биллинг узла, когда всё будет готово.
    for (_ <- saveFut) yield {
      Redirect( routes.SysBilling.forNode(nodeId) )
        .flashing(FLASH.SUCCESS -> s"Сброшен тариф узла: ${request.mnode.guessDisplayNameOrId.orNull}")
    }
  }

}
