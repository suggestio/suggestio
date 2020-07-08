package controllers

import javax.inject.Inject
import io.suggest.ad.blk.{BlockPadding, BlockPaddings}
import io.suggest.ad.edit.m.{MAdEditFormConf, MAdEditFormInit}
import io.suggest.ad.form.AdFormConstants
import io.suggest.common.empty.OptionUtil
import io.suggest.ctx.CtxData
import io.suggest.err.HttpResultingException
import HttpResultingException._
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.jd.tags.JdTag
import io.suggest.jd.{MJdData, MJdDoc, MJdEdge, MJdTagId}
import io.suggest.n2.edge._
import io.suggest.n2.extra.MNodeExtras
import io.suggest.n2.extra.doc.MNodeDoc
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.primo.id._
import models.mctx.Context
import models.mproj.ICommonDi
import models.mup.{MColorDetectArgs, MUploadFileHandlers, MUploadInfoQs}
import models.req.{BfpArgs, IReq}
import play.api.libs.json.Json
import play.api.mvc._
import util.acl.{BruteForceProtect, CanCreateOrEditAd, CanEditAd, IsNodeAdmin}
import util.ad.{JdAdUtil, LkAdEdFormUtil}
import util.ext.ExtRscUtil
import util.mdr.MdrUtil
import util.n2u.N2VldUtil
import util.sec.CspUtil
import views.html.lk.ad.edit._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.17 12:04
  * Description: Контроллер react-формы редактора карточек.
  * Идёт на смену MarketAd, который обслуживал старинную форму редактора.
  */
final class LkAdEdit @Inject() (
                                 sioControllerApi                       : SioControllerApi,
                                 mCommonDi                              : ICommonDi,
                               )
  extends MacroLogsImpl
{

  import mCommonDi.current.injector

  // Ленивое DI для не-Singleton-режима работы.
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val canEditAd = injector.instanceOf[CanEditAd]
  private lazy val canCreateOrEditAd = injector.instanceOf[CanCreateOrEditAd]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val cspUtil = injector.instanceOf[CspUtil]
  private lazy val lkAdEdFormUtil = injector.instanceOf[LkAdEdFormUtil]
  private lazy val uploadCtl = injector.instanceOf[Upload]
  private lazy val n2VldUtil = injector.instanceOf[N2VldUtil]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]
  private lazy val jdAdUtil = injector.instanceOf[JdAdUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val extRscUtil = injector.instanceOf[ExtRscUtil]
  private lazy val mdrUtil = injector.instanceOf[MdrUtil]


  import sioControllerApi._
  import mCommonDi.{ec, csrf, errorHandler}


  private lazy val _BFP_ARGS = (BfpArgs.tryCountDivisor set 2)( BfpArgs.default )

  /** Накатить какие-то дополнительные CSP-политики для работы редактора. */
  private def _applyCspToEditPage(res0: Result): Result = {
    import cspUtil.Implicits._
    res0.withCspHeader( cspUtil.CustomPolicies.AdEdit )
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
      for {
        ctx <- _mkAdEditHtmlCtx
        adData <- lkAdEdFormUtil.defaultEmptyDocument(ctx)
      } yield {
        // Конфиг формы содержит только данные о родительском узле-продьюсере.
        val formInit = MAdEditFormInit(
          conf          = MAdEditFormConf(
            producerId  = producerId,
            adId        = None,
            ctxId       = ctx.ctxIdStr
          ),
          adData        = adData,
          blockPadding  = prodBlockPadding(request.mnode)
        )

        // Используем тот же edit-шаблон. что и при редактировании. Они отличаются только заголовками страниц.
        val html = adEditTpl(
          mad     = None,
          parent  = request.mnode,
          state0  = _formInit2str( formInit ),
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
      canCreateOrEditAd(adIdOptU, producerIdOpt = producerIdU).async( parse.json[MJdData] ) { implicit request =>
        import esModel.api._

        // Взять форму из реквеста, провалидировать
        lazy val logPrefix = s"saveAdSubmit(${request.madOpt.fold("pro")(_ => "a")}d#${adIdOptU.orElse(producerIdU).orNull}):"

        (for {
          // Сначала early-валидация эджей. Для валидации шаблона нужны рабочие эджи, поэтому валидация идёт в несколько шагов.
          jdData1 <- Future {
            lkAdEdFormUtil
              .earlyValidateJdData( request.body )
              .fold(
                // Не удалось понять присланные эджи:
                {errorsNel =>
                  val msg = errorsNel
                    .iterator
                    .mkString(", ")
                  LOGGER.warn(s"$logPrefix Failed to validate remote edges: $msg")
                  val resFut = errorHandler.onClientError(request, NOT_ACCEPTABLE, s"edges: $msg")
                  throw new HttpResultingException( resFut )
                },
                identity
              )
          }

          // Собрать данные по всем упомянутым в запросе узлам, не обрывая связь с исходными эджами.
          vldEdgesMap <- n2VldUtil.EdgesValidator( jdData1.edges ).vldEdgesMapFut

          tpl2 = lkAdEdFormUtil
            .validateTpl(
              template      = request.body.doc.template,
              vldEdgesMap   = vldEdgesMap,
            )
            .fold(
              // Не удалось провалидировать шаблон.
              {failedMsgs =>
                val msgsConcat = failedMsgs
                  .iterator
                  .mkString(", ")
                LOGGER.warn(s"$logPrefix Unable to validate template: $msgsConcat")
                val resFut = errorHandler.onClientError(request, NOT_ACCEPTABLE, s"tpl: $msgsConcat")
                throw new HttpResultingException( resFut )
              },
              identity
            )

          // -- Есть на руках валидный шаблон. Можно создавать новую карточку и др.узлы. dry-run-часть завершена --

          // Повторная чистка эджей по финальному шаблону.
          // Т.к. JdDocValidator теперь может удалять из шаблона невалидные куски, то тут требуется дополнительная зачистка эджей:
          edges2Map = JdTag.purgeUnusedEdges[MJdEdge](
            tpl2,
            jdData1
              .edges
              .zipWithIdIter[EdgeUid_t]
              .to( Map )
          )

          framePred = MPredicates.JdContent.Frame

          // Сграбить ссылки на внешние фрейморесурсы -- для них надо создать ext-узлы, этим занимается VideoUtil:
          extRscEdgesFut = extRscUtil.ensureExtRscNodes(
            videoUrls = (for {
              jdEdge <- edges2Map.valuesIterator
              if jdEdge.predicate ==>> framePred
              url    <- jdEdge.url
            } yield {
              url
            })
              .toSet,
            personIdOpt = request.user.personIdOpt
          )

          // Создать/обновить строковое название для узла карточки (на основе текстовых эджей)
          edgesTexts2Map = lkAdEdFormUtil.mkEdgeTextsMap( edges2Map.valuesIterator )

          nodeTechNameOpt = OptionUtil.maybeOpt( jdData1.title.isEmpty )( lkAdEdFormUtil.mkTechName(tpl2, edgesTexts2Map) )
          //LOGGER.trace(s"$logPrefix nodeTechName = ${nodeTechNameOpt.orNull}")

          extRscEdges <- extRscEdgesFut

          edgesAcc0 = {
            LOGGER.trace(s"$logPrefix ${extRscEdges.size} ExtRscEdges = [${extRscEdges.view.mapValues(_.idOrNull).mkString(", ")}]")

            var _acc0 = edges2Map.valuesIterator.foldLeft(List.empty[MEdge]) { (acc0, jdEdge) =>
              MEdge(
                predicate = jdEdge.predicate,
                nodeIds = {
                  val extRscNodeIdOpt = OptionUtil.maybeOpt(jdEdge.predicate ==>> framePred) {
                    jdEdge.url
                      .flatMap( extRscEdges.get )
                      .flatMap( _.id )
                  }
                  extRscNodeIdOpt
                    .orElse {
                      jdEdge.fileSrv.map(_.nodeId)
                    }
                    .toSet
                },
                doc = jdEdge.edgeDoc,
              ) :: acc0
            }

            // Помечаем, как отмодерированный, если текущий юзер -- это супер-юзер.
            if (request.user.isSuper)
              _acc0 ::= mdrUtil.mdrEdge(request.user, mdrUtil.mdrEdgeInfo(None))

            LOGGER.trace(s"$logPrefix JdEE->MEdge edges: [${_acc0.mkString(", ")}]")
            _acc0
          }

          // В зависимости от создания или редактирования карточки, делаем тот или иной апдейт.
          madSaved <- request.madOpt.fold[Future[MNode]] {
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
                  nameOpt  = jdData1.title,
                  techName = nodeTechNameOpt,
                )
              )
            )
            for (adId <- mNodes.save( mad0 )) yield {
              LOGGER.trace(s"$logPrefix Created new ad#$adId")
              MNode.id.set( Some(adId) )(mad0)
            }

          } { mad00 =>
            LOGGER.trace(s"$logPrefix Will update existing ad: ${mad00.idOrNull}")
            if (mad00.extras.doc.isEmpty)
              LOGGER.info(s"$logPrefix Initialized new jd-template, previous ad template was empty.")

            // Сохраняемая карточка уже существует: перезаписать в ней некоторые эджи.
            val filteredPreds = MPredicates.JdContent :: MPredicates.ModeratedBy :: Nil
            mNodes.tryUpdate(mad00)(
              MNode.edges
                .modify { edges0 =>
                  MNodeEdges(
                    MNodeEdges.edgesToMap1(
                      // Убрать все существующие jd-content-эджи.
                      edges0.withoutPredicateIter(filteredPreds: _*) ++ edgesAcc0
                    )
                  )
                } andThen
                // Залить новый шаблон:
                MNode.extras
                  .composeLens( MNodeExtras.doc )
                  .modify { mdoc0 =>
                    val mdoc2 = mdoc0.fold( MNodeDoc(template = tpl2) )( MNodeDoc.template.set(tpl2) )
                    Some( mdoc2 )
                  } andThen
                MNode.meta
                  .composeLens( MMeta.basic )
                  .modify(
                    (MBasicMeta.nameOpt set jdData1.title) andThen
                    (MBasicMeta.techName set nodeTechNameOpt)
                  )
            )
          }

        } yield {
          LOGGER.trace(s"$logPrefix Saved ok, ad#${madSaved.idOrNull}")
          implicit val ctx = implicitly[Context]

          // Сохранено успешно. Вернуть ответ в виде обновлённой формы.
          val formReInit2 = MAdEditFormInit(
            conf = MAdEditFormConf(
              producerId  = request.producer.id.get,
              adId        = madSaved.id,
              ctxId       = ctx.ctxIdStr
            ),
            adData = MJdData(
              doc = MJdDoc(
                template  = tpl2,
                tagId      = MJdTagId(
                  nodeId = madSaved.id,
                ),
              ),
              edges = edges2Map.values,
              title = madSaved.meta.basic.nameOpt,
            ),
            blockPadding = prodBlockPadding(request.producer)
          )

          Ok( Json.toJson(formReInit2) )
        })
          .recoverHttpResEx
      }
    }
  }


  /** Экшен рендера страницы с формой редактирования карточки.
    *
    * @param adIdU id рекламной карточки.
    * @return 200 OK с HTML-страницей для формы редактирования карточки.
    */
  def editAd(adIdU: MEsUuId) = csrf.AddToken {
    canEditAd(adIdU.id, U.Lk).async { implicit request =>
      for {
        // Сразу заготовить контекст рендера, он нужен на всех последующих шагах:
        ctx <- _mkAdEditHtmlCtx

        // Запустить сбоку карточки.
        // Тут зависимость от контекста. Если тормозит, то для ускорения можно передавать неполный контекст без ctxData - на jd-рендер это не влияет.
        jdAdData <- jdAdUtil
          .mkJdAdDataFor
          .edit( request.mad )(ctx)
          .execute()

      } yield {
        val formInit = MAdEditFormInit(
          conf = MAdEditFormConf(
            producerId  = request.producer.id.get,
            adId        = request.mad.id,
            ctxId       = ctx.ctxIdStr
          ),
          adData = jdAdData,
          blockPadding = prodBlockPadding(request.producer)
        )
        val formInitStr = _formInit2str( formInit )

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


  /**
    * POST для удаления рекламной карточки.
    *
    * @param adId id рекламы.
    * @return Редирект в магазин или ТЦ.
    */
  def deleteSubmit(adId: String) = csrf.Check {
    canEditAd(adId).async { implicit request =>
      import esModel.api._

      lazy val logPrefix = s"deleteSubmit($adId):"
      LOGGER.trace(s"$logPrefix Starting by user#${request.user.personIdOpt.orNull}...")

      for {
        isDeleted <- mNodes.deleteById(adId)
      } yield {
        LOGGER.info(s"$logPrefix Done, isDeleted = $isDeleted")

        // Удаление выполнено. Т.к. у нас pure-js-форма, то надо редирект на клиенте сделать.
        val call = routes.LkAds.adsPage(
          nodeKey = request.producer.id.get :: Nil
        )
        Ok( call.url )
      }
    }
  }


  /** Собрать инстанс ctxData. */
  private def _ctxDataFut(implicit request: IReq[_]): Future[CtxData] = {
    request.user.lkCtxDataFut
      .map(
        CtxData.jsInitTargetsAppendOne( MJsInitTargets.LkAdEditR )
      )
  }

  private def _mkAdEditHtmlCtx(implicit request: IReq[_]): Future[Context] = {
    _ctxDataFut.map { implicit ctxData0 =>
      getContext2
    }
  }


  /** Сериализовать в строку инсанс MAdEditFormInit. */
  private def _formInit2str(formInit: MAdEditFormInit): String = {
    // TODO Есть мнение, что надо будет заюзать MsgPack+base64, т.е. и бинарь, и JSON в одном флаконе.
    // Этот JSON рендерится в html-шаблоне сейчас в строковую помойку вида "&quota&quot;,&quot" и довольно толстоват.
    // Только сначала желательно бы выкинуть boopickle, заменив её на play-json или msgpack везде, где уже используется.
    Json.toJson(formInit).toString()
  }


  private def prodBlockPadding(producer: MNode): BlockPadding = {
    // Реализация подсистемы управления интервалом плитки отложена на потом: быстро сделать не удалось.
    BlockPaddings.default
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
  def prepareImgUpload(adIdU: Option[MEsUuId], nodeIdU: Option[MEsUuId]) = {
    csrf.Check {
      bruteForceProtect(_BFP_ARGS) {
        // Не используем canUpload*. Если юзер может создавать/редактировать карточку, то и картинки он может загружать.
        canCreateOrEditAd(adIdOpt = adIdU, producerIdOpt = nodeIdU)
          .async( uploadCtl.prepareUploadBp ) { implicit request =>
            val validated = lkAdEdFormUtil.image4UploadPropsV( request.body )

            // И просто запустить API-метод prepareUpload() из Upload-контроллера.
            uploadCtl.prepareUploadLogic(
              logPrefix = s"prepareUpload(${adIdU.orElse(nodeIdU).orNull})#${System.currentTimeMillis()}:",
              validated = validated,
              upInfo = MUploadInfoQs(
                // Сразу отправлять принятый файл в MLocalImg минуя /tmp/.
                fileHandler       = Some( MUploadFileHandlers.Image ),
                colorDetect       = Some {
                  val cdConst = AdFormConstants.ColorDetect
                  val sz = cdConst.PALETTE_SIZE
                  MColorDetectArgs(
                    paletteSize   = sz,
                    wsPaletteSize = sz  // cdConst.PALETTE_SHRINK_SIZE
                  )
                },
                nodeType = Some( MNodeTypes.Media.Image ),
              ),
            )
          }
      }
    }
  }

}
