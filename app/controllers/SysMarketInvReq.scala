package controllers

import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import models._
import scala.concurrent.Future
import views.html.sys1.market.invreq._
import util.PlayMacroLogsImpl
import util.event.SiowebNotifier.Implicts.sn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.06.14 10:16
 * Description: Sys-контроллер для обработки входящих инвайтов.
 * v1: Отображать список, отображать каждый элемент, удалять.
 * v2: Нужен многошаговый и удобный мастер создания узлов со всеми контрактами и инвайтами, отметками о ходе
 * обработки запроса и т.д.
 */
object SysMarketInvReq extends SioController with PlayMacroLogsImpl {

  val MIRS_FETCH_COUNT = 300

  /** Вернуть страницу, отображающую всю инфу по текущему состоянию подсистемы IR.
    * Список реквестов, в первую очередь. */
  def index = IsSuperuser.async { implicit request =>
    MInviteRequest.getAll(MIRS_FETCH_COUNT) flatMap { mirs =>
      val thisCount = mirs.size
      val allCountFut: Future[Long] = if (thisCount >= MIRS_FETCH_COUNT) {
        MInviteRequest.countAll
      } else {
        Future successful thisCount.toLong
      }
      allCountFut.map { allCount =>
        Ok(irIndexTpl(mirs, thisCount, allCount))
      }
    }
  }


  /** Отрендерить страницу одного инвайт-реквеста. */
  def showIR(mirId: String) = IsSuperuserMir(mirId).async { implicit request =>
    import request.mir
    val mcOptFut = mir.company.fold[Future[Option[MCompany]]](
      { mc => Future successful Some(mc) },
      { mcId => MCompany.getById(mcId) }
    )
    for {
      mcOpt <- mcOptFut
    } yield {
      Ok(irShowOneTpl(mir, mcOpt))
    }
  }


  /** Удалить один IR. */
  def deleteIR(irId: String) = IsSuperuser.async { implicit request =>
    MInviteRequest.deleteById(irId) map { isDeleted =>
      val flasher: (String, String) = if (isDeleted) {
        "success" -> "Запрос на подключение удалён."
      } else {
        "error"   -> "Не найдено документа для удаления."
      }
      Redirect(routes.SysMarketInvReq.index())
        .flashing(flasher)
    }
  }

}
