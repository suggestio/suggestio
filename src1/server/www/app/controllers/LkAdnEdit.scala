package controllers

import io.suggest.adn.edit.m.{MAdnEditForm, MAdnEditFormConf, MAdnEditFormInit}
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.file.up.MFile4UpProps
import io.suggest.img.MImgFmts
import io.suggest.init.routed.MJsInitTargets
import io.suggest.js.UploadConstants
import io.suggest.model.n2.edge._
import io.suggest.model.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.{BruteForceProtect, IsNodeAdmin}
import util.ad.JdAdUtil
import util.cdn.CdnUtil
import views.html.lk.adn.edit._
import models.mup.MUploadFileHandlers
import play.api.mvc.Result
import scalaz.ValidationNel
import util.n2u.N2VldUtil
import util.sec.CspUtil
import io.suggest.scalaz.ScalazUtil.Implicits._
import models.req.MNodeReq

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 15:57
  * Description: Контроллер для react-формы редактирования метаданных ADN-узла.
  * Контроллер заменяет собой MarketLkAdnEdit, который нужен для
  */
@Singleton
class LkAdnEdit @Inject() (
                            esModel                   : EsModel,
                            isNodeAdmin               : IsNodeAdmin,
                            cspUtil                   : CspUtil,
                            n2VldUtil                 : N2VldUtil,
                            jdAdUtil                  : JdAdUtil,
                            mNodes                    : MNodes,
                            bruteForceProtect         : BruteForceProtect,
                            upload                    : Upload,
                            cdnUtil                   : CdnUtil,
                            sioControllerApi          : SioControllerApi,
                            mCommonDi                 : ICommonDi,
                          )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import mCommonDi._
  import esModel.api._

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
      val ctxFut = for {
        ctxData0 <- request.user.lkCtxDataFut
      } yield {
        implicit val ctxData1 = ctxData0.withJsInitTargets(
          jsInitTargets = MJsInitTargets.LkAdnEditForm :: ctxData0.jsInitTargets
        )
        implicitly[Context]
      }

      val mformFut = _mkForm( Some(ctxFut) )

      // Синхронно подготовить прочие данные для инициализации:
      val mconf = MAdnEditFormConf(
        nodeId = request.mnode.id.get
      )

      // Сгенерить итоговый ответ сервера и ответить наконец:
      for {
        ctx         <- ctxFut
        mform       <- mformFut
      } yield {
        // Собрать данные для инициализации начального состояния формы:
        val minit = MAdnEditFormInit(
          conf = mconf,
          form = mform
        )
        // Сериализация состояния в строку:
        val formInitStr = Json
          .toJson(minit)
          .toString()

        val html = nodeEdit2Tpl(
          mnode         = request.mnode,
          formStateStr  = formInitStr
        )(ctx)

        _applyCspToEditPage( Ok(html) )
      }
    }
  }


  /** Сборка данных формы редактора.
    *
    * @param ctxFutOpt Данные контекста рендера.
    * @param request HTTP-реквест.
    * @return Фьючерс с инстансом формы.
    */
  private def _mkForm(ctxFutOpt: Option[Future[Context]] = None)
                     (implicit request: MNodeReq[_]): Future[MAdnEditForm] = {
    // Запустить сборку контекста, если ещё не запущен:
    val ctxFut = ctxFutOpt
      .getOrElse( Future.successful(getContext2) )

    val imgEdgesJdIds = request.mnode.extras
      .adnEdgeUids
      .toStream

    // Какие предикаты и эджи здесь интересуют?
    val imgEdgeUids = imgEdgesJdIds
      .iterator
      .map(e => e.edgeUid -> e)
      .toMap
    val imgEdges = request.mnode
      .edges
      .withUid1(imgEdgeUids.keys)

    val imgsEdges = jdAdUtil.collectImgEdges( imgEdges, imgEdgeUids )

    // Запустить сбор данных по интересующим картинкам:
    val imgMediasMapFut = jdAdUtil.prepareImgMedias( imgsEdges )
    val mediaNodesMapFut = jdAdUtil.prepareMediaNodes( imgsEdges, videoEdges = Nil )

    lazy val logPrefix = s"_mkForm(${request.mnode.idOrNull})#${System.currentTimeMillis()}:"

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

    // Подготовить метаданные узла:
    val mMetaPub = request.mnode.meta.public

    // Собрать класс с формой
    for {
      jdEdges <- jdEdgesFut
    } yield {
      LOGGER.trace(s"$logPrefix Compiled jd-edges: $jdEdges")
      MAdnEditForm(
        meta    = mMetaPub,
        edges   = jdEdges,
        // TODO Тут безопасно ли делать .get? По идее, этот метод может быть вызван и для карточки...
        resView = request.mnode.extras.adn.get.resView
      )
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
      // Теоретически, может загружаться очень тривиальный svg-логотип:
      minSizeB      = 200,
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
    bruteForceProtect {
      isNodeAdmin(nodeId).async( parse.json[MAdnEditForm] ) { implicit request =>
        lazy val logPrefix = s"save($nodeId)#${System.currentTimeMillis()}:"

        // Для прочистки карты эджей, надо узнать все пустующие эджи:
        val usedEdgeIds = request.body.usedEdgeUidsSet
        val jdEdges0 = EdgesUtil.purgeUnusedEdgesFrom(usedEdgeIds, request.body.edges)

        // Надо распарсенный реквест провалидировать.
        n2VldUtil.earlyValidateEdges( jdEdges0 ).fold(
          // Ошибка базовой проверки эджей.
          {errors =>
            val msg = errors.iterator.mkString("\n")
            LOGGER.debug(s"$logPrefix Failed to validate form edges:\n$msg")
            errorHandler.onClientError(request, NOT_ACCEPTABLE, msg)
          },
          // Всё ок, переходим к дальнейшим асинхронным проверкам:
          {edges2 =>
            val evld = n2VldUtil.EdgesValidator( edges2 )

            evld.vldEdgesMapFut.flatMap { vldEdgesMap =>
              // Произвести полную валидацию присланных данных:
              MAdnEditForm.validate( request.body, vldEdgesMap ).fold(
                {errors =>
                  val msg = errors.iterator.mkString("\n")
                  LOGGER.debug(s"$logPrefix Failed to validate form: \n$msg")
                  errorHandler.onClientError(request, NOT_ACCEPTABLE, msg)
                },
                {form =>
                  // Все данные проверены, перейти к апдейту данных узла.
                  LOGGER.trace(s"$logPrefix Form validated ok. Will update node.\n $form")

                  // Конвертация jd-эджей в MEdge-представление. Коллекция (не одноразовая!).
                  val addNodeEdges = for (jdEdge <- form.edges) yield {
                    MEdge(
                      predicate = jdEdge.predicate,
                      nodeIds = jdEdge.fileSrv
                        .map(_.nodeId)
                        .toSet,
                      doc = MEdgeDoc(
                        uid = Some( jdEdge.id )
                      )
                    )
                  }

                  // Запуск обновления.
                  val saveFut = mNodes.tryUpdate(request.mnode) { mnode0 =>
                    mnode0.copy(
                      edges = mnode0.edges
                        // Стираем все jd-эджи. На момент описания результат был эквивалентен
                        .withoutPredicate( MPredicates.JdContent )
                        .withEdges( addNodeEdges ),
                      extras = mnode0.extras.withAdn(Some(
                        mnode0.extras.adn.get
                          .withResView( form.resView )
                      )),
                      meta = mnode0.meta
                        .withPublic( form.meta )
                    )
                  }

                  // Параллельно собрать json-ответ с провалидированной формой: для этого нужно пересобрать новые эджи.
                  // Тут всё ок, но в эджах нет достаточных данных в url или fileSrv. Это отрабатываетя на клиенте.
                  val respFormJson = Json.toJson( form )

                  // Дождаться завершения, собрать ответ с обновлённой формой.
                  for (_ <- saveFut) yield {
                    LOGGER.trace(s"$logPrefix Node updated ok.")
                    Ok( respFormJson )
                  }
                }
              )
            }
          }
        )
      }
    }
  }

}
