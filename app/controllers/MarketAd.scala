package controllers

import models.im.MImg
import play.api.libs.json.JsValue
import util.PlayMacroLogsImpl
import util.blocks.BlocksUtil.BlockImgMap
import util.img.MainColorDetector.{Remove, ImgBgColorUpdateAction}
import views.html.market.lk.ad._
import models._
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.FormUtil._
import play.api.data._, Forms._
import util.acl._
import scala.concurrent.Future
import play.api.mvc.{WebSocket, Request}
import play.api.Play.{current, configuration}
import MMartCategory.CollectMMCatsAcc_t
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import controllers.ad.MarketAdFormUtil
import MarketAdFormUtil._
import util.blocks.{LkEditorWsActor, BlockMapperResult}
import util.img.MainColorDetector
import io.suggest.ym.model.common.Texts4Search

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
object MarketAd extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  // Дублирующиеся куски маппина выносим за пределы метода.
  val CAT_ID_K = "catId"
  val AD_IMG_ID_K = "image_key"

  /** Сколько попыток сохранения карточки предпринимать при runtime-экзепшенах при сохранении?
    * Такие проблемы возникают при конфликте версий. */
  val SAVE_AD_RETRIES_MAX = configuration.getInt("ad.save.retries.max") getOrElse 7

  type ReqSubmit = Request[collection.Map[String, Seq[String]]]
  type DetectForm_t = Either[AdFormM, (BlockConf, AdFormM)]


  /**
   * Внутренний сборщик форм создания/редактирования рекламных карточек.
   * @param anmt Тип узла.
   * @param blockM Маппинг блока.
   * @return Форма, готовая к эксплуатации
   */
  private def getSaveAdFormM(anmt: AdNetMemberType, blockM: Mapping[BlockMapperResult]): AdFormM = {
    import AdNetMemberTypes._
    val catIdM = anmt match {
      case SHOP | RESTAURANT      => userCatIdSomeM
      case MART | RESTAURANT_SUP  => userCatIdOptM
    }
    getAdFormM(catIdM, blockM)
  }

  /**
   * Сборщик форм произвольного назначения для парсинга реквестов с данными рекламной карточки.
   * @param catIdM маппер для id категории.
   * @param blockM маппер для блоков.
   * @return Маппинг формы, готовый к эксплуатации.
   */
  def getAdFormM(catIdM: Mapping[Option[String]], blockM: Mapping[BlockMapperResult]): AdFormM = {
    Form(
      "ad" -> mapping(
        CAT_ID_K -> catIdM,
        OFFER_K  -> blockM,
        "descr"  -> richDescrOptM
      )(adFormApply)(adFormUnapply)
    )
  }


  /** Выдать маппинг ad-формы в зависимости от типа adn-узла. */
  private def detectAdnAdForm(adnNode: MAdnNode)(implicit request: ReqSubmit): DetectForm_t = {
    val anmt = adnNode.adn.memberType
    val adMode = request.body.getOrElse("ad.offer.mode", Nil)
      .headOption
      .flatMap(AdOfferTypes.maybeWithName)
      .getOrElse(AdOfferTypes.BLOCK)
    adMode match {
      case aot @ AdOfferTypes.BLOCK =>
        // Нужно раздобыть id из реквеста
        val nodeBlockIds = blockIdsFor(adnNode)
        val blockId = request.body.getOrElse("ad.offer.blockId", Nil)
          .headOption
          // Аккуратно парсим blockId ручками
          .flatMap { rawBlockId =>
            try {
              Some(rawBlockId.toInt)
            } catch {
              case ex: NumberFormatException =>
                warn("detectAdnAdForm(): Invalid block number format: " + rawBlockId)
                None
            }
          }
          // Фильтруем блокId по списку допустимых для узла.
          .filter { blockId =>
            val result = nodeBlockIds contains blockId
            if (!result)
              warn("detectAdnAdForm(): Unknown or disallowed blockId requested: " + blockId)
            result
          }
          // Если blockId был отфильтрован или отсутствовал, то берём первый допустимый id. TODO А надо это вообще?
          .getOrElse ( nodeBlockIds.head )
        val blockConf: BlockConf = BlocksConf(blockId)
        Right(blockConf -> getSaveAdFormM(anmt, blockConf.strictMapping))
    }
  }


  /**
   * Рендер унифицированной страницы добаления рекламной карточки.
   * @param adnId id узла рекламной сети.
   */
  def createAd(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import request.adnNode
    renderCreateFormWith(
      af = getSaveAdFormM(adnNode.adn.memberType, BlocksConf.DEFAULT.strictMapping),
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
    detectAdnAdForm(adnNode) match {
      // Как маппить форму - ясно. Теперь надо это сделать.
      case Right((bc, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(logPrefix + "Bind failed: \n" + formatFormErrors(formWithErrors))
            createAdFormError(formWithErrors, catOwnerId, adnNode, Some(bc))
          },
          {case (mad, bim) =>
            // Асинхронно обрабатываем всякие прочие данные.
            val saveImgsFut = bc.saveImgs(newImgs = bim, oldImgs = Map.empty, blockHeight = mad.blockMeta.height)
            val t4s2Fut = newTexts4search(mad, request.adnNode)
            val ibgcUpdFut = adPrepareUpdateBgColors(bim, bc)
            // Когда всё готово, сохраняем саму карточку.
            for {
              t4s2      <- t4s2Fut
              savedImgs <- saveImgsFut
              ibgcUpd   <- ibgcUpdFut
              adId      <- {
                mad.copy(
                  producerId    = adnId,
                  imgs          = savedImgs,
                  texts4search  = t4s2,
                  colors        = ibgcUpd.updateColors(mad.colors)
                ).save
              }
            } yield {
              Redirect(routes.MarketLkAdn.showNodeAds(adnId, newAdId = Some(adId)))
                .flashing("success" -> "Рекламная карточка создана.")
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

  /** Выдать множество допустимых id блоков в контексте узла. */
  def blockIdsFor(adnNode: MAdnNode): Set[Int] = {
    val seq1 = BlocksConf.valuesShown.map(_.id).toSet
    val ids0 = adnNode.conf.withBlocks
    if (ids0.isEmpty) {
      seq1
    } else {
      seq1 ++ ids0
    }
  }

  /** Выдать список блоков в корректном порядке: публичные + специфичные для узла. */
  private def blocksFor(adnNode: MAdnNode): Seq[BlockConf] = {
    val seq1 = BlocksConf.valuesShown
    val ids0 = adnNode.conf.withBlocks
    if (ids0.isEmpty) {
      seq1
    } else {
      val allIds = seq1.map(_.id).toSet ++ ids0
      val allBlocks = allIds
        .toSeq
        .flatMap { id =>
          try {
            Some(BlocksConf(id) : BlockConf)
          } catch {
            case ex: NoSuchElementException =>
              error(s"blocksFor(node=${adnNode.id.get}): No such block: $id, looks like node.conf.withBlocks is invalid: ${adnNode.conf.withBlocks} ;; node text: ${adnNode.meta.name} / ${adnNode.meta.town}")
              None
          }
        }
      BlocksConf.orderBlocks(allBlocks)
    }
  }


  /** Общий код рендера createShopAdTpl с запросом необходимых категорий. */
  private def renderCreateFormWith(af: AdFormM, catOwnerId: String, adnNode: MAdnNode, withBC: Option[BlockConf] = None)(implicit ctx: Context) = {
    getMMCatsForCreate(af, catOwnerId) map { mmcats =>
      createAdTpl(mmcats, af, adnNode, withBC, blocksFor(adnNode))
    }
  }


  private def renderEditFormWith(af: AdFormM)(implicit request: RequestWithAd[_]) = {
    import request.{producer, mad}
    val catOwnerId = getCatOwnerId(producer)
    getMMCatsForEdit(af, mad, catOwnerId) map { mmcats =>
      editAdTpl(mad, mmcats, af, producer, blocksFor(producer))
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
  def editAd(adId: String) = CanEditAd(adId).async { implicit request =>
    import request.mad
    val blockConf = BlocksConf.applyOrDefault(mad.blockMeta.blockId)
    val form0 = getSaveAdFormM(request.producer.adn.memberType, blockConf.strictMapping)
    val bim = mad.imgs.mapValues { mii =>
      MImg(mii.filename)
    }
    val formFilled = form0 fill ((mad, bim))
    renderEditFormWith(formFilled)
      .map { Ok(_) }
  }


  /** Сабмит формы рендера страницы редактирования рекламной карточки.
    * @param adId id рекламной карточки.
    */
  def editAdSubmit(adId: String) = CanEditAd(adId).async(parse.urlFormEncoded) { implicit request =>
    import request.mad
    detectAdnAdForm(request.producer) match {
      case Right((bc, formM)) =>
        val formBinded = formM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            debug(s"editShopAdSubmit($adId): Failed to bind form: " + formWithErrors.errors)
            renderFailedEditFormWith(formWithErrors)
          },
          {case (mad2, bim) =>
            val t4s2Fut = newTexts4search(mad2, request.producer)
            val ibgcUpdFut = adPrepareUpdateBgColors(bim, bc)
            // TODO Надо отделить удаление врЕменных и былых картинок от сохранения новых. И вызывать эти две фунции отдельно.
            // Сейчас возможна ситуация, что при поздней ошибке сохранения теряется старая картинка, а новая сохраняется вникуда.
            val saveImgsFut = bc.saveImgs(
              newImgs = bim,
              oldImgs = mad.imgs,
              blockHeight = mad2.blockMeta.height
            )
            // 2014.09.22: Обновление карточки переписано на потоко-безопасный манер с как-бы immutable-полями.
            for {
              imgsSaved <- saveImgsFut
              t4s2      <- t4s2Fut
              ibgcUpd   <- ibgcUpdFut
              _adId     <- MAd.tryUpdate(request.mad) { mad0 =>
                mad0.copy(
                  imgs          = imgsSaved,
                  texts4search  = t4s2,
                  disableReason = Nil,
                  moderation    = mad0.moderation.copy(
                    freeAdv = mad0.moderation.freeAdv
                      .filter { _.isAllowed != true }
                  ),
                  colors        = ibgcUpd.updateColors(mad2.colors),
                  offers        = mad2.offers,
                  prio          = mad2.prio,
                  userCatId     = mad2.userCatId,
                  blockMeta     = mad2.blockMeta,
                  richDescrOpt  = mad2.richDescrOpt
                )
              }
            } yield {
              Redirect(routes.MarketLkAdn.showNodeAds(mad.producerId))
                .flashing("success" -> "Изменения сохранены")
            }
          }
        )

      case Left(formWithGlobalError) =>
        val formWithErrors = formWithGlobalError.bindFromRequest()
        renderFailedEditFormWith(formWithErrors)
    }
  }

  /** Рендер окошка с подтверждением удаления рекламной карточки. */
  def deleteWnd(adId: String) = CanEditAd(adId).async { implicit request =>
    Ok(_deleteWndTpl(request.mad))
  }

  /**
   * POST для удаления рекламной карточки.
   * @param adId id рекламы.
   * @return Редирект в магазин или ТЦ.
   */
  def deleteSubmit(adId: String) = CanEditAd(adId).async { implicit request =>
    MAd.deleteById(adId) map { _ =>
      val routeCall = routes.MarketLkAdn.showNodeAds(request.mad.producerId)
      Redirect(routeCall)
        .flashing("success" -> "Рекламная карточка удалена")
    }
  }


  /**
   * Сгенерить экземпляр Text4Search на основе данных старой и новой рекламных карточек.
   * @param newMadData Забинденные данные формы для новой (будущей) рекламной карточки.
   * @param producer Нода-продьюсер рекламной карточки.
   */
  private def newTexts4search(newMadData: MAd, producer: MAdnNode): Future[Texts4Search] = {
    // Собираем названия родительских категорий:
    val catNamesFut: Future[List[String]] = {
      newMadData.userCatId.fold(Future successful List.empty[String]) { userCatId =>
        MMartCategory.foldUpChain [List[String]] (userCatId, Nil) {
          (acc, e) =>
            if (e.includeInAll) {
              e.name :: acc
            } else {
              acc
            }
        }
      }
    }
    // Узнаём название узла-продьюсера:
    // Генерим общий результат:
    for {
      catNames <- catNamesFut
    } yield {
      newMadData.texts4search.copy(
        userCat = catNames,
        producerName = Some(producer.meta.name)
      )
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
        val prodSinks = request.producer.adn.sinks
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
  private def detectReceivers(producer: MAdnNode): Future[Receivers_t] = {
    val selfRcvrIds: Seq[String] = Some(producer)
      .filter(_.adn.isReceiver)
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


  // ============================== common-методы =================================


  private def maybeAfCatId(af: AdFormM) = {
    val catIdK = "ad." + CAT_ID_K
    af(catIdK).value.filter { _ => af.errors(catIdK).isEmpty }
  }

  /** Получение списков категорий на основе формы и владельца категорий. */
  private def getMMCatsForCreate(af: AdFormM, catOwnerId: String): Future[MMartCategory.CollectMMCatsAcc_t] = {
    val catIdOpt = maybeAfCatId(af)
    getMMCatsFor(catIdOpt, catOwnerId: String)
  }


  private def getMMCatsForEdit(af: AdFormM, mad: MAd, catOwnerId: String): Future[CollectMMCatsAcc_t] = {
    val catIdOpt = maybeAfCatId(af).orElse(mad.userCatId)
    getMMCatsFor(catIdOpt, catOwnerId: String)
  }

  private def getMMCatsFor(catIdOpt: Option[String], catOwnerId: String): Future[CollectMMCatsAcc_t] = {
    catIdOpt.fold
      { // Выдать top-категории
        val mmcatsFut = MMartCategory.findTopForOwner(catOwnerId)
        MMartCategory.catsAsAccFut(mmcatsFut)
      }
      {catId =>
        // Выдать над и под-категории по отношению к указанной категории.
        val subcatsFut = MMartCategory.findDirectSubcatsOf(catId)
        val upCatsFut = MMartCategory.collectCatListsUpTo(catOwnerId=catOwnerId, currCatId=catId)
        for {
          upCats  <- upCatsFut
          subcats <- subcatsFut
        } yield {
          if (subcats.nonEmpty)
            upCats ++ List(None -> subcats)
          else
            upCats
        }
      }
  }

  /**
   * Выполнить действия, связанные с определением цвета фона и возвращение промежуточного результата для MAd.
   * @param newBim Новый набор картинок.
   * @param bc Конфиг блока.
   * @return Фьючерс с действием по обновлению карты цветов рекламной карточки.
   */
  private def adPrepareUpdateBgColors(newBim: BlockImgMap, bc: BlockConf): Future[ImgBgColorUpdateAction] = {
    bc.getBgImg(newBim).fold [Future[ImgBgColorUpdateAction]] {
      trace(s"adPrepareUpdateBgColors(): No background image - nothing to do.")
      Future successful Remove    // TODO Возвращать Keep?
    } { bgImg4s =>
      MainColorDetector.detectColorCachedFor(bgImg4s)
    }
  }

}

