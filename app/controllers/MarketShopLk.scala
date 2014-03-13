package controllers

import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import play.api.data._, Forms._
import util.FormUtil._
import util.acl._
import models._, MShop.ShopId_t, MMart.MartId_t
import views.html.market.lk.shop._
import play.api.mvc.{AnyContent, SimpleResult}
import play.api.Play.current
import concurrent.duration._
import play.api.libs.concurrent.Akka
import scala.concurrent.{Future, Promise}
import play.api.mvc.Security.username
import util.img._
import ImgFormUtil.imgIdM
import net.sf.jmimemagic.Magic

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 13:34
 * Description: Контроллер личного кабинета для арендатора, т.е. с точки зрения конкретного магазина.
 */
object MarketShopLk extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Для защиты от брутфорса ключей активации используются задержки в работе функции проверку псевдослучайного ключа активации. */
  val INVITE_CHECK_LAG_DURATION = {
    val lagMs = current.configuration.getInt("market.lk.shop.invite.accept.lag_ms") getOrElse 333
    lagMs milliseconds
  }

  /** Маркер картинки для logo-вещичек */
  val SHOP_TMP_LOGO_MARKER = "shopLogo"

  /** Форма добавления/редактирования магазина. */
  val shopM = mapping(
    "name"         -> shopNameM,
    "description"  -> publishedTextOptM,
    "mart_floor"   -> optional(martFloorM),
    "mart_section" -> optional(martSectionM)
  )
  // apply()
  {(name, description, martFloor, martSection) =>
    MShop(name=name, companyId=null, description=description, martFloor=martFloor, martSection=martSection, personIds=null)
  }
  // unapply()
  {mshop =>
    import mshop._
    Some((name, description, martFloor, martSection))
  }

  private val shopKM = "shop" -> shopM

  /** Форма на которой нельзя менять логотип. */
  val shopFormM = Form(shopKM)

  val shopWithLogoFormM = Form(tuple(
    shopKM,
    "logoImgId" -> optional(imgIdM)
  ))

  /** Ограниченный маппинг магазина. Используется редактировании оного для имитации неизменяемых полей на форме.
    * Некоторые поля не доступны для редактирования владельцу магазина, и эта форма как раз для него. */
  val limitedShopFormM = Form(tuple(
    "name"         -> shopNameM,
    "description"  -> publishedTextOptM
  ))


  /**
   * Рендер страницы магазина (с точки зрения арендатора: владельца магазина).
   * @param shopId id магазина.
   */
  def showShop(shopId: ShopId_t) = IsShopAdm(shopId).async { implicit request =>
    import request.mshop
    val adsFut = MMartAd.findForShop(shopId)
    // TODO Если магазин удалён из ТЦ, то это как должно выражаться?
    val martId = mshop.martId.get
    MMart.getById(martId).flatMap {
      case Some(mmart) =>
        adsFut.map { ads =>
          Ok(shopShowTpl(mmart, mshop, ads))
        }

      case None => martNotFound(martId)
    }
  }

  /**
   * Страница с формой редактирования магазина. Арендатору не доступны некоторые поля.
   * @param shopId id магазина.
   */
  def editShopForm(shopId: ShopId_t) = IsShopAdm(shopId).async { implicit request =>
    import request.mshop
    // TODO Если магазин удалён из ТЦ, то это как должно выражаться?
    val martId = mshop.martId.get
    MMart.getById(martId).map {
      case Some(mmart) =>
        val formBinded = shopWithLogoFormM fill (mshop, None)
        Ok(shopEditFormTpl(mmart, mshop, formBinded))

      case None => martNotFound(martId)
    }
  }

  /**
   * Сабмит формы редактирования магазина арендатором.
   * @param shopId id магазина.
   */
  def editShopFormSubmit(shopId: ShopId_t) = IsShopAdm(shopId).async { implicit request =>
    import request.mshop
    limitedShopFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editShopFormSubmit($shopId): Bind failed: " + formWithErrors.errors)
        val martId = mshop.martId.get
        MMart.getById(martId).map {
          case Some(mmart) => NotAcceptable(shopEditFormTpl(mmart, mshop, formWithErrors))
          case None        => martNotFound(martId)
        }
      },
      {case (name, description) =>
        mshop.name = name
        mshop.description = description
        mshop.save.map { _ =>
          Redirect(routes.MarketShopLk.showShop(shopId))
            .flashing("success" -> "Изменения сохранены.")
        }
      }
    )
  }

  /** Есть формы подтверждаения инвайта для зареганных и для незареганных юзеров. Они обрабатываются вместе и должны быть совместимы. */
  type InviteAcceptFormM = Form[(String, Option[String])]

  /** Маппинг формы, которая рендерится будущему владельцу магазина (незареганному),
    * когда тот проходит по ссылки из письма-инвайта. */
  val inviteAcceptAnonFormM: InviteAcceptFormM = {
    val passwordsM = tuple(
      "pw1" -> passwordM,
      "pw2" -> passwordM
    )
    .verifying("passwords.do.not.match", { pws => pws match {
      case (pw1, pw2) => pw1 == pw2
    }})
    .transform[Option[String]](
      { case (pw1, pw2) => Some(pw1) },
      { _: AnyRef =>
        // Назад пароли тут не возвращаем никогда. Форма простая, и ошибка может возникнуть лишь при вводе паролей.
        val pw = ""
        (pw, pw)
      }
    )
    Form(tuple(
      "shopName" -> shopNameM,
      "password" -> passwordsM
    ))
  }

  /** Маппинг формы, которая для уже залогиненного будущего владельца магазина, когда тот проходит по ссылке инвайта. */
  val inviteAcceptAuthFormM: InviteAcceptFormM = Form(
    "shopName" -> shopNameM
      // Чтобы сохранить совместимость с anon-формой, добавляем в маппинг пустое поле пароля с null.
      .transform(
        { shopName => (shopName, None.asInstanceOf[Option[String]]) },
        { c: (String, Option[String]) => c._1 }
      )
  )

  /**
   * Юзер (владелец магазина) приходит через ссылку в письме. Ссылка для активации содержит id'шники, и чтобы их было
   * сложнее предугадать, используется и id магазина, и id ключа, которые оба псевдослучайны и генерятся на стороне ES.
   * @param shopId id магазина.
   * @param eaId Ключ активации.
   * @return Рендер начальной формы, где можно ввести пароль.
   */
  def inviteAccept(shopId:String, eaId: String) = inviteAcceptCommon(shopId, eaId=eaId) { (eAct, mshop) => implicit request =>
    // Всё норм как бы. Но до сабмита нельзя менять состояние - просто рендерим форму для активации учетки.
    // Можно также продлить жизнь записи активации. Но сценарий нужности этого маловероятен.
    val formUsedM = if (request.isAuth) inviteAcceptAuthFormM else inviteAcceptAnonFormM
    val formBindedM = formUsedM fill (mshop.name, None)
    Ok(invite.inviteAcceptFormTpl(mshop, eAct, formBindedM, withRegister = !request.isAuth))
  }

  /**
   * Юзер сабмиттит форму инвайт-регистрации магазина.
   * @param shopId id магазина
   * @param eaId ключ активации.
   * @return Страницу с ошибкой, или редирект в личный кабинет.
   */
  def inviteAcceptSubmit(shopId: String, eaId: String) = inviteAcceptCommon(shopId, eaId=eaId) { (eAct, mshop) => implicit request =>
    val formBindM = if (request.isAuth) inviteAcceptAuthFormM else inviteAcceptAnonFormM
    lazy val logPrefix = s"inviteAcceptSubmit($shopId, eaId=$eaId): pwOpt=${request.pwOpt}: "
    formBindM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind invite form: " + formWithErrors.errors)
        NotAcceptable(invite.inviteAcceptFormTpl(mshop, eAct, formWithErrors, withRegister = !request.isAuth))
      },
      {case (shopName, passwordOpt) =>
        // Сначала удаляем запись об активации, убедившись что она не была удалена асинхронно.
        // TODO Нужно отработать ситуацию, когда юзер сабмиттит форму резко несколько раз. Может удалять с задержкой? Тогда есть риск создания нескольких юзеров разом.
        val eActDeleteFut = eAct.delete
        eActDeleteFut flatMap { isDeleted =>
          if (isDeleted)
            trace(logPrefix + "eAct record deleted successfully: " + eAct)
          else
            warn(logPrefix + "eAct record has been already deleted. Suppressing error.")
          // Если юзер не зареган, то создать MPerson залогинить
          val newPersonIdOptFut: Future[Option[String]] = if (!request.isAuth) {
            MPerson(lang = lang.code).save flatMap { personId =>
              EmailPwIdent.applyWithPw(email = eAct.email, personId=personId, password = passwordOpt.get, isVerified = true)
                .save
                .map { emailPwIdentId => Some(personId) }
            }
          } else {
            Future successful None
          }
          // Для обновления полей MShop требуется доступ к personId. Дожидаемся сохранения юзера...
          newPersonIdOptFut flatMap { personIdOpt =>
            // Одновременно, следует обновить название магазина
            mshop.name = shopName
            // Выставляем текущего юзера как единственного и главного владельца магазина.
            val personId: String = (personIdOpt orElse request.pwOpt.map(_.personId)).get
            mshop.personIds = List(personId)
            mshop.save map { _shopId =>
              trace(logPrefix + s"mshop(id=${_shopId}) saved with new owner: $personId")
              Redirect(routes.MarketShopLk.showShop(shopId))
                .flashing("success" -> "Регистрация завершена успешно.")
                .withSession(username -> personId)
            }
          }
        }
      }
    )
  }

  /**
   * Общий код работы inviteAccept-экшенов вынесен сюда для композиции.
   * @param shopId id магазина
   * @param eaId ключ
   * @param f функция генерации результата, когда всё ок.
   * @return То, что нагенерит функция или страницу с ошибкой.
   */
  private def inviteAcceptCommon(shopId: String, eaId: String)(f: (EmailActivation, MShop) => AbstractRequestWithPwOpt[AnyContent] => Future[SimpleResult]) = {
    MaybeAuth.async { implicit request =>
      bruteForceProtect flatMap { _ =>
        EmailActivation.getById(eaId) flatMap {
          case Some(eAct) if eAct.key == shopId =>
            MShop.getById(shopId) flatMap {
              case Some(mshop) => f(eAct, mshop)(request)
              case None =>
                // should never occur
                error(s"inviteAcceptCommon($shopId, eaId=$eaId): Shop not found, but code for shop exist. This should never occur.")
                NotFound(invite.inviteInvalidTpl("shop.not.found"))
            }

          case other =>
            // Неверный код активации или id магазина. Если None, то код скорее всего истёк. Либо кто-то брутфорсит.
            debug(s"inviteAcceptCommon($shopId, eaId=$eaId): Invalid activation code (eaId): code not found. Expired?")
            // TODO Надо проверить, есть ли у юзера права на магазин, и если есть, то значит юзер дважды засабмиттил форму, и надо его сразу отредиректить в его магазин.
            // TODO Может и быть ситуация, что юзер всё ещё не залогинен, а второй сабмит уже тут. Нужно это тоже как-то обнаруживать. Например через временную сессионную куку из формы.
            warn(s"TODO I need to handle already activated requests!!!")
            NotFound(invite.inviteInvalidTpl("shop.activation.expired.or.invalid.code"))
        }
      }
    }
  }

  /** Система асинхронного платформонезависимого противодействия брутфорс-атакам. */
  // TODO Надо вынести её код в util.
  private def bruteForceProtect: Future[_] = {
    // Для противодействию брутфорсу добавляем асинхронную задержку выполнения проверки по методике https://stackoverflow.com/a/17284760
    // TODO Нужно лимитировать попытки по IP клиента. ip можно закидывать в cache с коротким ttl.
    val lagPromise = Promise[Unit]()
    Akka.system.scheduler.scheduleOnce(INVITE_CHECK_LAG_DURATION) {
      lagPromise.success()
    }
    lagPromise.future
  }


  /**
   * Загрузка картинки для логотипа магазина. Права на доступ к магазину проверяем просто для галочки.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleShopTempLogo(shopId: String) = IsShopAdm(shopId)(parse.multipartFormData) { implicit request =>
    request.body.file("picture") match {
      case Some(pictureFile) =>
        val fileRef = pictureFile.ref
        val srcFile = fileRef.file
        // Если на входе png/gif, то надо эти форматы выставить в outFmt. Иначе jpeg.
        val srcMagicMatch = Magic.getMagicMatch(srcFile, false)
        val outFmt = OutImgFmts.forImageMime(srcMagicMatch.getMimeType)
        val mptmp = MPictureTmp.getForTempFile(fileRef, outFmt, Some(SHOP_TMP_LOGO_MARKER))
        try {
          ShopLogoImageUtil.convert(srcFile, mptmp.file)
          Ok(Img.jsonTempOk(mptmp.filename))
        } catch {
          case ex: Throwable =>
            debug(s"ImageMagick crashed on file $srcFile ; orig: ${pictureFile.filename} :: ${pictureFile.contentType} [${srcFile.length} bytes]", ex)
            val reply = Img.jsonImgError("Unsupported picture format.")
            BadRequest(reply)
        } finally {
          srcFile.delete()
        }

      case None =>
        val reply = Img.jsonImgError("Picture not found in request.")
        NotAcceptable(reply)
    }
  }


  private def martNotFound(martId: MartId_t) = NotFound("mart not found: " + martId)  // TODO Нужно дергать 404-шаблон.
  private def shopNotFound(shopId: ShopId_t) = NotFound("Shop not found: " + shopId)

}
