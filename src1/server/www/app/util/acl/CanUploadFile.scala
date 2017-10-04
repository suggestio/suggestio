package util.acl

import javax.inject.Inject

import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.req.ReqUtil
import models.mproj.ICommonDi
import models.mup.MUploadTargetQs
import models.req.IReq
import play.api.mvc._
import util.up.UploadUtil
import io.suggest.common.fut.FutureUtil.HellImplicits._
import japgolly.univeq._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 11:34
  * Description: ACL-проверка на предмет возможности текущему юзеру производить заливку файла в suggest.io.
  */
class CanUploadFile @Inject()(
                               aclUtil      : AclUtil,
                               reqUtil      : ReqUtil,
                               uploadUtil   : UploadUtil,
                               dab          : DefaultActionBuilder
                             )
  extends MacroLogsImpl
{

  /** Логика кода проверки прав, заворачивающая за собой фактический экшен, живёт здесь.
    * Это позволяет использовать код и в ActionBuilder, и в Action'ах.
    *
    * @param upTg Описание данных аплоада.
    * @param request0 HTTP-реквест.
    * @param f Фунция фактического экшена.
    * @tparam A Тип BodyParser'а.
    * @return Фьючерс результата.
    */
  private def _apply[A](upTg: MUploadTargetQs, request0: Request[A])(f: IReq[A] => Future[Result]): Future[Result] = {

    lazy val logPrefix = s"[${System.currentTimeMillis}]:"

    val uploadNow = uploadUtil.rightNow()

    // Сразу проверяем ttl, до user.isSuper, чтобы суперюзеры могли тоже увидеть возможные проблемы.
    if ( !uploadUtil.isTtlValid(upTg.validTillS, uploadNow) ) {
      // TTL upload-ссылки истёк. Огорчить юзера.
      val msg = "URL TTL expired"
      LOGGER.warn(s"$logPrefix $msg: ${upTg.validTillS}; now was == $uploadNow")
      Results.NotAcceptable(msg)

    } else {
      val mreq = aclUtil.reqFromRequest( request0 )
      val user = mreq.user
      if (upTg.personId !=* user.personIdOpt) {
        // Ссылка была выдана не текущему, а какому-то другому юзеру.
        val msg = "Unexpected userId"
        LOGGER.warn(s"$logPrefix [SEC] $msg: req.user#${user.personIdOpt} != args.user#${upTg.personId}")
        Results.Forbidden(msg)

      } else {
        // Больше нет препятствий для запуска экшена. Загруженный файл будет проверять и анализировать сам экшен.
        f(mreq)
      }
    }

  }


  /** Сборка ActionBuilder'а, проверяющего возможность для аплоада файла. */
  def apply(upTg: MUploadTargetQs): ActionBuilder[IReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[IReq] {
      override def invokeBlock[A](request: Request[A], block: (IReq[A]) => Future[Result]): Future[Result] = {
        _apply(upTg, request)(block)
      }
    }
  }


  /** Сборка заворачивающего экшена, который проверяет возможность для аплоада файла. */
  def A[A](upTg: MUploadTargetQs)(action: Action[A]): Action[A] = {
    dab.async(action.parser) { request =>
      _apply(upTg, request)(action.apply)
    }
  }

}
