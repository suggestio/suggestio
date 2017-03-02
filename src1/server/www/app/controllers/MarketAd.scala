package controllers

import java.time.OffsetDateTime

import com.google.inject.Inject
import com.google.inject.name.Named
import io.suggest.ad.form.AdFormConstants._
import io.suggest.init.routed.MJsiTgs
import io.suggest.model.n2.ad.MNodeAd
import io.suggest.model.n2.edge.{MNodeEdges, NodeEdgesMap_t}
import io.suggest.model.n2.node.MNodes
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.MBasicMeta
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.blk.PrepareBlkImgArgs
import models.blk.ed.{AdFormM, AdFormResult, BlockImgMap}
import models.im.MImg3
import models.im.make.IMaker
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.{IAdProdReq, INodeReq, IReq}
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Call, Request, Result, WebSocket}
import play.core.parsers.Multipart
import play.twirl.api.Html
import util.acl._
import util.ad.LkAdEdFormUtil
import util.blocks.{BgImg, ListBlock, LkEditorWsActors}
import util.mdr.SysMdrUtil
import util.n2u.N2NodesUtil
import views.html.lk.ad._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
class MarketAd @Inject() (
                           tempImgSupport                          : TempImgSupport,
                           mNodes                                  : MNodes,
                           sysMdrUtil                              : SysMdrUtil,
                           lkEditorWsActors                        : LkEditorWsActors,
                           isAuth                                  : IsAuth,
                           canEditAd                               : CanEditAd,
                           @Named("blk") override val blkImgMaker  : IMaker,
                           n2NodesUtil                             : N2NodesUtil,
                           canUpdateSls                            : CanUpdateSls,
                           override val isNodeAdmin             : IsNodeAdmin,
                           override val marketAdFormUtil           : LkAdEdFormUtil,
                           override val mCommonDi                  : ICommonDi
)
  extends SioController
  with MacroLogsImpl
  with BruteForceProtect
  with MarketAdPreview
{

  import LOGGER._
  import mCommonDi._
  import marketAdFormUtil._

  type ReqSubmit = Request[collection.Map[String, Seq[String]]]
  type DetectForm_t = Either[AdFormM, (BlockConf, AdFormM)]

  override val BRUTEFORCE_TRY_COUNT_DIVISOR = 3
  override val BRUTEFORCE_CACHE_PREFIX = "aip:"


  /** Макс.длина загружаемой картинки в байтах. */
  private val IMG_UPLOAD_MAXLEN_BYTES: Int = {
    val mib = current.configuration.getInt("ad.img.len.max.mib") getOrElse 40
    mib * 1024 * 1024
  }


  /** Полный ключ доступа к полю bgImg в маппинге формы. */
  private def bgImgFullK = OFFER_K + "." + BgImg.BG_IMG_FN


   /**
    * Запуск детектирования палитры картинки в фоне.
    *
    * @param form Маппинг формы со всеми данными.
    * @param ctx Контекст рендера.
    */
  private def detectMainColorBg(form: AdFormM)(implicit ctx: Context): Unit = {
    val vOpt = form(bgImgFullK).value
    try {
      for (v <- vOpt) {
        val im = MImg3(v)
        tempImgSupport._detectPalletteWs(im, wsId = ctx.ctxIdStr)
      }
    } catch {
      case ex: Throwable =>
        error("detectMainColorBg(): Cannot start color detection for im = " + vOpt, ex)
    }
  }


   /**
    * Рендер унифицированной страницы добаления рекламной карточки.
    *
    * @param adnId id узла рекламной сети.
    */
  def createAd(adnId: String) = csrf.AddToken {
    isNodeAdmin(adnId, U.Balance).async { implicit request =>
      _renderCreateFormWith(
        af      = adFormM,
        adnNode = request.mnode,
        rs      = Ok
      )
    }
  }

  /** Общий код рендера createShopAdTpl с запросом необходимых категорий. */
  private def _renderCreateFormWith(af: AdFormM, adnNode: MNode, withBC: Option[BlockConf] = None, rs: Status)
                                   (implicit request: INodeReq[_]): Future[Result] = {
    _renderPage(af, rs) { implicit ctx =>
      createAdTpl(af, adnNode, withBC)(ctx)
    }
  }

   /**
    * Сабмит формы добавления рекламной карточки товара/скидки.
    *
    * @param adnId id магазина.
    */
  def createAdSubmit(adnId: String) = csrf.Check {
    isNodeAdmin(adnId).async(parse.urlFormEncoded) { implicit request =>
      import request.mnode
      lazy val logPrefix = s"createAdSubmit($adnId): "
      val bc = BlocksConf.DEFAULT
      adFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(logPrefix + "Bind failed: \n" + formatFormErrors(formWithErrors))
          _renderCreateFormWith(formWithErrors, mnode, Some(bc), NotAcceptable)
        },
        {r =>
          // Асинхронно обрабатываем сохранение картинок
          val saveImgsFut = bc.saveImgs(r.bim)

          // Сохраняем саму карточку, когда пришло подтверждение от картинок.
          val adIdFut = for {
            savedImgs <- saveImgsFut
            adId      <- {
              val mad2 = r.mad.copy(
                edges = r.mad.edges.copy(
                  out = {
                    val prodE = MEdge(
                      predicate = MPredicates.OwnedBy,
                      nodeIds   = mnode.id.toSet
                    )
                    var iter  = r.mad.edges.iterator ++ savedImgs ++ Seq(prodE)
                    // Добавить эдж модератора, если карточка создаётся модератором.
                    if (request.user.isSuper)
                      iter ++= Seq( sysMdrUtil.mdrEdge() )
                    // Сгенерить и вернуть итоговую карту эджей.
                    MNodeEdges.edgesToMap1(iter)
                  }
                )
              )
              mNodes.save(mad2)
            }
          } yield {
            adId
          }
          // Сборка и возврат HTTP-ответа.
          for (adId <- adIdFut) yield {
            Redirect(routes.MarketLkAdn.showNodeAds(adnId, newAdId = Some(adId)))
              .flashing(FLASH.SUCCESS -> "Ad.created")
          }
        }
      )
    }
  }



  /** Акт рендера результирующей страницы в отрыве от самой страницы. */
  private def _renderPage(af: AdFormM, rs: Status)(f: Context => Html)
                         (implicit request: IReq[_]): Future[Result] = {
    for {
      ctxData0      <- request.user.lkCtxDataFut
    } yield {
      implicit val ctxData = ctxData0.withJsiTgs(
        MJsiTgs.AdForm :: ctxData0.jsiTgs
      )
      implicit val ctx = getContext2
      detectMainColorBg(af)(ctx)
      rs(f(ctx))
    }
  }


  /**
    * Рендер страницы с формой редактирования рекламной карточки магазина.
    *
    * @param adId id рекламной карточки.
    */
  def editAd(adId: String) = csrf.AddToken {
    canEditAd(adId, U.Lk).async { implicit request =>
      import request.mad
      val bc = n2NodesUtil.bc(mad)

      // Собрать карту картинок для маппинга формы.
      val bim: BlockImgMap = {
        mad.edges
          .withPredicateIter(bc.imgKeys : _*)
          .map { e =>
            e.predicate -> MImg3(e)
          }
          .toMap
      }

      // Собрать и заполнить маппинг формы.
      val formVal = AdFormResult(mad, bim)
      val formFilled = adFormM.fill( formVal )

      // Вернуть страницу с рендером формы.
      _renderEditFormWith(formFilled, Ok)
    }
  }


  /** Общий код рендера ad edit страницы живёт в этом методе. */
  private def _renderEditFormWith(af: AdFormM, rs: Status)
                                 (implicit request: IAdProdReq[_]): Future[Result] = {
    _renderPage(af, rs) { implicit ctx =>
      import request.{mad, producer}
      editAdTpl(mad, af, producer)(ctx)
    }
  }


  /** Сабмит формы рендера страницы редактирования рекламной карточки.
    *
    * @param adId id рекламной карточки.
    */
  def editAdSubmit(adId: String) = csrf.Check {
    canEditAd(adId).async(parse.urlFormEncoded) { implicit request =>
      lazy val logPrefix = s"editShopAdSubmit($adId): "
      adFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(logPrefix + "Failed to bind form: " + formWithErrors.errors)
          _renderEditFormWith(formWithErrors, NotAcceptable)
        },
        {r =>
          val bc = n2NodesUtil.bc(request.mad)

          // TODO Надо отделить удаление врЕменных и былых картинок от сохранения новых. И вызывать эти две фунции отдельно.
          // Сейчас возможна ситуация, что при поздней ошибке сохранения теряется старая картинка, а новая сохраняется вникуда.
          val _imgPreds = bc.imgKeys
          val saveImgsFut = bc.saveImgs(
            newImgs     = r.bim,
            oldImgs     = request.mad.edges
              .withPredicateIter(_imgPreds: _*)
              .toList
          )

          // Какие предикаты нужно удалить перед заливкой обновлённых?
          val predsFilteredOut: List[MPredicate] = {
            MPredicates.ModeratedBy :: _imgPreds
          }

          // Произвести действия по сохранению карточки.
          val saveFut = for {
            imgsSaved <- saveImgsFut
            mnode2    <- mNodes.tryUpdate(request.mad) { mad0 =>
              mad0.copy(
                meta = mad0.meta.copy(
                  colors = r.mad.meta.colors,
                  basic  = mad0.meta.basic.copy(
                    dateEdited = Some( OffsetDateTime.now() )
                  ),
                  business = mad0.meta.business.copy(
                    siteUrl = r.mad.meta.business.siteUrl
                  )
                ),
                ad = r.mad.ad,
                edges = r.mad.edges.copy(
                  out = {
                    // Нужно залить новые картинки, сбросить данные по модерации.
                    var iter = mad0.edges
                      .withoutPredicateIter(predsFilteredOut : _*)
                      .++( imgsSaved )
                    if (request.user.isSuper)
                      iter ++= Seq( sysMdrUtil.mdrEdge() )
                    MNodeEdges.edgesToMap1( iter )
                  }
                )
              )
            }
          } yield {
            // Просто надо что-нибудь вернуть...
            mnode2
          }
          // HTTP-ответ.
          for (_ <- saveFut) yield {
            Redirect( _routeToMadProducerOrLkList(request.mad) )
              .flashing(FLASH.SUCCESS -> "Changes.saved")
          }
        }
      )
    }
  }


  /** Безопасная сборка call для редиректа. Маловероятна (но возможна ситуация), когда у редактируемой
    * карточки нет продьюсера. Если так, то надо будет вернуть lkList(). */
  private def _routeToMadProducerOrLkList(mad: MNode): Call = {
    val prodIdOpt = n2NodesUtil.madProducerId(mad)
    val mlk = routes.MarketLkAdn
    prodIdOpt.fold[Call] {
      mlk.lkList()
    } { prodId =>
      mlk.showNodeAds(prodId)
    }
  }

  /** Рендер окошка с подтверждением удаления рекламной карточки. */
  def deleteWnd(adId: String) = csrf.AddToken {
    canEditAd(adId).async { implicit request =>
      Ok(_deleteWndTpl(request.mad))
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
      for {
        isDeleted <- mNodes.deleteById(adId)
      } yield {
        Redirect( _routeToMadProducerOrLkList(request.mad) )
          .flashing(FLASH.SUCCESS -> "Ad.deleted")
      }
    }
  }


  // ===================================== ad show levels =============================================

  /** Форма для маппинга результатов  */
  private def adShowLevelFormM: Form[(AdShowLevel, Boolean)] = Form(tuple(
    // id уровня, прописано в чекбоксе
    "levelId" -> nonEmptyText(maxLength = 1)
      .transform [Option[AdShowLevel]] (
        { AdShowLevels.maybeWithName },
        { case Some(sl) => sl.toString()
          case None => "" }
      )
      .verifying("ad.show.level.undefined", _.isDefined)
      .transform[AdShowLevel](_.get, Some.apply)
    ,
    "levelEnabled" -> boolean   // Новое состояние чекбокса.
  ))

  /**
   * Включение/выключение какого-то уровня отображения указанной рекламы.
   * Сабмит сюда должен отсылаться при нажатии на чекбоксы отображения на тех или иных экранах в _showAdsTpl.
   */
  def updateShowLevelSubmit(adId: String) = csrf.Check {
    canUpdateSls(adId).async { implicit request =>
      lazy val logPrefix = s"updateShowLevelSubmit($adId): "
      adShowLevelFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(logPrefix + "Failed to bind form: " + formWithErrors.errors)
          NotAcceptable("Request body invalid.")
        },
        {case (sl, isLevelEnabled) =>
          val producerId = request.producer.id.get

          trace(s"${logPrefix}Updating ad[$adId] with sl=$sl/$isLevelEnabled prodId=$producerId")

          val saveFut = mNodes.tryUpdate(request.mad) { mad =>
            // Извлекаем текущее ребро данного ресивера
            val e0Opt = mad
              .edges
              .withNodePred(producerId, MPredicates.Receiver)
              .toStream
              .headOption

            val e0 = e0Opt.getOrElse {
              MEdge(
                predicate = MPredicates.Receiver.Self,
                nodeIds   = request.producer.id.toSet
              )
            }

            val sls0 = e0.info.sls

            // TODO Удалить поддержку sink'ов, когда будет выпилен старый биллинг.
            def _mkSlss(src: TraversableOnce[AdnSink]) = {
              for (sink <- src.toIterator) yield {
                SinkShowLevels.withArgs(sink, sl)
              }
            }

            val sls1 = if (isLevelEnabled) {
              val prodSinks = Set(AdnSinks.SINK_GEO)
              sls0 ++ _mkSlss(prodSinks)

            } else {
              sls0 -- _mkSlss(AdnSinks.valuesT)
            }

            val mapOpt: Option[NodeEdgesMap_t] = if (sls1.isEmpty) {
              // Пустой список sls: значит нужно удалить ресивера.
              for (_ <- e0Opt) yield {
                // Исходный эдж существует. Удалить его из исходной карты эджей.
                MNodeEdges.edgesToMap1(
                  mad.edges.withoutNodePred(producerId, MPredicates.Receiver)
                )
              }
              // None будет означать, что ничего обновлять не надо, и нужно вернуть null из фунцкии.

            } else if (sls0 == sls1) {
              // Есть новые список уровней есть, но он не изменился относительно текущего. Не обновлять ничего.
              LOGGER.trace(s"$logPrefix SLS not changed, nothing to update: $sls0")
              None

            } else {
              // Есть новый и изменившийся список уровней. Выставить ресивера в карту эджей.
              val e1 = e0.copy(
                info = e0.info.copy(sls = sls1)
              )
              val map1 = mad.edges.out ++ MNodeEdges.edgesToMap(e1)
              Some(map1)
            }

            mapOpt.fold [MNode] {
              // Ничего менять не требуется -- вернуть null наверх
              LOGGER.trace(logPrefix + "nothing for tryUpdate()")
              null
            } { eout2 =>
              // Сохранить новую карту эджей в исходный инстанс
              mad.copy(
                edges = mad.edges.copy(
                  out = eout2
                )
              )
            }
          }

          // Отрендерить результат по завершению апдейта.
          for (_ <- saveFut) yield {
            Ok("Done")
          }
        }     // form.fold() right
      )       // form.fold()
    }
  }


  /** Открытие websocket'а для обратной асинхронной связи с браузером клиента. */
  def ws(wsId: String) = WebSocket.acceptOrResult[JsValue, JsValue] { implicit request =>
    // Прямо тут проверяем права доступа. Пока просто проверяем залогиненность вопрошающего.
    val personIdOpt = sessionUtil.getPersonId(request)
    val auth = personIdOpt.isDefined
    def logPrefix = s"ws($wsId):"
    val res = if (auth) {
      LOGGER.trace(s"$logPrefix Starting websocket...")
      val aFlow = ActorFlow.actorRef(
        props = lkEditorWsActors.props(_, wsId)
      )
      Right(aFlow)

    } else {
      LOGGER.warn(s"$logPrefix User $personIdOpt is not allowed.")
      val result = Forbidden("Unathorized")
      Left(result)
    }
    Future.successful( res )
  }


  /**
    * Рендер нового блока редактора для ввода текста.
    *
    * @param offerN номер оффера
    * @param height текущая высота карточки. Нужно для того, чтобы новый блок не оказался где-то не там.
    * @param width Текущая ширина карточки.
    * @return 200 ok с инлайновым рендером нового текстового поля формы редактора карточек.
    */
  def newTextField(offerN: Int, height: Int, width: Int) = isAuth() { implicit request =>
    val bfText = ListBlock.mkBfText(offerNopt = Some(offerN))
    // Чтобы залить в форму необходимые данные, надо сгенерить экземпляр рекламной карточки.
    implicit val ctx = implicitly[Context]
    val madStub = MNode(
      common = MNodeCommon(
        ntype         = MNodeTypes.Ad,
        isDependent   = true
      ),
      meta = MMeta(
        basic = MBasicMeta()
      ),
      ad = MNodeAd(
        entities = {
          val ent = MEntity(
            id = offerN,
            text = Some(TextEnt(
              value = ctx.messages("bf.text.example", offerN),
              font = EntFont(),
              coords = Some(Coords2d(
                x = height / 2,
                y = width / 4
              ))  // Coords2D
            ))    // AOStringField
          )
          MNodeAd.toEntMap(ent)
        }
      )         // MNodeAd
    )         // MAd
    val formData = AdFormResult(madStub, Map.empty)
    val af = adFormM.fill( formData )
    val nameBase = s"$OFFER_K.$OFFER_K[$offerN].${bfText.name}"
    val bc = BlocksConf.DEFAULT
    val render = bfText.renderEditorField(nameBase, af, bc)(ctx)
    Ok(render)
  }


  // ============================== common-методы =================================

  private def blockImgBp = parse.multipartFormData(
    Multipart.handleFilePartAsTemporaryFile,
    maxLength = IMG_UPLOAD_MAXLEN_BYTES.toLong
  )

  /** Подготовка картинки, которая загружается в динамическое поле блока. */
  def prepareBlockImg(args: PrepareBlkImgArgs) = isAuth().async(blockImgBp) { implicit request =>
    bruteForceProtected {
      args.bc.getImgFieldForName(args.bimKey) match {
        case Some(bfi) =>
          val resultFut = tempImgSupport._handleTempImg(
            preserveUnknownFmt = false,
            runEarlyColorDetector = bfi.preDetectMainColor,
            wsId   = args.wsId,
            ovlRrr = Some { (imgId, ctx) =>
              _bgImgOvlTpl(imgId)(ctx)
            }
          )
          resultFut

        case _ =>
          warn(s"prepareBlockImg($args): Unknown img field requested. 404")
          NotFound
      }
    }
  }


}

