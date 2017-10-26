package controllers

import javax.inject.{Inject, Singleton}

import io.suggest.ad.edit.m.{MAdEditForm, MAdEditFormConf, MAdEditFormInit}
import io.suggest.ad.form.AdFormConstants
import io.suggest.common.empty.OptionUtil
import io.suggest.ctx.CtxData
import io.suggest.err.ErrorConstants
import io.suggest.es.model.MEsUuId
import io.suggest.file.MSrvFileInfo
import io.suggest.init.routed.MJsiTgs
import io.suggest.jd.MJdEditEdge
import io.suggest.model.n2.edge._
import io.suggest.model.n2.extra.MNodeExtras
import io.suggest.model.n2.extra.doc.MNodeDoc
import io.suggest.model.n2.media.MMedias
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.swfs.client.ISwfsClient
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.scalaz.ScalazUtil.Implicits._
import models.im.MImg3
import models.mctx.Context
import models.mproj.ICommonDi
import models.mup.{MColorDetectArgs, MUploadFileHandlers}
import models.req.IReq
import play.api.Mode
import play.api.libs.json.Json
import play.api.mvc._
import util.acl.{BruteForceProtect, CanEditAd, IsNodeAdmin}
import util.ad.LkAdEdFormUtil
import util.img.DynImgUtil
import util.mdr.SysMdrUtil
import util.sec.CspUtil
import util.up.UploadUtil
import util.vid.VideoUtil
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
                           cspUtil                                : CspUtil,
                           lkAdEdFormUtil                         : LkAdEdFormUtil,
                           mMedias                                : MMedias,
                           uploadUtil                             : UploadUtil,
                           uploadCtl                              : Upload,
                           bruteForceProtect                      : BruteForceProtect,
                           dab                                    : DefaultActionBuilder,
                           mNodes                                 : MNodes,
                           swfsClient                             : ISwfsClient,
                           sysMdrUtil                             : SysMdrUtil,
                           videoUtil                              : VideoUtil,
                           override val mCommonDi                 : ICommonDi
                         )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._

  private lazy val _BFP_ARGS = bruteForceProtect.ARGS_DFLT.withTryCountDivisor(2)

  /** Новые карточки ломают слишком много, поэтому временно сохранение ограничено только в dev-режиме. */
  private def IS_SAVING_ALLOWED = current.mode == Mode.Dev

  /** Накатить какие-то дополнительные CSP-политики для работы редактора. */
  private def _applyCspToEditPage(res0: Result): Result = {
    cspUtil.applyCspHdrOpt( cspUtil.CustomPolicies.AdEdit )(res0)
  }


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
      val formConfFut = for (ctx <- ctxFut) yield {
        MAdEditFormConf(
          producerId  = producerId,
          adId        = None,
          ctxId    = ctx.ctxIdStr
        )
      }

      // Собрать модель инициализации формы редактора
      val formInitJsonStrFut0 = for {
        docForm  <- docFormFut
        formConf <- formConfFut
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

        // Вернуть ответ с коррективами в CSP-политике.
        _applyCspToEditPage(
          Ok( html )
        )
      }
    }
  }


  /** Экшен сабмита формы создания новой карточки.
    *
    * @param producerIdU id узла-продьюсера, в рамках которого создаётся карточка.
    * @return 201 Created + init-форма с обновлённой карточкой и данными.
    */
  def createAdSubmit(producerIdU: MEsUuId) = bruteForceProtect(_BFP_ARGS) {
    val producerId = producerIdU.id
    csrf.AddToken {
      isNodeAdmin(producerId).async( parse.json[MAdEditForm] ) { implicit request =>
        // Взять форму из реквеста, провалидировать
        lazy val logPrefix = s"createAdSubmit($producerId):"
        lkAdEdFormUtil.earlyValidateEdges( request.body ).fold(
          // Не удалось понять присланные эджи:
          {errorsNel =>
            LOGGER.warn(s"$logPrefix Failed to validate remote edges: ${errorsNel.iterator.mkString(", ")}")
            NotAcceptable("edges")
          },

          // Есть проверенные эджи, похожие на валидные. Надо заняться валидацией самого шаблона.
          {edges2 =>
            // Собрать данные по всем упомянутым в запросе узлам, не обрывая связь с исходными эджами.
            val nodeId2edgesMap = (
              for {
                jdEdge  <- edges2.iterator
                fileSrv <- jdEdge.fileSrv
              } yield {
                fileSrv.nodeId -> jdEdge
              }
            )
              .toSeq
              .groupBy(_._1)
              .mapValues(_.map(_._2))

            // Поискать узлы, упомянутые в этих эджах.
            val edgedNodesMapFut = mNodesCache.multiGetMap( nodeId2edgesMap.keys )

            // Для валидации самого шаблона нужны данные по размерам связанных картинок. Поэтому залезаем в MMedia за оригиналами упомянутых картинок:
            val imgNeededMap = lkAdEdFormUtil.collectNeededImgs( edges2 )
            val imgsMediasFut = mMedias.multiGetMap {
              imgNeededMap
                .mapValues(_.mediaId)
                .valuesIterator
                .toSet
            }

            // Когда будут собраны данные, произвести валидацию шаблона:
            val vldResFut = for {
              imgsMediasMap <- imgsMediasFut
              edgedNodesMap <- edgedNodesMapFut
            } yield {
              lkAdEdFormUtil.validateTpl(
                template      = request.body.template,
                jdEdges       = edges2,
                imgsNeededMap = imgNeededMap,
                nodesMap      = edgedNodesMap,
                mediasMap     = imgsMediasMap
              )
            }

            // Дождаться результата валидации...
            vldResFut.flatMap { vldRes =>
              vldRes.fold(
                {failedMsgs =>
                  val msgsConcat = failedMsgs.iterator.mkString(", ")
                  LOGGER.warn(s"$logPrefix Unable to validate template: $msgsConcat")
                  NotAcceptable( s"tpl: $msgsConcat" )
                },
                {tpl2 =>
                  // Есть на руках валидный шаблон. Можно создавать новую карточку.
                  ErrorConstants.assertArg( IS_SAVING_ALLOWED )

                  val videoPred = MPredicates.JdContent.Video
                  // Сграбить ссылки на внешнее видео -- для них надо создать videoExt-узлы, этим занимается VideoUtil:
                  val videoExtEdgesFut = {
                    val videoExtUrls = (
                      for {
                        jdEdge <- edges2.iterator
                        if jdEdge.predicate ==>> videoPred
                        url    <- jdEdge.url
                      } yield {
                        url -> jdEdge.id
                      }
                    ).toMap
                    videoUtil.ensureExtVideoNodes(videoExtUrls.keys, request.user.personIdOpt)
                  }

                  val mnode0Fut = for {
                    //edgedNodesMap <- edgedNodesMapFut
                    videoExtEdges <- videoExtEdgesFut
                  } yield {
                    MNode(
                      common = MNodeCommon(
                        ntype       = MNodeTypes.Ad,
                        isDependent = true
                      ),
                      extras  = MNodeExtras(
                        doc = Some(MNodeDoc(
                          template = tpl2
                        ))
                      ),
                      edges = MNodeEdges(
                        out = {
                          // Нужно собрать в кучу все эджи: producer, creator, эджи от связанных узлов (картинок/файлов) итд.
                          // Пропихиваем jd-эджи:
                          var edgesAcc = edges2.foldLeft(List.empty[MEdge]) { (acc0, jdEdge) =>
                            MEdge(
                              predicate = jdEdge.predicate,
                              nodeIds = {
                                val videoNodeIdOpt = OptionUtil.maybeOpt(jdEdge.predicate ==>> videoPred) {
                                  jdEdge.url
                                    .flatMap( videoExtEdges.get )
                                    .flatMap( _.id )
                                }
                                videoNodeIdOpt
                                  .orElse {
                                    jdEdge.fileSrv.map(_.nodeId)
                                  }
                                  .iterator
                                  .toSet
                              },
                              doc = MEdgeDoc(
                                uid   = Some(jdEdge.id),
                                text  = jdEdge.text.toSeq
                              )
                            ) :: acc0
                          }

                          // Собираем эдж до текущего узла-продьюсера:
                          edgesAcc ::= MEdge(
                            predicate = MPredicates.OwnedBy,
                            nodeIds   = request.mnode.id.toSet
                          )

                          // Собираем эдж до текущего узла юзера-создателя:
                          edgesAcc ::= MEdge(
                            predicate = MPredicates.CreatedBy,
                            nodeIds   = request.user.personIdOpt.toSet
                          )

                          // Помечаем, как отмодерированный, если текущий юзер -- это супер-юзер.
                          if (request.user.isSuper)
                            edgesAcc ::= sysMdrUtil.mdrEdge()

                          edgesAcc
                        }
                      )
                    )
                  }

                  // Сохранить карточку и вернуть результат.
                  for {
                    mnode0 <- mnode0Fut
                    adId   <- mNodes.save( mnode0 )
                  } yield {
                    implicit val ctx = implicitly[Context]

                    // Сохранено успешно. Вернуть ответ в виде обновлённой формы.
                    val formReInit2 =MAdEditFormInit(
                      conf = MAdEditFormConf(
                        producerId  = request.mnode.id.get,
                        adId        = Some(adId),
                        ctxId       = ctx.ctxIdStr
                      ),
                      form = MAdEditForm(
                        template  = tpl2,
                        edges     = edges2
                      )
                    )

                    Created( Json.toJson(formReInit2) )
                  }
                }
              )
            }
          }
        )
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
      val ctxFut = ctxData1Fut.map { implicit ctxData0 =>
        implicitly[Context]
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
          url         = Some( dynImgUtil.imgCall(mimg).url ),
          fileSrv     = Some(MSrvFileInfo(
            nodeId = nodeId,
            url = Some("TODO.need.url")   // TODO Генерить тут нормальную ссылку на картинку для редактора.
            // TODO Другие поля, надо извлечь их из MMedia.
          ))
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
          url       = None
        )
      }

      val edEdges = Iterator(imgEdgesIter, textEdgesIter)
        .flatten
        .toSeq

      val nodeDoc = request.mad.extras.doc.get    // TODO .getOrElse(defaultDoc)

      // Собрать модель и отрендерить:
      val formInitStrFut = for {
        ctx <- ctxFut
      } yield {
        val formInit = MAdEditFormInit(
          conf = MAdEditFormConf(
            producerId  = request.producer.id.get,
            adId        = request.mad.id,
            ctxId       = ctx.ctxIdStr
          ),
          form = MAdEditForm(
            template  = nodeDoc.template,
            edges     = edEdges
          )
        )
        _formInit2str( formInit )
      }

      // Дождаться готовности фоновых операций и отрендерить результат.
      for {
        ctx         <- ctxFut
        formInitStr <- formInitStrFut
      } yield {
        // Вернуть HTTP-ответ, т.е. страницу с формой.
        val html = adEditTpl(
          mad     = Some(request.mad),
          parent  = request.producer,
          state0  = formInitStr
        )(ctx)

        _applyCspToEditPage(
          Ok(html)
        )
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



  /** Экшен подготовки к загрузке файла на сервер.
    *
    * Подразумевается POST-запрос, потому что:
    * - csrf.Check
    * - Запрос немного влияет (или может влиять) на состояние сервера
    * - Содержит JSON-тело с описанием загружаемого файла.
    *
    * @param adIdU id рекламной карточки для которой подготавливается загрузка файла.
    *              None, если происходит создание новой карточки.
    * @param nodeIdU id текущего узла-продьюсера, когда не задан id редактируемой карточки.
    *                None, если задан id карточки.
    * @return JSON-ответ.
    */
  def prepareImgUpload(adIdU: Option[MEsUuId], nodeIdU: Option[MEsUuId]) = csrf.Check {
    lazy val logPrefix = s"prepareUpload(${adIdU.orElse(nodeIdU).orNull})#${System.currentTimeMillis()}:"
    val bp = uploadCtl.prepareUploadBp

    // Нельзя, чтобы было два Some или оба None.
    if (adIdU.isEmpty == nodeIdU.isEmpty) {
      dab(bp) { implicit request =>
        val msg = "Exact one arg expected"
        LOGGER.warn(s"$logPrefix $msg adId=$adIdU, nodeId=$nodeIdU, body=${request.body}")
        PreconditionFailed(msg)
      }

    } else {
      val ab = adIdU
        .map[ActionBuilder[IReq, AnyContent]] { canEditAd(_) }
        .getOrElse {
          isNodeAdmin( nodeIdU.get.id )
        }

      // TODO Использовать какой-нибудь canUploadFile или canUploadFile4Ad.
      ab.async(bp) { implicit request =>
        val validated = lkAdEdFormUtil.image4UploadPropsV( request.body )

        // И просто запустить API-метод prepareUpload() из Upload-контроллера.
        uploadCtl.prepareUploadLogic(
          logPrefix = logPrefix,
          validated = validated,
          // Сразу отправлять принятый файл в MLocalImg минуя /tmp/.
          uploadFileHandler = Some( MUploadFileHandlers.Picture ),
          colorDetect       = Some {
            val cdConst = AdFormConstants.ColorDetect
            val sz = cdConst.PALETTE_SIZE
            MColorDetectArgs(
              paletteSize   = sz,
              wsPaletteSize = sz  // cdConst.PALETTE_SHRINK_SIZE
            )
          }
        )
      }
    }
  }

}
