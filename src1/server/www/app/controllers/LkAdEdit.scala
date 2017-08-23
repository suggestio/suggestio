package controllers

import javax.inject.{Inject, Singleton}

import io.suggest.ad.edit.m.{MAdEditForm, MAdEditFormConf, MAdEditFormInit}
import io.suggest.es.model.MEsUuId
import io.suggest.init.routed.MJsiTgs
import io.suggest.jd.MJdEditEdge
import io.suggest.model.n2.edge.MPredicates
import io.suggest.util.logs.MacroLogsImpl
import models.im.MImg3
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.CanEditAd
import util.img.DynImgUtil
import views.html.lk.ad.edit._

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
                           dynImgUtil                             : DynImgUtil,
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
  def editAd(adIdU: MEsUuId) = csrf.AddToken {
    val adId = adIdU.id
    canEditAd(adId, U.Lk).async { implicit request =>
      // Запускаем фоновые операции: подготовить ctxData:
      val ctxData1Fut = for (ctxData0 <- request.user.lkCtxDataFut) yield {
        ctxData0.withJsiTgs(
          MJsiTgs.AdForm :: ctxData0.jsiTgs
        )
      }

      // Нужно собрать начальное состояние формы. Для этого нужно собрать ресурсы текущей карточки.
      val imgPredicate = MPredicates.Bg

      // Собираем картинки, используемые в карточке:
      val imgEdgesIter = for {
        // Пройти по BgImg-эджам карточки:
        medge   <- request.mad.edges
          .withPredicateIter( imgPredicate )
        // id узла эджа -- это идентификатор картинки.
        edgeUid <- medge.doc.uid.iterator
        nodeId  <- medge.nodeIds
      } yield {
        val mimg = MImg3(medge)
        MJdEditEdge(
          predicate   = imgPredicate,
          id          = edgeUid,
          text        = None,
          nodeId      = Some(nodeId),
          url         = Some( dynImgUtil.imgCall(mimg).url )
        )
      }

      // Собрать тексты из эджей
      val textPred = MPredicates.Text
      val textEdgesIter = for {
        textEdge <- request.mad.edges
          .withPredicateIter( textPred )
        edgeUid  <- textEdge.doc.uid.iterator
        text     <- textEdge.doc.text
      } yield {
        MJdEditEdge(
          predicate = textPred,
          id        = edgeUid,
          text      = Some(text),
          nodeId    = None,
          url       = None
        )
      }

      val edEdges = Iterator(imgEdgesIter, textEdgesIter)
        .flatten
        .toSeq

      val nodeDoc = request.mad.extras.doc.get    // TODO .getOrElse(defaultDoc)

      // Собрать модель и отрендерить:
      val formInit = MAdEditFormInit(
        conf = MAdEditFormConf(
          producerId = request.producer.id.get,
          adId = request.mad.id
        ),
        form = MAdEditForm(
          template  = nodeDoc.template,
          edges     = edEdges
        )
      )

      val formInitJsonStr = Json.toJson(formInit).toString()

      // Дождаться готовности фоновых операций и отрендерить результат.
      for {
        ctxData1 <- ctxData1Fut
      } yield {
        implicit val ctxData9 = ctxData1

        // Вернуть HTTP-ответ, т.е. страницу с формой.
        Ok(adEditTpl(
          mad     = Some(request.mad),
          parent  = request.producer,
          state0  = formInitJsonStr
        ))
      }

    }
  }

}
