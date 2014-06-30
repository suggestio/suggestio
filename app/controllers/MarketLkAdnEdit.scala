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

  /** Ключ для картинки, используемой в качестве приветствия. */
  val WELCOME_IMG_KEY = "wlcm"

  /** Маркер картинки для использования в качестве логотипа. */
  private val TMP_LOGO_MARKER = "leadLogo"


  /** Маппер для метаданных. */
  private val nodeMetaM = mapping(
    "name"      -> nameM,
    "town"      -> toStrOptM(townM),
    "address"   -> toStrOptM(martAddressM),
    "color"     -> toStrOptM(colorM),
    "siteUrl"   -> urlStrOptM,
    "phone"     -> phoneOptM
  )
  {(name, town, address, color, siteUrlOpt, phoneOpt) =>
    AdnMMetadata(
      name    = name,
      town    = town,
      address = address,
      color = color,
      siteUrl = siteUrlOpt,
      phone   = phoneOpt
    )
  }
  {meta =>
    import meta._
    Some((name, town, address, color, siteUrl, phone))
  }


  /** Маппинг для формы добавления/редактирования торгового центра. */
  private val nodeFormM = Form(tuple(
    "meta" -> nodeMetaM,
    "welcomeImgId" -> optional(ImgFormUtil.imgIdJpegM),
    ImgFormUtil.getLogoKM("adn.rcvr.logo.invalid", marker=TMP_LOGO_MARKER)
  ))


  /** Страница с формой редактирования узла рекламной сети. Функция смотрит тип узла и рендерит ту или иную страницу. */
  def editAdnNode(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import request.adnNode
    getWelcomeAdOpt(adnNode) map { welcomeAdOpt =>
      val martLogoOpt = adnNode.logoImgOpt.map { img =>
        ImgInfo4Save(imgInfo2imgKey(img))
      }
      val welcomeImgKey = welcomeAdOpt.flatMap { _.imgs.headOption }.map[OrigImgIdKey] { img => img._2 }
      val formFilled = nodeFormM.fill((adnNode.meta, welcomeImgKey, martLogoOpt))
      Ok(leaderEditFormTpl(adnNode, formFilled, welcomeAdOpt))
    }
  }


  /** Сабмит формы редактирования узла рекламной сети. Функция смотрит тип узла рекламной сети и использует
    * тот или иной хелпер. */
  def editAdnNodeSubmit(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import request.adnNode
    nodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        val welcomeAdOptFut = getWelcomeAdOpt(adnNode)
        debug(s"martEditFormSubmit(${adnNode.id.get}): Failed to bind form: ${formatFormErrors(formWithErrors)}")
        welcomeAdOptFut map { welcomeAdOpt =>
          NotAcceptable(leaderEditFormTpl(adnNode, formWithErrors, welcomeAdOpt))
            .flashing("error" -> "Ошибка заполнения формы.")
        }
      },
      {case (adnMeta2, newWelcomeImgOpt, logoImgIdOpt) =>
        // В фоне обновляем логотип ТЦ
        val savedLogoFut = ImgFormUtil.updateOrigImg(logoImgIdOpt, oldImgs = adnNode.logoImgOpt)
        // В фоне обновляем картинку карточки-приветствия.
        val savedWelcomeImgsFut = updateWelcodeAdFut(adnNode, newWelcomeImgOpt)
        savedLogoFut flatMap { savedLogo =>
          savedWelcomeImgsFut flatMap { waIdOpt =>
            // Собираем новый экземпляр узла
            val adnNode3 = adnNode.copy(
              meta = adnNode.meta.copy(
                // сохраняем метаданные
                name    = adnMeta2.name,
                town    = adnMeta2.town,
                address = adnMeta2.address,
                color   = adnMeta2.color,
                siteUrl = adnMeta2.siteUrl,
                phone   = adnMeta2.phone,
                // сохраняем welcome ad id
                welcomeAdId = waIdOpt
              ),
              // сохраняем логотип
              logoImgOpt = savedLogo
            )
            adnNode3.save.map { _adnId =>
              Redirect(routes.MarketLkAdn.showAdnNode(_adnId))
                .flashing("success" -> "Изменения сохранены.")
            }
          }
        }
      }
    )
  }


  /**
   * Экшен загрузки картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleTempLogo(adnId: String) = IsAdnNodeAdmin(adnId)(parse.multipartFormData) { implicit request =>
    _handleTempImg(MartLogoImageUtil, Some(TMP_LOGO_MARKER))
  }


  /** Асинхронно получить welcome-ad-карточку. */
  private def getWelcomeAdOpt(welcomeAdId: Option[String]): Future[Option[MWelcomeAd]] = {
    welcomeAdId
      .fold [Future[Option[MWelcomeAd]]] (Future successful None) (MWelcomeAd.getById)
  }
  private def getWelcomeAdOpt(node: MAdnNode): Future[Option[MWelcomeAd]] = {
    getWelcomeAdOpt( node.meta.welcomeAdId )
  }


  /** Обновление картинки приветствия. Она хранится в полу-рекламной карточке, поэтому надо ещё
    * обновить карточку и пересохранить её. */
  private def updateWelcodeAdFut(adnNode: MAdnNode, newWelcomeImgOpt: Option[ImgIdKey]): Future[Option[String]] = {
    getWelcomeAdOpt(adnNode) flatMap { currWelcomeAdOpt =>
      ImgFormUtil.updateOrigImg(
        needImgs = newWelcomeImgOpt.map(ImgInfo4Save(_, withThumb = false)),
        oldImgs = currWelcomeAdOpt.flatMap(_.imgs.headOption).map(_._2)
      ) flatMap {
        // Новой картинки нет. Надо удалить старую карточку (если была), и очистить соотв. welcome-поле.
        case None =>
          adnNode.meta
            .welcomeAdId
            .fold [Future[Option[String]]]
              { Future successful None }
              { waId => MAd.deleteById(waId).map { _ => None } }

        // Новая картинка есть. Пора обновить текущую карточук, или новую создать.
        case newImgInfoOpt @ Some(newImgInfo) =>
          val newImgs = Map(WELCOME_IMG_KEY -> newImgInfo)
          val newWelcomeAd = currWelcomeAdOpt.fold
            { MWelcomeAd(producerId = adnNode.id.get, imgs = newImgs) }
            { _.copy(imgs = newImgs) }
          newWelcomeAd.save
            .map { Some.apply }
      }
    }
  }

}

