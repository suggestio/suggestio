package controllers

import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import play.api.data._, Forms._
import util.FormUtil._
import util.acl._
import models._
import views.html.market.lk.shop._
import play.api.mvc.{RequestHeader, AnyContent, Result}
import play.api.Play.current
import concurrent.duration._
import scala.concurrent.Future
import play.api.mvc.Security.username
import util.img._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 13:34
 * Description: Контроллер личного кабинета для арендатора, т.е. с точки зрения конкретного магазина.
 */
object MarketShopLk extends SioController with PlayMacroLogsImpl with BruteForceProtect with ShopMartCompat {

  import LOGGER._

  /** Для защиты от брутфорса ключей активации используются задержки в работе функции проверку псевдослучайного ключа активации. */
  override val INVITE_CHECK_LAG_DURATION = {
    val lagMs = current.configuration.getInt("market.lk.shop.invite.accept.lag_ms") getOrElse 333
    lagMs milliseconds
  }

  /** Маркер картинки для logo-вещичек */
  val SHOP_TMP_LOGO_MARKER = "shopLogo"

  /** Метаданные магазина, которые может редактировать владелец магазина. */
  val shopMetaFullM = mapping(
    "name"          -> shopNameM,
    "description"   -> publishedTextOptM,
    "floor"         -> optional(floorM),
    "section"       -> optional(sectionM)
  )
  {(name, descr, floor, section) =>
    AdnMMetadata(
      name = name,
      description = descr,
      floor = floor,
      section = section
    )
  }
  {adnMeta =>
    import adnMeta._
    Some((name, description, floor, section))
  }
  val shopMetaFullKM = "meta" -> shopMetaFullM


  /** Маппер для необязательного логотипа магазина. */
  val logoImgOptIdKM = ImgFormUtil.getLogoKM("shop.logo.invalid", marker=SHOP_TMP_LOGO_MARKER)

  /** Форма для заполнения страницы, но НЕ для сабмита. */
  val shopFullFormM = Form(tuple(
    "email" -> optional(email),
    shopMetaFullKM,
    logoImgOptIdKM
  ))


  /** Ограниченный маппинг доступен владельцу магазина внутри ТЦ. */
  val shopMetaLimitedM = mapping(
    "name"          -> shopNameM,
    "description"   -> publishedTextOptM
  )
  {(name, descr) =>
    AdnMMetadata(
      name = name,
      description = descr
    )
  }
  {adnMeta =>
    import adnMeta._
    Some((name, description))
  }

  /** Ограниченный маппинг магазина. Используется при сабмите редактирования профиля магазина для имитации
    * неизменяемых полей на форме. Некоторые поля не доступны для редактирования владельцу магазина. */
  val limitedShopFormM = Form(tuple(
    // Вложенный маппинг для совместимости с исходным шаблоном.
    "meta" -> shopMetaLimitedM,
    logoImgOptIdKM
  ))


  /** Асинхронно заполнить full форму с помощью указанного магазина. */
  def fillFullForm(mshop: MAdnNode) = {
    val shopOwnerEmailFut: Future[Option[String]] = mshop.mainPersonId match {
      case Some(personId) =>
        MPersonIdent.findAllEmails(personId)
          .map { _.headOption }

      // Нет почты, магазин не активирован. Но юзеру надо что-то отобразить, поэтому ищем в активациях.
      case None =>
        EmailActivation.findByKey(mshop.id.get)
          .map { _.headOption.map(_.email) }
    }
    val imgId = mshop.logoImgOpt.map { img =>
      ImgInfo4Save(ImgFormUtil.imgInfo2imgKey(img))
    }
    shopOwnerEmailFut map { shopOwnerEmail =>
      shopFullFormM fill (shopOwnerEmail, mshop.meta, imgId)
    }
  }


  /** Страница с формой редактирования магазина. Арендатору не доступны некоторые поля. */
  def editShopForm(implicit request: AbstractRequestForAdnNodeAdm[_]): Future[Result] = {
    import request.adnNode
    // TODO Если магазин удалён из рекламной сети или не имеет своего ТЦ, то это как должно выражаться?
    val martId = adnNode.adn.supId.get
    val formBindedFut = fillFullForm(adnNode)
    getMartByIdCache(martId) flatMap {
      case Some(mmart) =>
        formBindedFut map { formBinded =>
          Ok(shopEditFormTpl(mmart, adnNode, formBinded))
        }

      case None => martNotFound(martId)
    }
  }

  /** Сабмит формы редактирования магазина арендатором. */
  def editShopFormSubmit(implicit request: AbstractRequestForAdnNodeAdm[_]): Future[Result] = {
    import request.adnNode
    limitedShopFormM.bindFromRequest().fold(
      {formWithErrors =>
        val fullFormBindedFut = fillFullForm(adnNode) map { _.bindFromRequest }
        debug(s"editShopFormSubmit(${adnNode.id.get}}): Bind failed: " + formWithErrors.errors)
        // TODO Что делать, если у магазина нет своего супервизора?
        val martId = adnNode.adn.supId.get
        getMartByIdCache(martId) flatMap {
          case Some(mmart) =>
            fullFormBindedFut map { formWithErrors2 =>
              NotAcceptable(shopEditFormTpl(mmart, adnNode, formWithErrors2))
            }
          case None        => martNotFound(martId)
        }
      },
      {case (meta, logoImgIdOpt) =>
        val updateImgsFut = ImgFormUtil.updateOrigImg(
          needImgs = logoImgIdOpt,
          oldImgs  = adnNode.logoImgOpt
        )
        adnNode.meta.name = meta.name
        adnNode.meta.description = meta.description
        // Для обновления shop'а надо дождаться генерации нового id логотипа.
        updateImgsFut.flatMap { newImgInfo =>
          adnNode.logoImgOpt = newImgInfo
          adnNode.save map { _shopId =>
            Redirect(routes.MarketLkAdn.showAdnNode(adnNode.id.get))
              .flashing("success" -> "Изменения сохранены.")
          }
        }
      }
    )
  }

  /** Есть формы подтверждаения инвайта для зареганных и для незареганных юзеров. Они обрабатываются вместе и должны быть совместимы. */
  type InviteAcceptFormM = Form[(String, Option[String])]

  /** Маппинг формы, которая рендерится будущему владельцу магазина (незареганному),
    * когда тот проходит по ссылки из письма-инвайта. */
  val inviteAcceptAnonFormM: InviteAcceptFormM = {
    Form(tuple(
      "shopName" -> shopNameM,
      "password" -> passwordWithConfirmSomeM
    ))
  }

  /** Маппинг формы, которая для уже залогиненного будущего владельца магазина, когда тот проходит по ссылке инвайта. */
  val inviteAcceptAuthFormM: InviteAcceptFormM = Form(
    "shopName" -> shopNameM
      // Чтобы сохранить совместимость с anon-формой, добавляем в маппинг пустое поле пароля с None вместо нового пароля.
      .transform(
        { shopName => (shopName, Option.empty[String]) },
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
    val formBindedM = formUsedM.fill((mshop.meta.name, None))
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
            mshop.meta.name = shopName
            // Выставляем текущего юзера как единственного и главного владельца магазина.
            val personId: String = (personIdOpt orElse request.pwOpt.map(_.personId)).get
            mshop.personIds = Set(personId)
            mshop.save map { _shopId =>
              trace(logPrefix + s"mshop(id=${_shopId}) saved with new owner: $personId")
              Redirect(routes.MarketLkAdn.showAdnNode(shopId))
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
  private def inviteAcceptCommon(shopId: String, eaId: String)(f: (EmailActivation, MAdnNode) => AbstractRequestWithPwOpt[AnyContent] => Future[Result]) = {
    MaybeAuth.async { implicit request =>
      bruteForceProtect flatMap { _ =>
        EmailActivation.getById(eaId) flatMap {
          case Some(eAct) if eAct.key == shopId =>
            getShopByIdCache(shopId) flatMap {
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


  private def martNotFound(martId: String)(implicit request: RequestHeader) = http404AdHoc

}
