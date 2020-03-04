package controllers

import io.suggest.adn.edit.m.{MAdnEditForm, MAdnEditFormConf, MAdnEditFormInit}
import io.suggest.ctx.CtxData
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.jd.MJdEdge
import io.suggest.n2.edge._
import io.suggest.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.n2.media.MFileMeta
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.pick.ContentTypeCheck
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.{BruteForceProtect, IsNodeAdmin}
import util.ad.JdAdUtil
import util.cdn.CdnUtil
import views.html.lk.adn.edit._
import models.mup.{MUploadFileHandlers, MUploadInfoQs}
import play.api.mvc.Result
import scalaz.ValidationNel
import util.n2u.N2VldUtil
import util.sec.CspUtil
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.up.UploadConstants
import models.req.MNodeReq
import monocle.Traversal
import scalaz.std.option._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 15:57
  * Description: Контроллер для react-формы редактирования метаданных ADN-узла.
  * Контроллер заменяет собой MarketLkAdnEdit, который нужен для
  */
final class LkAdnEdit @Inject() (
                                  sioControllerApi          : SioControllerApi,
                                  mCommonDi                 : ICommonDi,
                                )
  extends MacroLogsImpl
{
  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val cspUtil = injector.instanceOf[CspUtil]
  private lazy val n2VldUtil = injector.instanceOf[N2VldUtil]
  private lazy val jdAdUtil = injector.instanceOf[JdAdUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]
  private lazy val upload = injector.instanceOf[Upload]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]


  import sioControllerApi._
  import mCommonDi.{ec, csrf, errorHandler}


  /** Накатить какие-то дополнительные CSP-политики для работы редактора. */
  private def _applyCspToEditPage(res0: Result): Result = {
    import cspUtil.Implicits._
    res0.withCspHeader( cspUtil.CustomPolicies.AdEdit )
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
        implicit val ctxData1 = CtxData.jsInitTargetsAppendOne( MJsInitTargets.LkAdnEditForm )(ctxData0)
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

    lazy val logPrefix = s"_mkForm(${request.mnode.idOrNull})#${System.currentTimeMillis()}:"

    val imgEdgeIds = request.mnode.extras
      .adnEdgeUidsIter
      .to(LazyList)

    // Если есть инфа по img-эджам - собрать инфу:
    val jdEdgesFut: Future[List[MJdEdge]] = {
      if (imgEdgeIds.isEmpty) {
        Future.successful( Nil )

      } else {
        // Какие предикаты и эджи здесь интересуют?
        val imgEdgeUids = request.mnode.extras
          .adnEdgeUidsIter
          .map(e => e.edgeUid -> e)
          .toMap
        val imgEdges = request.mnode
          .edges
          .withUid1( imgEdgeUids.keySet )

        val imgsEdges = jdAdUtil.collectImgEdges( imgEdges, imgEdgeUids )

        // Запустить сбор данных по интересующим картинкам:
        val mediaNodesMapFut = jdAdUtil.prepareMediaNodes( imgsEdges, videoEdges = Nil )

        val mediaHostsMapFut = for {
          imgMedias  <- mediaNodesMapFut
          mediaHosts <- cdnUtil.mediasHosts( imgMedias.values )
        } yield {
          LOGGER.trace(s"$logPrefix For ${imgMedias.size} medias (${imgMedias.keysIterator.mkString(", ")}) found ${mediaHosts.size} media hosts = ${mediaHosts.values.flatMap(_.headOption).map(_.namePublic).mkString(", ")}")
          mediaHosts
        }

        // Скомпилить в jd-эджи собранную инфу, затем завернуть в edit-imgs:
        for {
          mediaNodesMap <- mediaNodesMapFut
          mediaHostsMap <- mediaHostsMapFut
          ctx           <- ctxFut
        } yield {
          jdAdUtil.mkJdImgEdgesForEdit(
            imgsEdges  = imgsEdges,
            mediaNodes = mediaNodesMap,
            mediaHosts = mediaHostsMap
          )(ctx)
        }
      }
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
    bruteForceProtect {
      isNodeAdmin(nodeIdU).async(upload.prepareUploadBp) { implicit request =>
        upload.prepareUploadLogic(
          logPrefix           = s"${getClass.getSimpleName}.uploadImg($nodeIdU):",
          validated           = image4UploadPropsV(request.body),
          upInfo = MUploadInfoQs(
            fileHandler     = Some( MUploadFileHandlers.Image ),
            colorDetect     = None,
            nodeType        = Some( MNodeTypes.Media.Image ),
          ),
        )
      }
    }
  }

  /** Валидация данных файла, готовящегося к заливке.
    *
    * @param fileProps Присланные клиентом данные по файлу.
    * @return ValidationNel с выверенными данными или ошибкой.
    */
  private def image4UploadPropsV(fileProps: MFileMeta): ValidationNel[String, MFileMeta] = {
    // Тут практически копипаст данных из LkAdEdit/LkAdEdFormUtil:
    MFileMeta.validateUpload(
      m             = fileProps,
      // Теоретически, может загружаться очень тривиальный svg-логотип:
      minSizeB      = 200,
      maxSizeB      = 10*1024*1024,
      mimeVerifierF = ContentTypeCheck.OnlyImages,
      mustHashes    = UploadConstants.CleverUp.UPLOAD_FILE_HASHES
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
        val jdEdges0 = EdgesUtil.purgeUnusedEdgesFrom(
          usedEdgeIds = request.body
            .resView
            .edgeUids
            .iterator
            .map(_.edgeUid)
            .toSet,
          edges = request.body.edges
        )

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
            import esModel.api._

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
                        uid = jdEdge.id
                      )
                    )
                  }

                  // Запуск обновления.
                  val saveFut = mNodes.tryUpdate( request.mnode )(
                    MNode.edges.modify { edges0 =>
                      // Стираем все jd-эджи. На момент описания результат был эквивалентен
                      MNodeEdges.out.modify(_ ++ addNodeEdges)(
                        edges0
                          .withoutPredicate( MPredicates.JdContent )
                      )
                    } andThen
                    MNode.extras
                      .composeLens( MNodeExtras.adn )
                      .composeTraversal( Traversal.fromTraverse[Option, MAdnExtra] )
                      .composeLens( MAdnExtra.resView )
                      .set( form.resView ) andThen
                    MNode.meta
                      .modify( _.withPublic(form.meta) )
                  )

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
