package controllers

import javax.inject.{Inject, Singleton}

import io.suggest.ad.edit.m.{MAdEditForm, MAdEditFormConf, MAdEditFormInit}
import io.suggest.ad.form.AdFormConstants
import io.suggest.color.MHistogram
import io.suggest.common.empty.OptionUtil
import io.suggest.ctx.CtxData
import io.suggest.es.model.MEsUuId
import io.suggest.file.MSrvFileInfo
import io.suggest.init.routed.MJsiTgs
import io.suggest.jd.MJdEditEdge
import io.suggest.model.n2.ad.MNodeAd
import io.suggest.model.n2.edge._
import io.suggest.model.n2.extra.MNodeExtras
import io.suggest.model.n2.extra.doc.MNodeDoc
import io.suggest.model.n2.media.{MFileMetaHash, MMedias, MMediasCache}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.util.logs.MacroLogsImpl
import models.im._
import models.mctx.Context
import models.mproj.ICommonDi
import models.mup.{MColorDetectArgs, MUploadFileHandlers}
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc._
import util.acl.{BruteForceProtect, CanCreateOrEditAd, CanEditAd, IsNodeAdmin}
import util.ad.LkAdEdFormUtil
import util.cdn.{CdnUtil, DistUtil}
import util.img.DynImgUtil
import util.mdr.SysMdrUtil
import util.sec.CspUtil
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
                           canEditAd                              : CanEditAd,
                           canCreateOrEditAd                      : CanCreateOrEditAd,
                           dynImgUtil                             : DynImgUtil,
                           isNodeAdmin                            : IsNodeAdmin,
                           cspUtil                                : CspUtil,
                           lkAdEdFormUtil                         : LkAdEdFormUtil,
                           mMedias                                : MMedias,
                           uploadCtl                              : Upload,
                           bruteForceProtect                      : BruteForceProtect,
                           distUtil                               : DistUtil,
                           mMediasCache                           : MMediasCache,
                           mNodes                                 : MNodes,
                           cdnUtil                                : CdnUtil,
                           sysMdrUtil                             : SysMdrUtil,
                           videoUtil                              : VideoUtil,
                           override val mCommonDi                 : ICommonDi
                         )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._

  private lazy val _BFP_ARGS = bruteForceProtect.ARGS_DFLT.withTryCountDivisor(2)

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
          ctxId       = ctx.ctxIdStr
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
  def saveAdSubmit(adIdOptU: Option[MEsUuId], producerIdU: Option[MEsUuId]) = bruteForceProtect(_BFP_ARGS) {
    csrf.Check {
      canCreateOrEditAd(adIdOptU, producerIdOpt = producerIdU).async( parse.json[MAdEditForm] ) { implicit request =>
        // Взять форму из реквеста, провалидировать
        lazy val logPrefix = s"saveAdSubmit(${request.madOpt.fold("pro")(_ => "a")}d#${adIdOptU.orElse(producerIdU).orNull}):"

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
                // Не удалось провалидировать шаблон.
                {failedMsgs =>
                  val msgsConcat = failedMsgs.iterator.mkString(", ")
                  LOGGER.warn(s"$logPrefix Unable to validate template: $msgsConcat")
                  NotAcceptable( s"tpl: $msgsConcat" )
                },

                // Есть на руках валидный шаблон. Можно создавать новую карточку.
                {tpl2 =>
                  LOGGER.trace(s"$logPrefix Successfully validated template:\n${tpl2.drawTree}")

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

                  val edgesAcc0Fut = for {
                    videoExtEdges <- videoExtEdgesFut
                  } yield {
                    LOGGER.trace(s"$logPrefix ${videoExtEdges.size} VideoExtEdges = [${videoExtEdges.mapValues(_.idOrNull).mkString(", ")}]")

                    var _acc0 = edges2.foldLeft(List.empty[MEdge]) { (acc0, jdEdge) =>
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

                    // Помечаем, как отмодерированный, если текущий юзер -- это супер-юзер.
                    if (request.user.isSuper)
                      _acc0 ::= sysMdrUtil.mdrEdge()

                    _acc0
                  }

                  // Создать/обновить строковое название для узла карточки (на основе текстовых эджей)
                  val edges2Map = lkAdEdFormUtil.mkEdgeTextsMap( edges2 )

                  val nodeTechNameOpt = lkAdEdFormUtil.mkTechName(tpl2, edges2Map)
                  LOGGER.trace(s"$logPrefix nodeTechName = ${nodeTechNameOpt.orNull}")

                  // В зависимости от создания или редактирования карточки, делаем тот или иной апдейт.
                  val madSavedFut = edgesAcc0Fut.flatMap { edgesAcc0 =>
                    LOGGER.trace(s"$logPrefix JdEE->MEdge edges: [${edgesAcc0.mkString(", ")}]")

                    request.madOpt.fold[Future[MNode]] {
                      LOGGER.trace(s"$logPrefix Creating new ad...")
                      // Создание новой карточки. Инициализировать новую карточку.
                      val mad0 = MNode(
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
                            var edgesAcc = edgesAcc0
                            // Собираем эдж до текущего узла-продьюсера:
                            edgesAcc ::= MEdge(
                              predicate = MPredicates.OwnedBy,
                              nodeIds   = Set( request.producer.id.get )
                            )

                            // Собираем эдж до текущего узла юзера-создателя:
                            edgesAcc ::= MEdge(
                              predicate = MPredicates.CreatedBy,
                              nodeIds   = request.user.personIdOpt.toSet
                            )

                            edgesAcc
                          }
                        ),
                        meta = MMeta(
                          basic = MBasicMeta(
                            techName = nodeTechNameOpt
                          )
                        )
                      )
                      for (adId <- mNodes.save( mad0 )) yield {
                        LOGGER.trace(s"$logPrefix Created new ad#$adId")
                        mad0.withId( Some(adId) )
                      }
                    } { mad00 =>
                      LOGGER.trace(s"$logPrefix Will update existing ad: ${mad00.idOrNull}")
                      if (mad00.ad.nonEmpty)
                        LOGGER.info(s"$logPrefix Erasing old ad format data: ${mad00.ad}")
                      if (mad00.extras.doc.isEmpty)
                        LOGGER.info(s"$logPrefix Initialized new jd-template, previous ad template was empty.")

                      // Сохраняемая карточка уже существует: перезаписать в ней некоторые эджи.
                      mNodes.tryUpdate(mad00) { mad =>
                        mad.copy(
                          // Залить новые эджи:
                          edges = mad.edges.copy(
                            out = {
                              // Убрать все существующие jd-content-эджи. ТODO Bg-предикат: удалить старый предикат фона (старый формат market ad).
                              val edgesCleanIter = mad.edges
                                .withoutPredicateIter( MPredicates.JdContent, MPredicates.Bg )
                              // Добавить новые jd-эджи.
                              MNodeEdges.edgesToMap1( edgesCleanIter ++ edgesAcc0 )
                            }
                          ),
                          // Удалить данные старой карточки.
                          ad = MNodeAd.empty,
                          // Залить новый шаблон:
                          extras = mad.extras.withDoc {
                            Some(
                              mad.extras.doc
                                .fold(MNodeDoc(template = tpl2)) { _.withTemplate(tpl2) }
                            )
                          },
                          meta = mad.meta.withBasic {
                            mad.meta.basic
                              .withTechName( nodeTechNameOpt )
                          }
                        )
                      }
                    }
                  }

                  // Сохранить карточку и вернуть результат.
                  for {
                    mad2 <- madSavedFut
                  } yield {
                    LOGGER.trace(s"$logPrefix Saved ok, ad#${mad2.idOrNull}")
                    implicit val ctx = implicitly[Context]

                    // Сохранено успешно. Вернуть ответ в виде обновлённой формы.
                    val formReInit2 = MAdEditFormInit(
                      conf = MAdEditFormConf(
                        producerId  = request.producer.id.get,
                        adId        = mad2.id,
                        ctxId       = ctx.ctxIdStr
                      ),
                      form = MAdEditForm(
                        template  = tpl2,
                        edges     = edges2
                      )
                    )

                    Ok( Json.toJson(formReInit2) )
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
      lazy val logPrefix = s"editAd($adId):"

      // Нужно собрать начальное состояние формы. Для этого нужно собрать ресурсы текущей карточки.
      val imgPredicate = MPredicates.JdContent.Image

      // Собираем картинки, используемые в карточке:
      val imgsEdges = request.mad.edges
        .withPredicateIter( imgPredicate, MPredicates.Bg )  // TODO Надо ли тут Bg-предикат? по идее, он устарел ещё до ввода jd-редактора.
        .map { medge =>
          medge -> MImg3(medge)
        }
        .toSeq
      LOGGER.trace(s"$logPrefix Found ${imgsEdges.size} img.edges: ${imgsEdges.iterator.map(_._2.fileName).mkString(", ")}")

      // Собрать связанные инстансы MMedia
      val imgOrigsMediasMapFut = mMediasCache.multiGetMap {
        imgsEdges
          .iterator
          .map(_._2.mediaId)
          .toSet
      }

      val videoPredicate = MPredicates.JdContent.Video
      // Собрать video-эджи. Для них надо получить инстансы MNode, чтобы достучаться до ссылок.
      val videoEdges = request.mad.edges
        .withPredicateIter( videoPredicate )
        .toSeq
      LOGGER.trace(s"$logPrefix Found ${videoEdges.size} video edges: ${videoEdges.mkString(", ")}")

      // Для имён файлов нужно собрать сами узлы.
      val mediaNodesMapFut = mNodesCache.multiGetMap {
        // Перечисляем все интересующие img-ноды:
        val imgNodeIdsIter = imgsEdges
          .iterator
          .map(_._1)
        // Присунуть сюда же video-ноды:
        val videoNodeIdsIter = videoEdges
          .iterator
        // Всё объеденить и дедублицировать.
        (imgNodeIdsIter ++ videoNodeIdsIter)
          .flatMap(_.nodeIds)
          .toSet
      }

      // Собрать инфу по хостам, хранящим интересующие media-файлы.
      val imgMedia2hostsMapFut = imgOrigsMediasMapFut.flatMap { imgOrigsMediasMap =>
        LOGGER.trace(s"$logPrefix Found ${imgOrigsMediasMap.size} linked img medias.")
        distUtil.medias2hosts( imgOrigsMediasMap.values )
      }

      // Скомпилить jd-эджи картинок.
      val imgJdEdgesFut = for {
        mediasMap           <- imgOrigsMediasMapFut
        mediaNodesMap       <- mediaNodesMapFut
        imgMedia2hostsMap   <- imgMedia2hostsMapFut
      } yield {
        // Получены медиа-файлы на руки.
        val iter = for {
          // Пройти по BgImg-эджам карточки:
          (medge, mimg)   <- imgsEdges.iterator
          // id узла эджа -- это идентификатор картинки.
          edgeUid         <- medge.doc.uid.iterator
          nodeId          <- medge.nodeIds.iterator
          mmedia          <- mediasMap.get(mimg.mediaId).iterator
        } yield {
          // Получить инфу по хосту, на котором хранится данная картинка.
          lazy val hostInfoOpt = imgMedia2hostsMap
            .get( nodeId )
            .flatMap(_.headOption)
          def __imgUrl(mimg9: MImgT): String = {
            val call = dynImgUtil.imgCall(mimg9)
            hostInfoOpt
              .fold[String] {
                // Вообще, это плохо, если нет хостнейма для media. Это значит, что-то не так.
                LOGGER.warn(s"$logPrefix Media host-name missing for $nodeId")
                call.url
              } { hostInfo =>
                LOGGER.trace(s"$logPrefix Using host=$hostInfo for media-node#$nodeId")
                cdnUtil.distNodeCdnUrl( hostInfo, call)
              }
          }

          val jdEdge = MJdEditEdge(
            predicate = imgPredicate,
            id        = edgeUid,
            // url не ставим, потому что очень нужен около-оригинальная картинка, для кропа например.
            fileSrv   = Some(MSrvFileInfo(
              nodeId    = nodeId,
              url       = Some {
                // TODO Вместо сырого оригинала вернуть нечто пересжатое с тем же w/h.
                __imgUrl( mimg )
              },
              // TODO Дальше модель сильно дублирует модель в MMedia.file (без учёта date_created).
              sizeB     = Some( mmedia.file.sizeB ),
              mimeType  = Some( mmedia.file.mime ),
              hashesHex = MFileMetaHash.toHashesHex( mmedia.file.hashesHex ),
              colors    = for (_ <- mmedia.picture.colors.headOption) yield {
                MHistogram( mmedia.picture.colors )
              },
              name = mediaNodesMap.get( nodeId )
                .flatMap(_.guessDisplayName),
              whPx = mmedia.picture.whPx
            ))
          )

          LOGGER.trace(s"$logPrefix Img edge compiled: $jdEdge")
          jdEdge
        }
        iter.toSeq
      }

      // Скомпилить jd-эджи для видосов:
      val videoJdEdgesFut = for {
        mediaNodesMap   <- mediaNodesMapFut
      } yield {
        val iter = for {
          medge         <- videoEdges.iterator
          edgeUid       <- medge.doc.uid.iterator
          nodeId        <- medge.nodeIds.iterator
          mnode         <- mediaNodesMap.get( nodeId )
          videoExt      <- mnode.extras.videoExt
        } yield {
          MJdEditEdge(
            predicate = videoPredicate,
            id        = edgeUid,
            url       = Some( videoUtil.toIframeUrl(videoExt) )
          )
        }
        iter.toSeq
      }

      // Собрать тексты из эджей
      val textJdEdges = {
        val textPred = MPredicates.JdContent.Text
        val textJdEdgesIter = for {
          textEdge <- request.mad.edges
            .withPredicateIter( textPred )
          edgeUid  <- textEdge.doc.uid.iterator
          text     <- textEdge.doc.text
        } yield {
          MJdEditEdge(
            predicate = textPred,
            id        = edgeUid,
            text      = Some(text)
          )
        }
        textJdEdgesIter.toSeq
      }

      // Объеденить все эджи.
      val edEdgesFut = for {
        imgJdEdges    <- imgJdEdgesFut
        videoJdEdges  <- videoJdEdgesFut
      } yield {
        val r = Iterator(textJdEdges, imgJdEdges, videoJdEdges)
          .flatten
          .toSeq
        LOGGER.trace(s"$logPrefix Compiled ${r.size} jd edges: text=${textJdEdges.size} img=${imgJdEdges.size} video=${videoJdEdges.size}")
        r
      }

      val nodeDoc = request.mad.extras.doc.get    // TODO .getOrElse(defaultDoc)

      // Подготовить ctxData:
      val ctxData1Fut = _ctxDataFut
      val ctxFut = ctxData1Fut.map { implicit ctxData0 =>
        implicitly[Context]
      }

      // Собрать модель и отрендерить:
      val formInitStrFut = for {
        ctx     <- ctxFut
        edEdges <- edEdgesFut
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
    val bp = uploadCtl.prepareUploadBp

    // Не используем canUpload*. Если юзер может создавать/редактировать карточку, то и картинки он может загружать.
    canCreateOrEditAd(adIdOpt = adIdU, producerIdOpt = nodeIdU).async(bp) { implicit request =>

      lazy val logPrefix = s"prepareUpload(${adIdU.orElse(nodeIdU).orNull})#${System.currentTimeMillis()}:"
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
