package controllers.sysctl.mdr

import controllers.routes
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.model.n2.edge.MEdgeInfo
import models._
import models.mdr._
import util.acl.{IsSuperuser, IsSuperuserMad}
import util.billing.IBill2UtilDi
import util.mdr.SysMdrUtil
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
  with IBill2UtilDi
{

  import mCommonDi._

  override val sysMdrUtil: SysMdrUtil

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
      submitCall  = routes.SysMdr.freeAdvMdrBan(adId),
      withModes   = true
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
      {res =>
        val someReason = Some(res.reason)

        // Сохранить отказ в бесплатной модерации.
        val saveFreeFut = sysMdrUtil.updMdrEdge {
          MEdgeInfo(
            dateNi    = sysMdrUtil.someNow,
            commentNi = someReason,
            flag      = Some(false)
          )
        }

        // Если задан режим, то произвести какие-то дополнительные действия.
        val saveFut = res.mode match {
          // Только обновить эдж
          case MRefuseModes.OnlyThis =>
            saveFreeFut

          case other =>
            val q1: sysMdrUtil.Q_t = other match {
              // и отказать в остальных реквестах
              case MRefuseModes.WithReqs =>
                sysMdrUtil.itemsQueryAwaiting(adId)
              // и резануть все текущие item'ы резануть
              case MRefuseModes.WithAll =>
                val statuses = MItemStatuses.advBusy.toSeq
                sysMdrUtil.onlyStatuses(
                  sysMdrUtil.itemsQuery(adId),
                  statuses
                )
            }
            saveFreeFut.flatMap { _ =>
              sysMdrUtil._processItemsForAd(
                nodeId  = adId,
                q       = q1
              )(bill2Util.refuseItemAction(_, someReason))
            }
        }

        // Дождаться итогов, вернуть модератору результат.
        for (_ <- saveFut) yield {
          Redirect( routes.SysMdr.forAd(adId) )
            .flashing(FLASH.SUCCESS -> "Отказано.")
        }
      }
    )
  }

}
