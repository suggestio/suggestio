package controllers

import views.html.market.lk.ad._
import models._
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.FormUtil._
import play.api.data._, Forms._
import util.acl._
import scala.concurrent.Future
import play.api.mvc.Request
import play.api.Play.current
import MMartCategory.CollectMMCatsAcc_t
import io.suggest.ym.ad.ShowLevelsUtil
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import controllers.ad.MarketAdFormUtil
import MarketAdFormUtil._
import util.blocks.BlockMapperResult
import util.img.{ImgInfo4Save, OrigImgIdKey}
import io.suggest.ym.model.common.Texts4Search

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
object MarketAd extends SioController with TempImgSupport {

  import LOGGER._

  // Дублирующиеся куски маппина выносим за пределы метода.
  val CAT_ID_K = "catId"
  val AD_IMG_ID_K = "image_key"


  private val shopCatIdKM = CAT_ID_K -> userCatIdOptM.verifying(_.isDefined)

  /** Дефолтовый блок, используемый редакторами форм. */
  protected[controllers] def dfltBlock = BlocksConf.Block1

  /** Генератор форм добавления/редактирования рекламируемого продукта в зависимости от вкладок. */
  private def getShopAdFormM(blockM: Mapping[BlockMapperResult]): AdFormM = Form(
    "ad" -> mapping(
      shopCatIdKM,
      OFFER_K -> blockM
    )(adFormApply)(adFormUnapply)
  )


  type ReqSubmit = Request[collection.Map[String, Seq[String]]]
  type DetectForm_t = Either[AdFormM, (BlockConf, AdFormM)]


  /** Выдать маппинг ad-формы в зависимости от типа adn-узла. */
  private def detectAdnAdForm(anmt: AdNetMemberType)(implicit request: ReqSubmit): DetectForm_t = {
    val adMode = (request.body.get("ad.offer.mode") getOrElse Nil)
      .headOption
      .flatMap(AdOfferTypes.maybeWithName)
      .getOrElse(AdOfferTypes.BLOCK)
    adMode match {
      case aot @ AdOfferTypes.BLOCK =>
        // Нужно раздобыть id из реквеста
        val blockId = (request.body.get("ad.offer.blockId") getOrElse Nil)
          .headOption
          .fold(1)(_.toInt)
        val blockConf: BlockConf = BlocksConf(blockId)
        Right(blockConf -> getAdFormM(anmt, blockConf.strictMapping))
    }
  }


  /**
   * Рендер унифицированной страницы добаления рекламной карточки.
   * @param adnId id узла рекламной сети.
   */
  def createAd(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import request.adnNode
    renderCreateFormWith(
      af = getAdFormM(adnNode.adn.memberType, dfltBlock.strictMapping),
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
  private def createAdFormError(formWithErrors: AdFormM, catOwnerId: String, adnNode: MAdnNode, withBC: Option[BlockConf])(implicit ctx: util.Context) = {
    renderCreateFormWith(formWithErrors, catOwnerId, adnNode, withBC)
      .map(NotAcceptable(_))
  }

  /** Сабмит формы добавления рекламной карточки товара/скидки.
    * @param adnId id магазина.
    */
  def createAdSubmit(adnId: String) = IsAdnNodeAdmin(adnId).async(parse.urlFormEncoded) { implicit request =>
    import request.adnNode
    val catOwnerId = getCatOwnerId(adnNode)
    lazy val logPrefix = s"createAdSubmit($adnId): "
    detectAdnAdForm(adnNode.adn.memberType) match {
      // Как маппить форму - ясно. Теперь надо это сделать.
      case Right((bc, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(logPrefix + "Bind failed: \n" + formatFormErrors(formWithErrors))
            createAdFormError(formWithErrors, catOwnerId, adnNode, Some(bc))
          },
          {case (mad, bim) =>
            val t4s2Fut = newTexts4search(mad)
            // Асинхронно обрабатываем логотип.
            bc.saveImgs(newImgs = bim, oldImgs = Map.empty) flatMap { savedImgs =>
              mad.producerId = adnId
              mad.imgs = savedImgs
              t4s2Fut flatMap { t4s2 =>
                mad.texts4search = t4s2
                // Сохранить изменения в базу
                mad.save.map { adId =>
                  Redirect(routes.MarketLkAdn.showAdnNode(adnId, newAdId = Some(adId)))
                    .flashing("success" -> "Рекламная карточка создана.")
                }
              }
            }
          }
        )

      // Не ясно, как именно надо биндить тело реквеста на маппинг формы.
      case Left(formWithGlobalError) =>
        warn(logPrefix + "AD mode is undefined or invalid. Returning form back.")
        val formWithErrors = formWithGlobalError.bindFromRequest()
        createAdFormError(formWithErrors, catOwnerId, adnNode, withBC = None)
    }
  }

  /** Общий код рендера createShopAdTpl с запросом необходимых категорий. */
  private def renderCreateFormWith(af: AdFormM, catOwnerId: String, adnNode: MAdnNode, withBC: Option[BlockConf] = None)(implicit ctx: Context) = {
    getMMCatsForCreate(af, catOwnerId) map { mmcats =>
      createAdTpl(mmcats, af, adnNode, withBC)
    }
  }


  private def topCatsAsAcc(catOwnerId: String): Future[CollectMMCatsAcc_t] = {
    MMartCategory.findTopForOwner(catOwnerId) map {
      topCats => List(None -> topCats)
    }
  }
  

  /** Выдать над и под-категории по отношению к указанной категории. */
  private def nearCatsList(catOwnerId: String, catId: String): Future[CollectMMCatsAcc_t] = {
    val subcatsFut = MMartCategory.findDirectSubcatsOf(catId)
    for {
      upCats  <- MMartCategory.collectCatListsUpTo(catOwnerId=catOwnerId, currCatId=catId)
      subcats <- subcatsFut
    } yield {
      if (!subcats.isEmpty)
        upCats ++ List(None -> subcats)
      else
        upCats
    }
  }


  private def renderEditFormWith(af: AdFormM)(implicit request: RequestWithAd[_]) = {
    import request.{producer, mad}
    val catOwnerId = getCatOwnerId(producer)
    getMMCatsForEdit(af, mad, catOwnerId) map { mmcats =>
      editAdTpl(mad, mmcats, af, producer)
    }
  }


  private def renderFailedEditFormWith(af: AdFormM)(implicit request: RequestWithAd[_]) = {
    renderEditFormWith(af) map {
      NotAcceptable(_)
    }
  }

  /** Рендер страницы с формой редактирования рекламной карточки магазина.
    * @param adId id рекламной карточки.
    */
  def editAd(adId: String) = IsAdEditor(adId).async { implicit request =>
    import request.mad
    val blockConf: BlockConf = BlocksConf.apply(mad.blockMeta.blockId)
    val form0 = getAdFormM(request.producer.adn.memberType, blockConf.strictMapping)
    val bim = mad.imgs.mapValues { mii =>
      val oiik = OrigImgIdKey(filename = mii.filename, meta = mii.meta)
      ImgInfo4Save(oiik)
    }
    val formFilled = form0 fill ((mad, bim))
    renderEditFormWith(formFilled)
      .map { Ok(_) }
  }

  /** Импортировать выхлоп маппинга формы в старый экземпляр рекламы. Этот код вызывается во всех editAd-экшенах. */
  private def importFormAdData(oldMad: MAd, newMad: MAd) {
    oldMad.offers = newMad.offers
    oldMad.prio = newMad.prio
    oldMad.userCatId = newMad.userCatId
    oldMad.blockMeta = newMad.blockMeta
    oldMad.colors = newMad.colors
  }

  /** Сабмит формы рендера страницы редактирования рекламной карточки.
    * @param adId id рекламной карточки.
    */
  def editAdSubmit(adId: String) = IsAdEditor(adId).async(parse.urlFormEncoded) { implicit request =>
    import request.mad
    detectAdnAdForm(request.producer.adn.memberType) match {
      case Right((bc, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(s"editShopAdSubmit($adId): Failed to bind form: " + formWithErrors.errors)
            renderFailedEditFormWith(formWithErrors)
          },
          {case (mad2, bim) =>
            val t4s2Fut = newTexts4search(mad2)
            bc.saveImgs(newImgs = bim, oldImgs = mad.imgs) flatMap { imgsSaved =>
              mad.imgs = imgsSaved
              importFormAdData(oldMad = mad, newMad = mad2)
              t4s2Fut flatMap { t4s2 =>
                mad.texts4search = t4s2
                mad.disableReason = None
                mad.save.map { _ =>
                  Redirect(routes.MarketLkAdn.showAdnNode(mad.producerId))
                    .flashing("success" -> "Изменения сохранены")
                }
              }
            }
          }
        )

      case Left(formWithGlobalError) =>
        val formWithErrors = formWithGlobalError.bindFromRequest()
        renderFailedEditFormWith(formWithErrors)
    }
  }

  /**
   * POST для удаления рекламной карточки.
   * @param adId id рекламы.
   * @return Редирект в магазин или ТЦ.
   */
  def deleteSubmit(adId: String) = IsAdEditor(adId).async { implicit request =>
    MAd.deleteById(adId) map { _ =>
      val routeCall = routes.MarketLkAdn.showAdnNode(request.mad.producerId)
      Redirect(routeCall)
        .flashing("success" -> "Рекламная карточка удалена")
    }
  }


  /**
   * Залить в mad.text4search новые данные, в частности по категориям.
   * @param mad Изменяемый инстанс рекламной карточки.
   */
  private def newTexts4search(mad: MAd): Future[Texts4Search] = {
    mad.userCatId.fold(Future successful mad.texts4search) { userCatId =>
      MMartCategory.foldUpChain [List[String]] (userCatId, Nil) {
        (acc, e) =>
          if (e.includeInAll)
            e.name :: acc
          else
            acc
      } map { ucats =>
        mad.texts4search.copy(userCat = ucats)
      }
    }
  }


  // ===================================== ad show levels =============================================

  /** Форма для маппинга результатов  */
  private val adShowLevelFormM: Form[(AdShowLevel, Boolean)] = Form(tuple(
    // id уровня, прописано в чекбоксе
    "levelId" -> nonEmptyText(maxLength = 1)
      .transform [Option[AdShowLevel]] (
        { AdShowLevels.maybeWithName },
        { case Some(sl) => sl.toString
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
  def updateShowLevelSubmit(adId: String) = IsAdEditor(adId).async { implicit request =>
    lazy val logPrefix = s"updateShowLevelSubmit($adId): "
    adShowLevelFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formWithErrors.errors)
        NotAcceptable("Request body invalid.")
      },
      {case (levelId, isLevelEnabled) =>
        import request.mad
        // Бывает, что ресиверы ещё не выставлены. Тогда нужно найти получателя и вписать его сразу.
        val additionalReceiversFut: Future[Receivers_t] = if (mad.receivers.isEmpty) {
          val rcvrsFut = detectReceivers(request.producer)
          rcvrsFut onSuccess {
            case result =>
              debug(logPrefix + "No receivers found in Ad. Generated new receivers map: " + result.valuesIterator.mkString("[", ", ", "]"))
          }
          rcvrsFut
        } else {
          Future successful Map.empty
        }
        additionalReceiversFut flatMap { addRcvrs =>
          mad.receivers ++= addRcvrs
          // Нужно, чтобы настройки отображения также повлияли на выдачу:
          val slUpdF: Set[AdShowLevel] => Set[AdShowLevel] = if (isLevelEnabled) {
            { asl => asl + levelId }
          } else {
            { asl => asl - levelId }
          }
          mad.updateAllWantLevels(slUpdF)
          mad.applyOutputConstraintsFor(request.producer) flatMap { appliedAds =>
            ShowLevelsUtil.saveAllReceivers(appliedAds)
          } map { _ =>
            Ok("Updated ok.")
          }
        }
      }
    )
  }


  /** Детектор получателей рекламы. Заглядывает к себе и к прямому родителю, если он указан. */
  private def detectReceivers(producer: MAdnNode): Future[Receivers_t] = {
    val supRcvrIdsFut: Future[Seq[String]] = producer.adn.supId
      .map { supId =>
        MAdnNodeCache.getByIdCached(supId)
          .map { _.filter(_.adn.isReceiver).map(_.idOrNull).toSeq }
      } getOrElse {
        Future successful Nil
      }
    val selfRcvrIds: Seq[String] = Some(producer)
      .filter(_.adn.isReceiver)
      .map(_.idOrNull)
      .toSeq
    supRcvrIdsFut map { supRcvrIds =>
      val rcvrIds: Seq[String] = supRcvrIds ++ selfRcvrIds
      rcvrIds.distinct.map { rcvrId =>
        rcvrId -> AdReceiverInfo(rcvrId)
      }.toMap
    }
  }


  // ============================ ТЦ ================================

  private val martCatIdKM = CAT_ID_K -> userCatIdOptM
  /** Генератор форм добавления/редактирования рекламиры в ТЦ в зависимости от вкладок.
    * Категория не обязательная, логотип от ТЦ. */
  private def getMartAdFormM(blockM: Mapping[BlockMapperResult]): AdFormM = Form(
    "ad" -> mapping(
      martCatIdKM,
      OFFER_K -> blockM
    )(adFormApply)(adFormUnapply)
  )


  // ============================== common-методы =================================

  private def getAdFormM(anmt: AdNetMemberType, blockM: Mapping[BlockMapperResult]): AdFormM = {
    import AdNetMemberTypes._
    anmt match {
      case SHOP | RESTAURANT      => getShopAdFormM(blockM)
      case MART | RESTAURANT_SUP  => getMartAdFormM(blockM)
    }
  }


  private def maybeAfCatId(af: AdFormM) = {
    val catIdK = "ad." + CAT_ID_K
    af(catIdK).value.filter { _ => af.errors(catIdK).isEmpty }
  }

  /** Получение списков категорий на основе формы и владельца категорий. */
  private def getMMCatsForCreate(af: AdFormM, catOwnerId: String): Future[MMartCategory.CollectMMCatsAcc_t] = {
    val catIdOpt = maybeAfCatId(af)
    catIdOpt match {
      case Some(catId) =>
        nearCatsList(catOwnerId=catOwnerId, catId=catId)
          .filter { !_.isEmpty }
          .recoverWith { case ex: NoSuchElementException => topCatsAsAcc(catOwnerId) }

      case None => topCatsAsAcc(catOwnerId)
    }
  }


  private def getMMCatsForEdit(af: AdFormM, mad: MAd, catOwnerId: String): Future[CollectMMCatsAcc_t] = {
    maybeAfCatId(af).orElse(mad.userCatId) match {
      case Some(catId) => nearCatsList(catOwnerId=catOwnerId, catId=catId)
      case None => topCatsAsAcc(catOwnerId)
    }
  }


}

