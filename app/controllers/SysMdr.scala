package controllers

import com.google.inject.Inject
import controllers.sysctl.mdr.{SysMdrFree, SysMdrPaid}
import io.suggest.mbill2.m.item.MItems
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

  /** Отобразить начальную страницу раздела модерации рекламных карточек. */
  def index = IsSuperuser { implicit request =>
    Ok( mdrIndexTpl() )
  }

}

