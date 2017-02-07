package controllers.sysctl.mdr

import controllers.routes
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.model.n2.edge.MEdgeInfo
import io.suggest.model.n2.node.IMNodes
import models.mdr._
import util.acl.{IsSuperuser, IIsSuMad}
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
  with IIsSuMad
  with IBill2UtilDi
  with IMNodes
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
  def freeAdvs(args: MdrSearchArgs) = IsSuGet.async { implicit request =>
    // Необходимо искать карточки, требующие модерации/обработки.
    val madsFut = mNodes.dynSearch( args.toNodeSearch )
    _adsPage(madsFut, args)
  }


  /** Страница для модерации одной карточки. */
  def refuseFreeAdvPopup(adId: String) = isSuMad.Get(adId).apply { implicit request =>
    val form = sysMdrUtil.refuseFormM.fill(
      MRefuseFormRes(
        reason  = "",
        modeOpt = Some( MRefuseModes.OnlyThis )
      )
    )
    val args = MSysMdrRefusePopupTplArgs(
      refuseFormM = form,
      submitCall  = routes.SysMdr.freeAdvMdrBan(adId),
      modes       = MRefuseModes.valuesT
    )
    val render = _refusePopupTpl(args)
    Ok(render)
  }


  /** Сабмит одобрения пост-модерации бесплатного размещения.
    * Нужно выставить в карточку данные о модерации. */
  def freeAdvMdrAccept(adId: String) = isSuMad.Post(adId).async { implicit request =>
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
  def freeAdvMdrBan(adId: String) = isSuMad.Post(adId).async { implicit request =>
    lazy val logPrefix = s"freeAdvMdrBan($adId u:${request.user.personIdOpt.orNull}):"
    sysMdrUtil.refuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind form:\n${formatFormErrors(formWithErrors)}")
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
        res.mode match {
          // Только обновить эдж
          case MRefuseModes.OnlyThis =>
            LOGGER.trace(s"$logPrefix Refusing only free advs.")
            for (_ <- saveFreeFut) yield {
              Redirect( routes.SysMdr.forAd(adId) )
                .flashing(FLASH.SUCCESS -> "Отказано в бесплатном размещении.")
            }

          case other =>
            val q1: sysMdrUtil.Q_t = other match {
              // и отказать в остальных реквестах
              case MRefuseModes.WithReqs =>
                LOGGER.debug(s"$logPrefix Also refusing all unconfirmed advs.")
                sysMdrUtil.itemsQueryAwaiting(adId)
              // и резануть все текущие item'ы резануть
              case MRefuseModes.WithAll =>
                LOGGER.debug(s"$logPrefix Also refusing ALL ADVS!")
                val statuses = MItemStatuses.advBusy.toSeq
                sysMdrUtil.onlyStatuses(
                  sysMdrUtil.itemsQuery(adId),
                  statuses
                )
            }
            // Запустить необходимый отказ.
            val saveFut = saveFreeFut.flatMap { _ =>
              sysMdrUtil._processItemsForAd(
                nodeId  = adId,
                q       = q1
              )(bill2Util.refuseItemAction(_, someReason))
            }
            // Дождаться итогов, вернуть модератору результат.
            for (res <- saveFut) yield {
              val rdrArgs = MdrSearchArgs(hideAdIdOpt = Some(adId))
              val msg = other match {
                case MRefuseModes.WithReqs =>
                  s"Отказано в бесплатном и всех запрошенных размещениях (${res.itemsCount})."
                case MRefuseModes.WithAll =>
                  s"Отказано во всех размещениях, в т.ч. уже подтвержденных (${res.itemsCount})."
              }
              Redirect( routes.SysMdr.rdrToNextAd( rdrArgs ) )
                .flashing(FLASH.SUCCESS -> msg)
            }
        }
      }
    )
  }

}
