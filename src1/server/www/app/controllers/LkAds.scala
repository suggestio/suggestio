package controllers

import io.suggest.ads.{MLkAdsForm, MLkAdsFormInit}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.init.routed.MJsiTgs
import javax.inject.Inject
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.IsNodeAdmin
import views.html.lk.ads._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 18:21
  * Description: Менеджер карточек в личном кабинете.
  * React-замена MarketLkAdn.showNodeAds() и связанным экшенам.
  */
class LkAds @Inject() (
                        isNodeAdmin             : IsNodeAdmin,
                        override val mCommonDi  : ICommonDi
                      )
  extends SioController
{

  import mCommonDi._


  /** Рендер странице с react-формой управления карточками.
    * Сами карточки приходят отдельным экшеном, здесь же только страница с конфигом
    * формы карточек.
    *
    * @param nodeKey ключ узла - цепочка узлов.
    * @return 200 ОК + страница управления карточками узла.
    */
  def adsPage(nodeKey: RcvrKey) = csrf.AddToken {
    isNodeAdmin(nodeKey, U.Lk).async { implicit request =>
      // Заготовить контекст.
      val ctxFut = for {
        lkCtxData0 <- request.user.lkCtxDataFut
      } yield {
        implicit val lkCtxData2 = lkCtxData0.withJsiTgs(
          MJsiTgs.LkAdsForm :: lkCtxData0.jsiTgs
        )
        implicitly[Context]
      }

      // Собираем состояние формы:
      val initS = MLkAdsFormInit(
        form = MLkAdsForm(
          nodeKey = nodeKey
        )
      )
      val initJson = Json.toJson( initS )

      for {
        ctx <- ctxFut
      } yield {
        val html = lkAdsTpl(
          mnode   = request.mnode,
          state0  = initJson.toString
        )(ctx)

        Ok(html)
      }
    }
  }


  /** Запрос ещё карточек с сервера.
    * По сути, надо подготовить карточки, как для плитки.
    *
    * @param nodeKey Ключ до родительского узла.
    * @param offset Сдвиг в кол-ве карточек.
    * @return JSON.
    */
  def getAds(nodeKey: RcvrKey, offset: Int) = isNodeAdmin(nodeKey).async { implicit request =>
    ???
  }

}
