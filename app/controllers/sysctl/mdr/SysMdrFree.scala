package controllers.sysctl.mdr

import controllers.routes
import io.suggest.model.n2.edge.MEdgeInfo
import models._
import models.mdr._
import org.joda.time.DateTime
import util.acl.{IsSuperuser, IsSuperuserMad}
import views.html.sys1.mdr._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 11:57
  * Description: Поддержка экшенов модерации
  */
trait SysMdrFree
  extends SysMdrBase
  with IsSuperuser
  with IsSuperuserMad
{

  import mCommonDi._


  /**
    * Страница с бесплатно-размещёнными рекламными карточками, подлежащими модерации s.io.
    *
    * ! Следует не забывать, что предикат Receiver.Self появился только в 2016.feb.28, поэтому
    * ! старые карточки могут не отображаться в списках на модерацию. Чтобы они появились в модерации,
    * ! надо переустановить галочку саморазмещения.
    *
    * @param args Аргументы для поиска (QSB).
    */
  def freeAdvs(args: MdrSearchArgs) = IsSuperuser.async { implicit request =>
    // Необходимо искать карточки, требующие модерации/обработки.
    val madsFut = MNode.dynSearch( args.toNodeSearch )
    _adsPage(madsFut, args)
  }


  /** Страница для модерации одной карточки. */
  def refuseFreeAdvPopup(adId: String) = IsSuperuserMadGet(adId).apply { implicit request =>
    val args = MSysMdrRefusePopupTplArgs(
      refuseFormM = sysMdrUtil.refuseFormM,
      submitCall  = routes.SysMdr.freeAdvMdrBan(adId)
    )
    val render = _refusePopupTpl(args)
    Ok(render)
  }


  /** Сабмит одобрения пост-модерации бесплатного размещения.
    * Нужно выставить в карточку данные о модерации. */
  def freeAdvMdrAccept(adId: String) = IsSuperuserMadPost(adId).async { implicit request =>
    // Запускаем сохранение данных модерации.
    val updFut = sysMdrUtil.updMdrEdge {
      MEdgeInfo(
        flag   = Some(true),
        dateNi = sysMdrUtil.someNow
      )
    }

    // После завершения асинхронный операций, вернуть результат.
    for (_ <- updFut) yield {
      Redirect( routes.SysMdr.forAd(adId) )
        .flashing(FLASH.SUCCESS -> "Карточка помечена как проверенная.")
    }
  }


  /** Сабмит формы блокирования бесплатного размещения рекламной карточки. */
  def freeAdvMdrBan(adId: String) = IsSuperuserMadPost(adId).async { implicit request =>
    sysMdrUtil.refuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"freeAdvMdrBan($adId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        Redirect( routes.SysMdr.forAd(adId) )
          .flashing(FLASH.ERROR -> "Возникли проблемы. см application.log")
      },
      {reason =>
        // Сохранить отказ в модерации.
        val saveFut = sysMdrUtil.updMdrEdge {
          MEdgeInfo(
            dateNi    = sysMdrUtil.someNow,
            commentNi = Some(reason),
            flag      = Some(false)
          )
        }

        for (_ <- saveFut) yield {
          Redirect( routes.SysMdr.forAd(adId) )
            .flashing(FLASH.SUCCESS -> "Карточка убрана из бесплатной выдачи.")
        }
      }
    )
  }

}
