package controllers

import javax.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import util.acl.IsSu
import util.adv.direct.AdvRcvrsUtil
import util.img.DynImgUtil
import views.html.sys1.debug._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 16:16
 * Description: Sys-контроллер для отладки.
 */
@Singleton
class SysDebug @Inject() (
                           advRcvrsUtil                  : AdvRcvrsUtil,
                           dynImgUtil                    : DynImgUtil,
                           isSu                          : IsSu,
                           sioControllerApi              : SioControllerApi,
                           mCommonDi                     : ICommonDi,
) {

  import sioControllerApi._
  import mCommonDi._

  /** Экшен для отображения индексной страницы. */
  def index = csrf.AddToken {
    isSu() { implicit request =>
      Ok( indexTpl() )
    }
  }

  /** Запуск поиска и ремонта неправильных ресиверов в карточках. */
  def resetAllRcvrs = csrf.Check {
    isSu().async { implicit request =>
      for (count <- advRcvrsUtil.resetAllReceivers()) yield {
        Ok(s"$count ads updated.")
      }
    }
  }


  /** Запуск сброса значений полей MJdEdgeId.dynFormat для картинок на orig-значения. */
  def resetImgsToOrig = csrf.Check {
    isSu().async { implicit request =>
      for {
        nodesCompleted <- dynImgUtil.resetJdImgDynFormatsToOrigOnNodes()
      } yield {
        Ok(s"$nodesCompleted nodes processed.")
      }
    }
  }


  /** Запуск удаления всех img-деривативов. */
  def deleteAllDynImgDerivatives = csrf.Check {
    isSu().async { implicit request =>
      for {
        res <- dynImgUtil.deleteAllDerivatives( deleteEvenStorageMissing = false )
      } yield {
        Ok(s"$res imgs deleted.")
      }
    }
  }

}
