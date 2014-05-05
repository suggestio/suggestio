package controllers

import util.img._
import ImgFormUtil.imgInfo2imgKey
import util.PlayMacroLogsImpl
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import scala.concurrent.Future
import views.html.market.lk.adn._
import io.suggest.ym.model.MAdnNode
import play.api.data.Form
import play.api.data.Forms._
import util.FormUtil._
import scala.util.Success
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.14 10:22
 * Description: Редактирование узлов рекламной сети скрывается за парой экшенов, которые в зависимости от типов
 * узлов делают те или иные действия.
 * Супервайзер ресторанной сети и ТЦ имеют одну форму и здесь обозначаются как "узлы-лидеры".
 */
object MarketLkAdnEdit extends SioController with PlayMacroLogsImpl with TempImgSupport {

  import LOGGER._

  /** Маркер картинки для использования в качестве логотипа. */
  val LEADER_TMP_LOGO_MARKER = "leadLogo"

  val WELCOME_IMG_KEY = "wlcm"

  /** Страница с формой редактирования узла рекламной сети. Функция смотрит тип узла и рендерит ту или иную страницу. */
  def editAdnNode(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import AdNetMemberTypes._
    request.adnNode.adn.memberType match {
      case MART | RESTAURANT | RESTAURANT_SUP =>
        editAdnLeader
      case SHOP => MarketShopLk.editShopForm
    }
  }


  /** Сабмит формы редактирования узла рекламной сети. Функция смотрит тип узла рекламной сети и использует
    * тот или иной хелпер. */
  def editAdnNodeSubmit(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import AdNetMemberTypes._
    request.adnNode.adn.memberType match {
      case MART | RESTAURANT | RESTAURANT_SUP => editAdnLeaderSubmit
      case SHOP => MarketShopLk.editShopFormSubmit
    }
  }


  /**
   * Загрузка картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleTempLogo(adnId: String) = IsAdnNodeAdmin(adnId)(parse.multipartFormData) { implicit request =>
    import AdNetMemberTypes._
    request.adnNode.adn.memberType match {
      // TODO Может пора выпилить это разделение на сущности?
      case MART | RESTAURANT_SUP | RESTAURANT =>
        _handleTempImg(MartLogoImageUtil, Some(LEADER_TMP_LOGO_MARKER))
      case SHOP =>
        _handleTempImg(AdnLogoImageUtil, Some(MarketShopLk.SHOP_TMP_LOGO_MARKER))
    }
  }


  /** Маппер для метаданных. */
  private val leaderMetaM = mapping(
    "name"      -> nameM,
    "town"      -> toStrOptM(townM),
    "address"   -> toStrOptM(martAddressM),
    "siteUrl"   -> urlStrOptM,
    "phone"     -> phoneOptM
  )
  {(name, town, address, siteUrlOpt, phoneOpt) =>
    AdnMMetadata(
      name    = name,
      town    = town,
      address = address,
      siteUrl = siteUrlOpt,
      phone   = phoneOpt
    )
  }
  {meta =>
    import meta._
    Some((name, town, address, siteUrl, phone)) }


  /** Маппер для необязательного логотипа магазина. */
  private val rcvrLogoImgIdOptKM = ImgFormUtil.getLogoKM("adn.rcvr.logo.invalid", marker=LEADER_TMP_LOGO_MARKER)

  /** Маппинг для формы добавления/редактирования торгового центра. */
  private val martFormM = Form(tuple(
    "meta" -> leaderMetaM,
    "welcomeImgId" -> optional(ImgFormUtil.imgIdJpegM),
    rcvrLogoImgIdOptKM
  ))


  /** Асинхронно получить welcome-ad-карточку. */
  private def getWelcomeAdOpt(mmart: MAdnNode): Future[Option[MWelcomeAd]] = {
    mmart.meta.welcomeAdId
      .fold [Future[Option[MWelcomeAd]]] (Future successful None) (MWelcomeAd.getById)
  }


  /** Страница с формой редактирования узла-лидера. */
  private def editAdnLeader(implicit request: AbstractRequestForAdnNodeAdm[_]): Future[Result] = {
    import request.adnNode
    getWelcomeAdOpt(adnNode) map { welcomeAdOpt =>
      val martLogoOpt = adnNode.logoImgOpt.map { img =>
        ImgInfo4Save(imgInfo2imgKey(img))
      }
      val welcomeImgKey = welcomeAdOpt.flatMap { _.imgs.headOption }.map[OrigImgIdKey] { img => img._2 }
      val formFilled = martFormM.fill((adnNode.meta, welcomeImgKey, martLogoOpt))
      Ok(leaderEditFormTpl(adnNode, formFilled, welcomeAdOpt))
    }
  }


  /** Сабмит формы редактирования узла-лидера. */
  private def editAdnLeaderSubmit(implicit request: AbstractRequestForAdnNodeAdm[_]): Future[Result] = {
    import request.adnNode
    martFormM.bindFromRequest().fold(
      {formWithErrors =>
        val welcomeAdOptFut = getWelcomeAdOpt(adnNode)
        debug(s"martEditFormSubmit(${adnNode.id.get}): Failed to bind form: ${formatFormErrors(formWithErrors)}")
        welcomeAdOptFut map { welcomeAdOpt =>
          NotAcceptable(leaderEditFormTpl(adnNode, formWithErrors, welcomeAdOpt))
            .flashing("error" -> "Ошибка заполнения формы.")
        }
      },
      {case (adnMeta, welcomeImgOpt, logoImgIdOpt) =>
        // В фоне обновляем логотип ТЦ
        val savedLogoFut = ImgFormUtil.updateOrigImg(logoImgIdOpt, oldImgs = adnNode.logoImgOpt)
        // В фоне обновляем картинку карточки-приветствия.
        val savedWelcomeImgsFut: Future[_] = getWelcomeAdOpt(adnNode) flatMap { welcomeAdOpt =>
          ImgFormUtil.updateOrigImg(
            needImgs = welcomeImgOpt.map(ImgInfo4Save(_, withThumb = false)),
            oldImgs = welcomeAdOpt.flatMap(_.imgs.headOption).map(_._2)
          ) flatMap { savedImgs =>
            savedImgs.headOption match {
              // Новой картинки нет. Надо удалить старую карточку (если была), и очистить соотв. welcome-поле.
              case None =>
                val deleteOldAdFut = adnNode.meta.welcomeAdId
                  .fold [Future[_]] {Future successful ()} { MAd.deleteById }
                adnNode.meta.welcomeAdId = None
                deleteOldAdFut

              // Новая картинка есть. Пора обновить текущую карточук, или новую создать.
              case newImgInfoOpt @ Some(newImgInfo) =>
                val imgs = Map(WELCOME_IMG_KEY -> newImgInfo)
                val welcomeAd = welcomeAdOpt
                  .map { welcomeAd =>
                  welcomeAd.imgs = imgs
                  welcomeAd
                } getOrElse {
                  MWelcomeAd(producerId = adnNode.id.get, imgs = imgs)
                }
                welcomeAd.save andThen {
                  case Success(welcomeAdId) =>
                    adnNode.meta.welcomeAdId = Some(welcomeAdId)
                }
            }
          }
        }
        adnNode.meta = adnMeta
        savedLogoFut.flatMap { savedLogos =>
          adnNode.logoImgOpt = savedLogos.headOption
          savedWelcomeImgsFut flatMap { _ =>
            adnNode.save.map { _ =>
              Redirect(routes.MarketLkAdn.showAdnNode(adnNode.id.get))
                .flashing("success" -> "Изменения сохранены.")
            }
          }
        }
      }
    )
  }


}
