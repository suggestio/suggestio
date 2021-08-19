package controllers

import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImplLazy

import javax.inject.Inject
import play.api.Environment
import play.api.mvc.Result
import util.acl.{IsSu, SioControllerApi}
import util.adv.direct.AdvRcvrsUtil
import util.img.DynImgUtil
import views.html.sys1.debug._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 16:16
 * Description: Sys-контроллер для отладки.
 */
final class SysDebug @Inject() (
                                 isSu                          : IsSu,
                                 sioControllerApi              : SioControllerApi,
                               )
  extends MacroLogsImplLazy
{

  import sioControllerApi._
  import mCommonDi.{ec, current}

  private lazy val advRcvrsUtil = current.injector.instanceOf[AdvRcvrsUtil]
  private lazy val dynImgUtil = current.injector.instanceOf[DynImgUtil]
  private lazy val env = current.injector.instanceOf[Environment]
  private lazy val csrf = injector.instanceOf[Csrf]

  /** Экшен для отображения индексной страницы. */
  def index() = csrf.AddToken {
    isSu() { implicit request =>
      Ok( indexTpl() )
    }
  }

  /** Запуск поиска и ремонта неправильных ресиверов в карточках. */
  def resetAllRcvrs() = csrf.Check {
    isSu().async { implicit request =>
      for (count <- advRcvrsUtil.resetAllReceivers()) yield {
        Ok(s"$count ads updated.")
      }
    }
  }


  /** Запуск сброса значений полей MJdEdgeId.dynFormat для картинок на orig-значения. */
  def resetImgsToOrig() = csrf.Check {
    isSu().async { implicit request =>
      for {
        nodesCompleted <- dynImgUtil.resetJdImgDynFormatsToOrigOnNodes()
      } yield {
        Ok(s"$nodesCompleted nodes processed.")
      }
    }
  }


  /** Запуск удаления всех img-деривативов. */
  def deleteAllDynImgDerivatives() = csrf.Check {
    isSu().async { implicit request =>
      for {
        res <- dynImgUtil.deleteAllDerivatives( deleteEvenStorageMissing = false )
      } yield {
        Ok(s"$res imgs deleted.")
      }
    }
  }


  private def _CP_PATH_LEN_MAX = 255
  private def _cpPathForm = {
    import play.api.data._, Forms._
    Form(
      single(
        "cpPath" -> nonEmptyText(maxLength = _CP_PATH_LEN_MAX, minLength = 3)
      )
    )
  }


  /** Рендер формы запроса ресурса из classpath. */
  def getClassPathResourceInfoGet() = csrf.AddToken {
    isSu() { implicit request =>
      val form = _cpPathForm
      Ok( CpResFormTpl(form) )
    }
  }

  /** Сабмит формы запроса ресурса из classpath. */
  def getClassPathResourceInfoPost() = csrf.Check {
    isSu() { implicit request =>
      _cpPathForm.bindFromRequest().fold(
        {errors =>
          val msg = s"failed to bind form: ${formatFormErrors(errors)}"
          LOGGER.warn(msg)
          NotAcceptable(msg)
        },
        {cpPath =>
          env
            .resource(cpPath)
            .fold[Result] {
              NotFound(s"Resource not found in ClassPath: $cpPath")
            } { resUrl =>
              val conn = resUrl.openConnection()
              val msg = s"$cpPath : ${conn.getContentType} ${conn.getContentLength} bytes"
              Ok(msg)
            }
        }
      )
    }
  }

}
