package controllers.sysctl.mdr

import controllers.routes
import io.suggest.model.n2.edge.{MEdgeInfo, MNodeEdges}
import models._
import models.mdr._
import models.req.IAdReq
import org.joda.time.DateTime
import util.acl.{IsSuperuser, IsSuperuserMad}
import views.html.sys1.mdr._

import scala.concurrent.Future

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


  /** Страница с бесплатно-размещёнными рекламными карточками, подлежащими модерации s.io.
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
      refuseFormM    = refuseFormM,
      submitCall  = routes.SysMdr.freeAdvMdrBan(adId)
    )
    val render = _refusePopupTpl(args)
    Ok(render)
  }


  /** Сабмит одобрения пост-модерации бесплатного размещения.
    * Нужно выставить в карточку данные о модерации. */
  def freeAdvMdrAccept(adId: String) = IsSuperuserMadPost(adId).async { implicit request =>
    // Запускаем сохранение данных модерации.
    val updFut = _updMdrEdge {
      MEdgeInfo(
        flag   = Some(true),
        dateNi = _someNow
      )
    }

    // После завершения асинхронный операций, вернуть результат.
    for (_ <- updFut) yield {
      Redirect( routes.SysMdr.forAd(adId) )
        .flashing(FLASH.SUCCESS -> "Карточка помечена как проверенная.")
    }
  }


  private def _someNow = Some( DateTime.now )

  /** Код обновления эджа модерации живёт здесь. */
  private def _updMdrEdge(info: MEdgeInfo)(implicit request: IAdReq[_]): Future[MNode] = {
    // Сгенерить обновлённые данные модерации.
    val mdr2 = MEdge(
      nodeId    = request.user.personIdOpt.get,
      predicate = MPredicates.ModeratedBy,
      info      = info
    )

    LOGGER.trace(s"_updMdrEdge() Mdr mad[${request.mad.idOrNull}] with mdr-edge $mdr2")

    // Запускаем сохранение данных модерации.
    MNode.tryUpdate(request.mad) { mad0 =>
      mad0.copy(
        edges = mad0.edges.copy(
          out = {
            val iter0 = mad0.edges.withoutPredicateIter( MPredicates.ModeratedBy )
            val iter2 = Iterator(mdr2)
            MNodeEdges.edgesToMap1(iter0 ++ iter2)
          }
        )
      )
    }
  }


  /** Сабмит формы блокирования бесплатного размещения рекламной карточки. */
  def freeAdvMdrBan(adId: String) = IsSuperuserMadPost(adId).async { implicit request =>
    refuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"freeAdvMdrBan($adId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        Redirect( routes.SysMdr.forAd(adId) )
          .flashing(FLASH.ERROR -> "Возникли проблемы. см application.log")
      },
      {reason =>
        // Сохранить отказ в модерации.
        val saveFut = _updMdrEdge {
          MEdgeInfo(
            dateNi    = _someNow,
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
