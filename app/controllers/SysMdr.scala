package controllers

import com.google.inject.Inject
import controllers.sysctl.mdr.{SysMdrFree, SysMdrPaid}
import io.suggest.mbill2.m.item.MItems
import io.suggest.model.n2.node.MNode
import models.mdr.MdrSearchArgs
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.acl.IsSuperuser
import util.billing.Bill2Util
import util.lk.LkAdUtil
import util.n2u.N2NodesUtil
import util.showcase.ShowcaseUtil
import views.html.sys1.mdr._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 10:45
 * Description: Sys Moderation - контроллер, заправляющий s.io-модерацией рекламных карточек.
 * 2016.mar.1: Контроллер разбит на трейты, живущие в [[controllers.sysctl.mdr]].
 */

class SysMdr @Inject() (
  override val lkAdUtil             : LkAdUtil,
  override val scUtil               : ShowcaseUtil,
  override val n2NodesUtil          : N2NodesUtil,
  override val mItems               : MItems,
  override val bill2Util            : Bill2Util,
  override val mCommonDi            : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IsSuperuser
  with SysMdrFree
  with SysMdrPaid
{

  import mCommonDi._


  /** Отобразить начальную страницу раздела модерации рекламных карточек. */
  def index = IsSuperuser { implicit request =>
    Ok( mdrIndexTpl() )
  }

  /**
    * Поиск "следующей" для модерации карточки. Не обязательно эта карточка идёт следующей по списку.
    *
    * @return Редирект на страницу модерации какой-то карточки, требующей модерации.
    */
  def rdrToNextAd(args: MdrSearchArgs) = IsSuperuser.async { implicit request =>
    // Выставляем в аргументы limit = 1, т.к. нам нужна только одна карточка.
    val args1 = args.copy(
      limitOpt = Some(1)
    )

    slick.db
      // Ищем следующую карточку через биллинг и очередь на модерацию.
      .run {
        _findPaidAdIds4MdrAction(args1)
      }
      .map(_.headOption.get)    // почему-то .head не возвращает NSEE.
      // Ищем след. карточку через бесплатные размещения.
      .recoverWith { case _: NoSuchElementException =>
        // Если нет paid-модерируемых карточек, то поискать бесплатные размещения.
        val fut = MNode.dynSearchIds( args1.toNodeSearch )
        LOGGER.trace(s"rdrToNextAd(): No more paid advs, looking for free advs...\n $args")
        fut.map(_.head)
      }
      .map { adId =>
        Redirect( routes.SysMdr.forAd(adId) )
      }
      .recover { case _: NoSuchElementException =>
        Redirect( routes.SysMdr.index() )
          .flashing(FLASH.ERROR -> "Больше нет карточек для модерации.")
      }
  }

}

