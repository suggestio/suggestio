package controllers

import io.suggest.adn.edit.m.{MAdnEditForm, MAdnEditFormConf, MAdnEditFormInit, MAdnResView}
import io.suggest.es.model.MEsUuId
import io.suggest.file.up.MFile4UpProps
import io.suggest.img.{MImgEdgeWithOps, MImgFmts}
import io.suggest.init.routed.MJsiTgs
import io.suggest.jd.MJdEdgeId
import io.suggest.js.UploadConstants
import io.suggest.model.n2.edge.{MPredicate, MPredicates}
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.IsNodeAdmin
import util.ad.JdAdUtil
import util.cdn.CdnUtil
import views.html.lk.adn.edit._
import japgolly.univeq._
import models.mup.MUploadFileHandlers
import play.api.mvc.Result
import scalaz.ValidationNel
import util.sec.CspUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 15:57
  * Description: Контроллер для react-формы редактирования метаданных ADN-узла.
  * Контроллер заменяет собой MarketLkAdnEdit, который нужен для
  */
class LkAdnEdit @Inject() (
                            isNodeAdmin               : IsNodeAdmin,
                            cspUtil                   : CspUtil,
                            jdAdUtil                  : JdAdUtil,
                            upload                    : Upload,
                            cdnUtil                   : CdnUtil,
                            override val mCommonDi    : ICommonDi
                          )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._

  /** Накатить какие-то дополнительные CSP-политики для работы редактора. */
  private def _applyCspToEditPage(res0: Result): Result = {
    cspUtil.applyCspHdrOpt( cspUtil.CustomPolicies.AdEdit )(res0)
  }


  /** Экшен, возвращающий html-страницу с формой редактирования узла.
    * Начальные параметры
    *
    * @param nodeId id узла.
    * @return Страница, инициализирующая форму размещения узла-ресивера.
    */
  def editNodePage(nodeId: MEsUuId) = csrf.AddToken {
    isNodeAdmin(nodeId, U.Lk).async { implicit request =>
      // Запустить сборку контекста:
      val ctxFut = for (ctxData0 <- request.user.lkCtxDataFut) yield {
        implicit val ctxData1 = ctxData0.withJsiTgs(
          jsiTgs2 = MJsiTgs.LkAdnEditForm :: ctxData0.jsiTgs
        )
        implicitly[Context]
      }

      // Какие предикаты и эджи здесь интересуют?
      val nodeImgPredicates = MPredicates.Logo ::
        MPredicates.WcFgImg ::
        MPredicates.GalleryItem ::
        Nil
      val imgsEdges = jdAdUtil.collectImgEdges(request.mnode.edges, nodeImgPredicates)

      // Запустить сбор данных по интересующим картинкам:
      val imgMediasMapFut = jdAdUtil.prepareImgMedias( imgsEdges )
      val mediaNodesMapFut = jdAdUtil.prepareMediaNodes( imgsEdges, videoEdges = Nil )

      lazy val logPrefix = s"editNodePage($nodeId)#${System.currentTimeMillis()}:"

      val mediaHostsMapFut = for {
        imgMedias  <- imgMediasMapFut
        mediaHosts <- cdnUtil.mediasHosts( imgMedias.values )
      } yield {
        LOGGER.trace(s"$logPrefix For ${imgMedias.size} medias (${imgMedias.keysIterator.mkString(", ")}) found ${mediaHosts.size} media hosts = ${mediaHosts.values.flatMap(_.headOption).map(_.namePublic).mkString(", ")}")
        mediaHosts
      }

      // Скомпилить в jd-эджи собранную инфу, затем завернуть в edit-imgs:
      val jdEdgesFut = for {
        imgMediasMap  <- imgMediasMapFut
        mediaNodesMap <- mediaNodesMapFut
        mediaHostsMap <- mediaHostsMapFut
        ctx           <- ctxFut
      } yield {
        jdAdUtil.mkJdImgEdgesForEdit(
          imgsEdges  = imgsEdges,
          mediasMap  = imgMediasMap,
          mediaNodes = mediaNodesMap,
          mediaHosts = mediaHostsMap
        )(ctx)
      }

      // Синхронно подготовить прочие данные для инициализации:
      val mconf = MAdnEditFormConf(
        nodeId   = request.mnode.id.get
      )
      val mMetaPub = request.mnode.meta.public

      // Собрать данные для инициализации начального состояния формы:
      val formInitStrFut = for {
        jdEdges <- jdEdgesFut
      } yield {
        LOGGER.trace(s"$logPrefix Compiled jd-edges: $jdEdges")
        def __getImgEdge(pred: MPredicate) = {
          jdEdges
            .find(_.predicate ==* pred)
            .map(e => MJdEdgeId(e.id))
        }

        val minit = MAdnEditFormInit(
          conf = mconf,
          form = MAdnEditForm(
            meta  = mMetaPub,
            edges = jdEdges,
            resView = MAdnResView(
              logo = __getImgEdge( MPredicates.Logo ),
              wcFg = __getImgEdge( MPredicates.WcFgImg ),
              galImgs = jdEdges
                .iterator
                .filter(_.predicate ==* MPredicates.GalleryItem)
                // TODO Тут проблема: нужен dynImgArgs, который валяется в исходных эджах или ещё где-то.
                .map(e => MImgEdgeWithOps( MJdEdgeId(e.id) ))
                .toSeq
            )
          )
        )
        // Сериализация состояния в строку:
        Json.toJson(minit)
          .toString()
      }

      // Сгенерить итоговый ответ сервера и ответить наконец:
      for {
        ctx         <- ctxFut
        formInitStr <- formInitStrFut
      } yield {
        val html = nodeEdit2Tpl(
          mnode         = request.mnode,
          formStateStr  = formInitStr
        )(ctx)
        _applyCspToEditPage( Ok(html) )
      }
    }
  }


  /** Экшен получения ссылки для аплоада картинки.
    *
    * @param nodeIdU id узла.
    * @return
    */
  def uploadImg(nodeIdU: MEsUuId) = csrf.Check {
    isNodeAdmin(nodeIdU).async(upload.prepareUploadBp) { implicit request =>
      upload.prepareUploadLogic(
        logPrefix           = s"${getClass.getSimpleName}.uploadImg($nodeIdU):",
        validated           = image4UploadPropsV(request.body),
        uploadFileHandler   = Some( MUploadFileHandlers.Picture ),
        colorDetect         = None
      )
    }
  }

  /** Валидация данных файла, готовящегося к заливке.
    *
    * @param fileProps Присланные клиентом данные по файлу.
    * @return ValidationNel с выверенными данными или ошибкой.
    */
  private def image4UploadPropsV(fileProps: MFile4UpProps): ValidationNel[String, MFile4UpProps] = {
    // Тут практически копипаст данных из LkAdEdit/LkAdEdFormUtil:
    MFile4UpProps.validate(
      m             = fileProps,
      // Бывает, что загружается просто png-рамка, например:
      minSizeB      = 100,
      maxSizeB      = 10*1024*1024,
      mimeVerifierF = { mimeType =>
        MImgFmts.withMime(mimeType).nonEmpty
      },
      mustHashes    = UploadConstants.CleverUp.PICTURE_FILE_HASHES
    )
  }


  /** Экшен сохранения (обновления) узла.
    *
    * @param nodeId id узла.
    */
  def save(nodeId: MEsUuId) = csrf.Check {
    isNodeAdmin(nodeId).async { implicit request =>
      ???
    }
  }

}
