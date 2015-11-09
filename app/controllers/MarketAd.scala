package controllers

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import models.im.MImg
import models.jsm.init.MTargets
import models.mtag.MTagUtil
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import play.api.cache.CacheApi
import play.api.db.Database
import play.api.i18n.MessagesApi
import play.api.libs.json.JsValue
import play.core.parsers.Multipart
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.blocks.{LkEditorWsActor, ListBlock, BgImg}
import views.html.lk.ad._
import models._
import play.api.data._, Forms._
import util.acl._
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{WebSocket, Request}
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import controllers.ad.MarketAdFormUtil
import MarketAdFormUtil._
import io.suggest.ad.form.AdFormConstants._

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
class MarketAd @Inject() (
  override val messagesApi        : MessagesApi,
  override implicit val current   : play.api.Application,
  override val cache              : CacheApi,
  tempImgSupport                  : TempImgSupport,
  mTagUtil                        : MTagUtil,
  override val _contextFactory    : Context2Factory,
  override val mNodeCache         : MAdnNodeCache,
  override val db                 : Database,
  override val errorHandler       : ErrorHandler,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends SioController
  with PlayMacroLogsImpl
  with BruteForceProtectCtl
  with MarketAdPreview
  with ad.NodeTagsEdit
  with CanEditAd
  with CanUpdateSls
  with IsAdnNodeAdmin
  with IsAuth
{

  import LOGGER._

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
   * @param form Маппинг формы со всеми данными.
   * @param ctx Контекст рендера.
   */
  private def detectMainColorBg(form: AdFormM)(implicit ctx: Context): Unit = {
    val vOpt = form(bgImgFullK).value
    try {
      vOpt.foreach { v =>
        val im = MImg(v)
        tempImgSupport._detectPalletteWs(im, wsId = ctx.ctxIdStr)
      }
    } catch {
      case ex: Exception =>
        debug("detectMainColorBg(): Cannot start color detection for im = " + vOpt)
    }
  }


  /**
   * Рендер унифицированной страницы добаления рекламной карточки.
   * @param adnId id узла рекламной сети.
   */
  def createAd(adnId: String) = IsAdnNodeAdminGet(adnId).async { implicit request =>
    import request.adnNode
    renderCreateFormWith(
      af = adFormM,
      catOwnerId = getCatOwnerId(adnNode),
      adnNode = adnNode
    ).map(Ok(_))
  }

  /** Рендер ошибки в create-форме. Довольно общий, но асинхронный код.
    * @param formWithErrors Форма для рендера.
    * @param catOwnerId id владельца категории. Обычно id ТЦ.
    * @param adnNode Магазин, с которым происходит сейчас работа.
    * @return NotAcceptable со страницей с create-формой.
    */
  private def createAdFormError(formWithErrors: AdFormM, catOwnerId: String, adnNode: MNode, withBC: Option[BlockConf])
                               (implicit request: AbstractRequestForAdnNode[_]) = {
    renderCreateFormWith(formWithErrors, catOwnerId, adnNode, withBC)
      .map(NotAcceptable(_))
  }

  /**
   * Сабмит формы добавления рекламной карточки товара/скидки.
   * @param adnId id магазина.
   */
  def createAdSubmit(adnId: String) = IsAdnNodeAdminPost(adnId).async(parse.urlFormEncoded) { implicit request =>
    import request.adnNode
    val catOwnerId = getCatOwnerId(adnNode)
    lazy val logPrefix = s"createAdSubmit($adnId): "
    val formM = adFormM
    val formBinded = formM.bindFromRequest()
    val bc = BlocksConf.DEFAULT
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Bind failed: \n" + formatFormErrors(formWithErrors))
        createAdFormError(formWithErrors, catOwnerId, adnNode, Some(bc))
      },
      {case (mad, bim) =>
        // Асинхронно обрабатываем всякие прочие данные.
        val saveImgsFut = bc.saveImgs(
          newImgs     = bim,
          oldImgs     = Map.empty
        )
        // Когда всё готово, сохраняем саму карточку.
        val adIdFut = for {
          savedImgs <- saveImgsFut
          adId      <- {
            mad.copy(
              producerId    = adnId,
              imgs          = savedImgs
            ).save
          }
        } yield {
          adId
        }
        // Сборка HTTP-ответа.
        val resFut = adIdFut map { adId =>
          Redirect(routes.MarketLkAdn.showNodeAds(adnId, newAdId = Some(adId)))
            .flashing(FLASH.SUCCESS -> "Ad.created")
        }
        // Сохранение новых тегов в MNode.
        adIdFut.onSuccess { case _ =>
          val fut = mTagUtil.handleNewTagsM(mad.tags)
          logTagsUpdate(fut, logPrefix)
        }
        // Возврат HTTP-ответа.
        resFut
      }
    )
  }

  /** Общий код рендера createShopAdTpl с запросом необходимых категорий. */
  private def renderCreateFormWith(af: AdFormM, catOwnerId: String, adnNode: MNode, withBC: Option[BlockConf] = None)
                                  (implicit request: AbstractRequestForAdnNode[_]) = {
    _renderPage(af) { (mmcats, ctx) =>
      createAdTpl(mmcats, af, adnNode, withBC)(ctx)
    }
  }

  private def renderEditFormWith(af: AdFormM)(implicit request: RequestWithAdAndProducer[_]) = {
    _renderPage(af) { (mmcats, ctx) =>
      import request.{producer, mad}
      editAdTpl(mad, mmcats, af, producer)(ctx)
    }
  }

  /** Акт рендера результирующей страницы в отрыве от самой страницы. */
  private def _renderPage(af: AdFormM)(f: (Seq[MMartCategory], Context) => Html)
                         (implicit request: AbstractRequestWithPwOpt[_]): Future[Html] = {
    val cats = mmCats
    implicit val _jsInitTargets = Seq(MTargets.AdForm)
    implicit val ctx = implicitly[Context]
    detectMainColorBg(af)(ctx)
    cats.map(f(_, ctx))
  }


  private def renderFailedEditFormWith(af: AdFormM)(implicit request: RequestWithAdAndProducer[_]) = {
    renderEditFormWith(af) map {
      NotAcceptable(_)
    }
  }

  /** Рендер страницы с формой редактирования рекламной карточки магазина.
    * @param adId id рекламной карточки.
    */
  def editAd(adId: String) = CanEditAdGet(adId).async { implicit request =>
    import request.mad
    val form0 = adFormM
    val bim = mad
      .imgs
      .mapValues { mii =>
        MImg(mii.filename)
      }
    val formFilled = form0 fill ((mad, bim))
    renderEditFormWith(formFilled)
      .map { Ok(_) }
  }


  /** Сабмит формы рендера страницы редактирования рекламной карточки.
    * @param adId id рекламной карточки.
    */
  def editAdSubmit(adId: String) = CanEditAdPost(adId).async(parse.urlFormEncoded) { implicit request =>
    import request.mad
    val formM = adFormM
    val formBinded = formM.bindFromRequest()
    lazy val logPrefix = s"editShopAdSubmit($adId): "
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formWithErrors.errors)
        renderFailedEditFormWith(formWithErrors)
      },
      {case (mad2, bim) =>
        val bc = BlocksConf.DEFAULT
        // TODO Надо отделить удаление врЕменных и былых картинок от сохранения новых. И вызывать эти две фунции отдельно.
        // Сейчас возможна ситуация, что при поздней ошибке сохранения теряется старая картинка, а новая сохраняется вникуда.
        val saveImgsFut = bc.saveImgs(
          newImgs     = bim,
          oldImgs     = mad.imgs
        )
        // Произвести действия по сохранению карточки.
        val saveFut = for {
          imgsSaved <- saveImgsFut
          _adId     <- MAd.tryUpdate(request.mad) { mad0 =>
            mad0.copy(
              imgs          = imgsSaved,
              disableReason = Nil,
              moderation    = mad0.moderation.copy(
                freeAdv = mad0.moderation.freeAdv
                  .filter { _.isAllowed != true }
              ),
              colors        = mad2.colors,
              offers        = mad2.offers,
              userCatId     = mad2.userCatId,
              blockMeta     = mad2.blockMeta,
              richDescrOpt  = mad2.richDescrOpt,
              dateEdited    = Some(DateTime.now),
              tags          = mad2.tags
            )
          }
        } yield {
          // Просто надо что-нибудь вернуть...
          _adId
        }
        // Запустить сборку HTTP-ответа.
        val resFut = saveFut map { _ =>
          Redirect(routes.MarketLkAdn.showNodeAds(mad.producerId))
            .flashing(FLASH.SUCCESS -> "Changes.saved")
        }
        // Параллельно произвести обновление графа MNode на предмет новых тегов:
        saveFut.onSuccess { case _ =>
          val fut = mTagUtil.handleNewTagsI(mad2,  oldTags = mad)
          logTagsUpdate(fut, logPrefix)
        }
        // Вернуть HTTP-ответ
        resFut
      }
    )
  }

  private def logTagsUpdate(fut: Future[_], logPrefix: => String): Unit = {
    fut.onComplete {
      case Success(res) => trace(logPrefix + "saved tag nodes: " + res)
      case Failure(ex)  => error(logPrefix + "failed to save tag nodes", ex)
    }
  }

  /** Рендер окошка с подтверждением удаления рекламной карточки. */
  def deleteWnd(adId: String) = CanEditAdGet(adId).async { implicit request =>
    Ok(_deleteWndTpl(request.mad))
  }

  /**
   * POST для удаления рекламной карточки.
   * @param adId id рекламы.
   * @return Редирект в магазин или ТЦ.
   */
  def deleteSubmit(adId: String) = CanEditAdPost(adId).async { implicit request =>
    MAd.deleteById(adId) map { _ =>
      val routeCall = routes.MarketLkAdn.showNodeAds(request.mad.producerId)
      Redirect(routeCall)
        .flashing(FLASH.SUCCESS -> "Ad.deleted")
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
  def updateShowLevelSubmit(adId: String) = CanUpdateSls(adId).async { implicit request =>
    lazy val logPrefix = s"updateShowLevelSubmit($adId): "
    adShowLevelFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formWithErrors.errors)
        NotAcceptable("Request body invalid.")
      },
      {case (sl, isLevelEnabled) =>
        // Бывает, что ресиверы ещё не выставлены. Тогда нужно найти получателя и вписать его сразу.
        val additionalReceiversFut: Future[Receivers_t] = if (request.mad.receivers.isEmpty) {
          val rcvrsFut = detectReceivers(request.producer)
          rcvrsFut onSuccess {
            case result =>
              debug(logPrefix + "No receivers found in Ad. Generated new receivers map: " + result.valuesIterator.mkString("[", ", ", "]"))
          }
          rcvrsFut
        } else {
          Future successful Map.empty
        }
        // Маппим уровни отображения на sink-уровни отображения, доступные узлу-продьюсеру.
        // Нет смысла делить на wi-fi и geo, т.к. вектор идёт на геолокации, и wifi становится вторичным.
        val prodSinks = request.producer
          .extras.adn
          .fold(Set.empty[AdnSink])(_.sinks) + AdnSinks.SINK_GEO
        val ssls = prodSinks
          .map { SinkShowLevels.withArgs(_, sl) }
        trace(s"${logPrefix}Updating ad[$adId] with sinkSls = [${ssls.mkString(", ")}]; prodSinks = [${prodSinks.mkString(",")}] sl=$sl prodId=${request.producerId}")
        additionalReceiversFut flatMap { addRcvrs =>
          // Нужно, чтобы настройки отображения также повлияли на выдачу. Добавляем выхлоп для producer'а.
          MAd.tryUpdate(request.mad) { mad =>
            val rcvrs1 = mad.receivers ++ addRcvrs
            val rcvrs2: Receivers_t = rcvrs1.get(mad.producerId) match {
              // Ещё не было такого ресивера.
              case None =>
                if (isLevelEnabled) {
                  rcvrs1 + (mad.producerId -> AdReceiverInfo(mad.producerId, ssls.toSet))
                } else {
                  // Вычитать уровни из отсутсвующего ресивера бессмысленно. TODO Не обновлять mad в этом случае.
                  rcvrs1
                }
              // Уже есть ресивер с какими-то уровнями (или без них) в карте ресиверов.
              case Some(prodRcvr) =>
                val sls2 = if (isLevelEnabled)  prodRcvr.sls ++ ssls  else  prodRcvr.sls -- ssls
                if (sls2.isEmpty) {
                  // Уровней отображения больше не осталось, поэтому выпиливаем ресивера целиком.
                  rcvrs1 - mad.producerId
                } else {
                  // Добавляем новые уровни отображения к имеющемуся ресиверу.
                  val prodRcvr1 = prodRcvr.copy(
                    sls = sls2
                  )
                  rcvrs1 + (mad.producerId -> prodRcvr1)
                }
            }
            mad.copy(
              receivers = rcvrs2
            )
          } map { _ =>
            Ok("Updated ok.")
          }
        }
      }
    )
  }


  /** Детектор получателей рекламы. Заглядывает к себе и к прямому родителю, если он указан. */
  private def detectReceivers(producer: MNode): Future[Receivers_t] = {
    val selfRcvrIds: Seq[String] = Some(producer)
      .filter(_.extras.adn.exists(_.isReceiver))
      .map(_.idOrNull)
      .toSeq
    val result = selfRcvrIds.map { rcvrId =>
      rcvrId -> AdReceiverInfo(rcvrId)
    }.toMap
    Future successful result
  }


  /** Открытие websocket'а для обратной асинхронной связи с браузером клиента. */
  def ws(wsId: String) = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    // Прямо тут проверяем права доступа. Пока просто проверяем залогиненность вопрошающего.
    val auth = PersonWrapper.getFromRequest.isDefined
    Future.successful(
      if (auth) {
        Right(LkEditorWsActor.props(_, wsId))
      } else {
        val result = Forbidden("Unathorized")
        Left(result)
      }
    )
  }


  /** Рендер нового блока редактора для ввода текста.
    * @param offerN номер оффера
    * @param height текущая высота карточки. Нужно для того, чтобы новый блок не оказался где-то не там.
    * @param width Текущая ширина карточки.
    * @return 200 ok с инлайновым рендером нового текстового поля формы редактора карточек.
    */
  def newTextField(offerN: Int, height: Int, width: Int) = IsAuth { implicit request =>
    val bfText = ListBlock.mkBfText(offerNopt = Some(offerN))
    // Чтобы залить в форму необходимые данные, надо сгенерить экземпляр рекламной карточки.
    implicit val ctx = implicitly[Context]
    val madStub = MAd(
      producerId = "",
      offers = List(AOBlock(
        n = offerN,
        text1 = Some(TextEnt(
          value = ctx.messages("bf.text.example", offerN),
          font = EntFont(),
          coords = Some(Coords2d(
            x = height / 2,
            y = width / 4
          ))  // Coords2D
        ))    // AOStringField
      ))      // AOBlock
    )         // MAd
    val af = adFormM
      .fill((madStub, Map.empty))
    val nameBase = s"$OFFER_K.$OFFER_K[$offerN].${bfText.name}"
    val bc = BlocksConf.DEFAULT
    val render = bfText.renderEditorField(nameBase, af, bc)(ctx)
    Ok(render)
  }


  // ============================== common-методы =================================

  /** Подготовить список категорий асинхронно. */
  private def mmCats: Future[Seq[MMartCategory]] = {
    // 2014.dec.10: Плоские категории как были, так и остались. Упрощаем работу с категориями по максимуму.
    val catOwnerId = MMartCategory.DEFAULT_OWNER_ID
    MMartCategory.findTopForOwner(catOwnerId)
  }


  /** Подготовка картинки, которая загружается в динамическое поле блока. */
  def prepareBlockImg(blockId: Int, fn: String, wsId: Option[String]) = {
    val bp = parse.multipartFormData(Multipart.handleFilePartAsTemporaryFile, maxLength = IMG_UPLOAD_MAXLEN_BYTES.toLong)
    IsAuth.async(bp) { implicit request =>
      bruteForceProtected {
        val bc = BlocksConf.applyOrDefault(blockId)
        bc.getImgFieldForName(fn) match {
          case Some(bfi) =>
            val resultFut = tempImgSupport._handleTempImg(
              preserveUnknownFmt = false,
              runEarlyColorDetector = bfi.preDetectMainColor,
              wsId   = wsId,
              ovlRrr = Some { (imgId, ctx) =>
                _bgImgOvlTpl(imgId)(ctx)
              }
            )
            resultFut

          case _ =>
            warn(s"prepareBlockImg($blockId,$fn): Unknown img field requested. 404")
            NotFound
        }
      }
    }
  }

}

