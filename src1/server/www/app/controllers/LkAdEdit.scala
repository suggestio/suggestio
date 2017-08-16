package controllers

import javax.inject.{Inject, Singleton}

import io.suggest.es.model.MEsUuId
import io.suggest.model.n2.edge.MPredicates
import io.suggest.util.logs.MacroLogsImpl
import models.im.MImg3
import models.mproj.ICommonDi
import util.acl.CanEditAd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.17 12:04
  * Description: Контроллер react-формы редактора карточек.
  * Идёт на смену MarketAd, который обслуживал старинную форму редактора.
  */
@Singleton
class LkAdEdit @Inject() (

                           tempImgSupport                         : TempImgSupport,
                           canEditAd                              : CanEditAd,
                           //@Named("blk") override val blkImgMaker : IMaker,
                           override val mCommonDi                 : ICommonDi
                         )
  extends SioController
  with MacroLogsImpl
{

  import mCommonDi._


  /** Список предикатов, которые содержат данные о ресурсах рекламной карточки. */
  //def AD_RSC_PREDICATES = MPredicates.Bg :: Nil


  /** Экшен рендера страницы с формой редактирования карточки.
    *
    * @param adIdU id рекламной карточки.
    * @return 200 OK с HTML-страницей для формы редактирования карточки.
    */
  def editor(adIdU: MEsUuId) = csrf.AddToken {
    val adId = adIdU.id
    canEditAd(adId, U.Lk).async { implicit request =>
      // Нужно собрать начальное состояние формы. Для этого нужно собрать ресурсы текущей карточки.

      // Собираем картинки, используемые в карточке:
      for {
        // Пройти по BgImg-эджам карточки:
        medge <- request.mad.edges
          .withPredicateIter( MPredicates.Bg )
        // id узла эджа -- это идентификатор картинки.
        edgeNodeId <- medge.nodeIds
      } yield {
        val mimg = MImg3(medge)
        edgeNodeId -> mimg
      }

      ???
    }
  }

}
