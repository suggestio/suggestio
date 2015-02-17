package controllers

import models.im.MImg
import play.core.parsers.Multipart
import util.img.LogoUtil.LogoOpt_t
import util.img._
import util.PlayMacroLogsImpl
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import views.html.lk.adn._
import io.suggest.ym.model.MAdnNode
import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import util.FormUtil._
import GalleryUtil._
import WelcomeUtil._
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.14 10:22
 * Description: Редактирование узлов рекламной сети скрывается за парой экшенов, которые в зависимости от типов
 * узлов делают те или иные действия.
 * Супервайзер ресторанной сети и ТЦ имеют одну форму и здесь обозначаются как "узлы-лидеры".
 */
object MarketLkAdnEdit extends SioController with PlayMacroLogsImpl with TempImgSupport with BruteForceProtectCtl {

  import LOGGER._

  /** Макс. байтовая длина загружаемой картинки в галлерею. */
  private val IMG_GALLERY_MAX_LEN_BYTES: Int = {
    val mib = configuration.getInt("adn.node.img.gallery.len.max.mib") getOrElse 20
    mib * 1024 * 1024
  }

  def logoKM = ImgFormUtil.getLogoKM("adn.logo.invalid")

  // У нас несколько вариантов развития событий с формами: ресивер, продьюсер или что-то иное. Нужно три маппинга.
  private def nameKM        = "name"    -> nameM
  private def townKM        = "town"    -> townSomeM
  private def addressKM     = "address" -> addressOptM
  private def colorKM       = "color"   -> colorSomeM
  private def fgColorKM     = "fgColor" -> colorOptM
  private def siteUrlKM     = "siteUrl" -> urlStrOptM
  private def phoneKM       = "phone"   -> phoneOptM

  private def audDescrKM    = "audienceDescr"   -> toStrOptM(audienceDescrM)
  private def humTrafAvgKM  = "humanTrafficAvg" -> optional(humanTrafficAvgM)

  private def infoKM        = "info" -> toStrOptM(text2048M)

  /** Маппер подформы метаданных для узла-ресивера. */
  private def rcvrMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, fgColorKM, siteUrlKM, phoneKM, audDescrKM, humTrafAvgKM, infoKM)
    {(name, town, address, color, fgColorOpt, siteUrlOpt, phoneOpt, audDescr, humanTrafficAvg, info) =>
      AdnMMetadata(
        name    = name,
        town    = town,
        address = address,
        color   = color,
        fgColor = fgColorOpt,
        siteUrl = siteUrlOpt,
        phone   = phoneOpt,
        audienceDescr = audDescr,
        humanTrafficAvg = humanTrafficAvg,
        info    = info
      )
    }
    {meta =>
      import meta._
      Some((name, town, address, color, fgColor, siteUrl, phone, audienceDescr, humanTrafficAvg, info))
    }
  }

  /** Маппер подформы метаданных для узла-продьюсера. */
  private def prodMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, fgColorKM, siteUrlKM, phoneKM, infoKM)
    {(name, town, address, color, fgColor, siteUrlOpt, phoneOpt, info) =>
      AdnMMetadata(
        name    = name,
        town    = town,
        address = address,
        color   = color,
        fgColor = fgColor,
        siteUrl = siteUrlOpt,
        phone   = phoneOpt,
        info    = info
      )
    }
    {meta =>
      import meta._
      Some((name, town, address, color, fgColor, siteUrl, phone, info))
    }
  }

  /** Маппер для метаданных какого-то узла, для которого не подходят две предыдущие формы.
    * Сделан в виде метода, т.к. такой случай почти невероятен. */
  private def nodeMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, fgColorKM, siteUrlKM, phoneKM)
    {(name, town, address, color, fgColor, siteUrlOpt, phoneOpt) =>
      AdnMMetadata(
        name    = name,
        town    = town,
        address = address,
        color   = color,
        fgColor = fgColor,
        siteUrl = siteUrlOpt,
        phone   = phoneOpt
      )
    }
    {meta =>
      import meta._
      Some((name, town, address, color, fgColor, siteUrl, phone))
    }
  }

  /** Маппинг для формы добавления/редактирования торгового центра. */
  private def nodeFormM(nodeInfo: AdNetMemberInfo): Form[FormMapResult] = {
    val metaM = if (nodeInfo.isReceiver) {
      rcvrMetaM
    } else if (nodeInfo.isProducer) {
      prodMetaM
    } else {
      nodeMetaM
    }
    val metaKM = "meta" -> metaM
    // У ресивера есть поля для картинки приветствия и галерея для демонстрации.
    val m: Mapping[FormMapResult] = if (nodeInfo.isReceiver) {
      mapping(metaKM, logoKM, welcomeImgIdKM, galleryKM)
        { FormMapResult.apply }
        { FormMapResult.unapply }
    } else {
      mapping(metaKM, logoKM)
        { FormMapResult(_, _) }
        { fmr => Some((fmr.meta, fmr.logoOpt)) }
    }
    Form(m)
  }


  /** Страница с формой редактирования узла рекламной сети. Функция смотрит тип узла и рендерит ту или иную страницу. */
  def editAdnNode(adnId: String) = IsAdnNodeAdminGet(adnId).async { implicit request =>
    import request.adnNode
    val waOptFut = getWelcomeAdOpt(adnNode)
    val nodeLogoOpt = adnNode.logoImgOpt
      .map { img => MImg(img.filename) }
    val gallerryIks = gallery2iiks( adnNode.gallery )
    val formNotFilled = nodeFormM(adnNode.adn)
    waOptFut map { welcomeAdOpt =>
      val welcomeImgKey = welcomeAd2iik(welcomeAdOpt)
      val fmr = FormMapResult(adnNode.meta, nodeLogoOpt, welcomeImgKey, gallerryIks)
      val formFilled = formNotFilled fill fmr
      Ok(leaderEditFormTpl(adnNode, formFilled, welcomeAdOpt))
    }
  }

  /** Сабмит формы редактирования узла рекламной сети. Функция смотрит тип узла рекламной сети и использует
    * тот или иной хелпер. */
  def editAdnNodeSubmit(adnId: String) = IsAdnNodeAdminPost(adnId).async { implicit request =>
    import request.adnNode
    lazy val logPrefix = s"editAdnNodeSubmit($adnId): "
    nodeFormM(adnNode.adn).bindFromRequest().fold(
      {formWithErrors =>
        val welcomeAdOptFut = getWelcomeAdOpt(adnNode)
        debug(s"${logPrefix}Failed to bind form: ${formatFormErrors(formWithErrors)}")
        welcomeAdOptFut map { welcomeAdOpt =>
          NotAcceptable(leaderEditFormTpl(adnNode, formWithErrors, welcomeAdOpt))
        }
      },
      {fmr =>
        // В фоне обновляем картинку карточки-приветствия.
        val savedWelcomeImgsFut = WelcomeUtil.updateWelcodeAdFut(adnNode, fmr.waImgIdOpt)
        trace(s"${logPrefix}newGallery[${fmr.gallery.size}] ;; newLogo = ${fmr.logoOpt.map(_.fileName)}")
        // В фоне обновляем логотип узла
        val savedLogoFut = LogoUtil.updateLogo(fmr.logoOpt, adnNode.logoImgOpt)
        // Запускаем апдейт галереи.
        val galleryUpdFut = GalleryUtil.updateGallery(fmr.gallery, oldGallery = adnNode.gallery)
        for {
          savedLogo <- savedLogoFut
          waIdOpt   <- savedWelcomeImgsFut
          gallery   <- galleryUpdFut
          _         <- MAdnNode.tryUpdate(adnNode) {
            applyNodeChanges(_, fmr.meta, waIdOpt, savedLogo, gallery)
          }
        } yield {
          trace("New gallery = " + gallery.mkString(", "))
          // Собираем новый экземпляр узла
          Redirect(routes.MarketLkAdn.showAdnNode(adnId))
            .flashing("success" -> "Изменения сохранены.")
        }
      }
    )
  }


  /** Накатить изменения на инстанс узла, породив новый инстанс.
    * Вынесена из editAdnNodeSubmit() для декомпозиции и для нужд for{}-синтаксиса. */
  private def applyNodeChanges(adnNode: MAdnNode, adnMeta2: AdnMMetadata, waIdOpt: Option[String],
                               newLogo: Option[MImgInfoT], newImgGallery: List[String]): MAdnNode = {
    adnNode.copy(
      meta = adnNode.meta.copy(
        // сохраняем метаданные
        name    = adnMeta2.name,
        town    = adnMeta2.town,
        address = adnMeta2.address,
        color   = adnMeta2.color,
        fgColor = adnMeta2.fgColor,
        siteUrl = adnMeta2.siteUrl,
        phone   = adnMeta2.phone,
        // TODO Нужно осторожнее обновлять поля, которые не всегда содержат значения (зависят от типа узла).
        audienceDescr = adnMeta2.audienceDescr,
        humanTrafficAvg = adnMeta2.humanTrafficAvg,
        info = adnMeta2.info,
        // сохраняем welcome ad id
        welcomeAdId = waIdOpt
      ),
      // сохраняем логотип
      logoImgOpt = newLogo,
      gallery = newImgGallery
    )
  }


  /**
   * Экшен загрузки картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleTempLogo = IsAuth.async(parse.multipartFormData) { implicit request =>
    bruteForceProtected {
      _handleTempImg()
    }
  }


  /** Юзер постит временную картинку для личного галереи узла. */
  def handleGallweryImg = {
    val bp = parse.multipartFormData(Multipart.handleFilePartAsTemporaryFile, maxLength = IMG_GALLERY_MAX_LEN_BYTES)
    IsAuth.async(bp) { implicit request =>
      bruteForceProtected {
        _handleTempImg()
      }
    }
  }


  sealed case class FormMapResult(
    meta: AdnMMetadata,
    logoOpt: LogoOpt_t,
    waImgIdOpt: Option[MImg] = None,
    gallery: List[MImg] = Nil
  )
}

