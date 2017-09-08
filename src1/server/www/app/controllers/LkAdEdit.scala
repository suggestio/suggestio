package controllers

import javax.inject.{Inject, Singleton}

import io.suggest.ad.edit.m.{MAdEditForm, MAdEditFormConf, MAdEditFormInit}
import io.suggest.es.model.MEsUuId
import io.suggest.init.routed.MJsiTgs
import io.suggest.jd.MJdEditEdge
import io.suggest.model.n2.edge.MPredicates
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.m.mctx.CtxData
import models.im.MImg3
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.IReq
import play.api.libs.json.Json
import util.acl.{CanEditAd, IsNodeAdmin}
import util.ad.LkAdEdFormUtil
import util.img.DynImgUtil
import views.html.lk.ad.edit._

import scala.concurrent.Future

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
                           isNodeAdmin                            : IsNodeAdmin,
                           lkAdEdFormUtil                         : LkAdEdFormUtil,
                           override val mCommonDi                 : ICommonDi
                         )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._


  /** Страница создания новой карточки.
    *
    * @param producerIdU id текущего узла-продьюсера.
    * @return 200 OK и html-страница с формой создания карточки.
    */
  def createAd(producerIdU: MEsUuId) = csrf.AddToken {
    val producerId = producerIdU.id
    isNodeAdmin(producerId, U.Lk).async { implicit request =>
      // Сразу собираем контекст, так как он как бы очень нужен здесь.
      val ctxFut = _ctxDataFut.map { implicit ctxData0 =>
        implicitly[Context]
      }

      val docFormFut = ctxFut.flatMap(lkAdEdFormUtil.defaultEmptyDocument(_))

      // Конфиг формы содержит только данные о родительском узле-продьюсере.
      val formConf = MAdEditFormConf(
        producerId  = producerId,
        adId        = None
      )

      // Собрать модель инициализации формы редактора
      val formInitJsonStrFut0 = for {
        docForm <- docFormFut
      } yield {
        val formInit = MAdEditFormInit(
          conf = formConf,
          form = docForm
        )
        _formInit2str( formInit )
      }

      // Отрендерить страницы с формой, когда всё будет готово.
      for {
        ctx             <- ctxFut
        formInitJsonStr <- formInitJsonStrFut0
      } yield {
        // Используем тот же edit-шаблон. что и при редактировании. Они отличаются только заголовками страниц.
        val html = adEditTpl(
          mad     = None,
          parent  = request.mnode,
          state0  = formInitJsonStr
        )(ctx)
        Ok( html )
      }
    }
  }


  /** Экшен рендера страницы с формой редактирования карточки.
    *
    * @param adIdU id рекламной карточки.
    * @return 200 OK с HTML-страницей для формы редактирования карточки.
    */
  def editAd(adIdU: MEsUuId) = csrf.AddToken {
    val adId = adIdU.id
    canEditAd(adId, U.Lk).async { implicit request =>
      // Запускаем фоновые операции: подготовить ctxData:
      val ctxData1Fut = _ctxDataFut

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
      val textPred = MPredicates.JdContent.Text
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

      val formInitJsonStr = _formInit2str( formInit )

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


  /** Собрать инстанс ctxData. */
  private def _ctxDataFut(implicit request: IReq[_]): Future[CtxData] = {
    for (ctxData0 <- request.user.lkCtxDataFut) yield {
      ctxData0.withJsiTgs(
        MJsiTgs.LkAdEditR :: ctxData0.jsiTgs
      )
    }
  }


  /** Сериализовать в строку инсанс MAdEditFormInit. */
  private def _formInit2str(formInit: MAdEditFormInit): String = {
    // TODO Есть мнение, что надо будет заюзать MsgPack+base64, т.е. и бинарь, и JSON в одном флаконе.
    // Этот JSON рендерится в html-шаблоне сейчас в строковую помойку вида "&quota&quot;,&quot" и довольно толстоват.
    // Только сначала желательно бы выкинуть boopickle, заменив её на play-json или msgpack везде, где уже используется.
    Json.toJson(formInit).toString()
  }


}
