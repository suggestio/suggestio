package controllers

import play.api.mvc.Action
import util.img._
import util.img.ImgFormUtil.{LogoOpt_t, imgInfo2imgKey}
import util.PlayMacroLogsImpl
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import views.html.market.lk.adn._
import io.suggest.ym.model.MAdnNode
import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import util.FormUtil._
import GalleryUtil._
import WelcomeUtil._

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
  private val TMP_LOGO_MARKER = "leadLogo"

  // У нас несколько вариантов развития событий с формами: ресивер, продьюсер или что-то иное. Нужно три маппинга.
  private val nameKM        = "name"    -> nameM
  private val townKM        = "town"    -> townSomeM
  private val addressKM     = "address" -> addressSomeM
  val colorKM = "color"   -> colorSomeM
  private val siteUrlKM     = "siteUrl" -> urlStrOptM
  private val phoneKM       = "phone"   -> phoneOptM

  private val audDescrKM    = "audienceDescr"   -> toSomeStrM(audienceDescrM)
  private val humTrafAvgKM  = "humanTrafficAvg" -> humanTrafficAvgM.transform[Option[Int]](Some.apply, { _ getOrElse 0 })

  private val infoKM        = "info" -> toSomeStrM(text2048M)

  /** Маппер подформы метаданных для узла-ресивера. */
  private val rcvrMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, siteUrlKM, phoneKM, audDescrKM, humTrafAvgKM)
    {(name, town, address, color, siteUrlOpt, phoneOpt, audDescr, humanTrafficAvg) =>
      AdnMMetadata(
        name    = name,
        town    = town,
        address = address,
        color   = color,
        siteUrl = siteUrlOpt,
        phone   = phoneOpt,
        audienceDescr = audDescr,
        humanTrafficAvg = humanTrafficAvg
      )
    }
    {meta =>
      import meta._
      Some((name, town, address, color, siteUrl, phone, audienceDescr, humanTrafficAvg))
    }
  }

  /** Маппер подформы метаданных для узла-продьюсера. */
  private val prodMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, siteUrlKM, phoneKM, infoKM)
    {(name, town, address, color, siteUrlOpt, phoneOpt, info) =>
      AdnMMetadata(
        name    = name,
        town    = town,
        address = address,
        color   = color,
        siteUrl = siteUrlOpt,
        phone   = phoneOpt,
        info    = info
      )
    }
    {meta =>
      import meta._
      Some((name, town, address, color, siteUrl, phone, info))
    }
  }

  /** Маппер для метаданных какого-то узла, для которого не подходят две предыдущие формы.
    * Сделан в виде метода, т.к. такой случай почти невероятен. */
  private def nodeMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, siteUrlKM, phoneKM)
    {(name, town, address, color, siteUrlOpt, phoneOpt) =>
      AdnMMetadata(
        name    = name,
        town    = town,
        address = address,
        color   = color,
        siteUrl = siteUrlOpt,
        phone   = phoneOpt
      )
    }
    {meta =>
      import meta._
      Some((name, town, address, color, siteUrl, phone))
    }
  }

  val logoKM = ImgFormUtil.getLogoKM("adn.logo.invalid", marker=TMP_LOGO_MARKER)

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
  def editAdnNode(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import request.adnNode
    val waOptFut = getWelcomeAdOpt(adnNode)
    val martLogoOpt = adnNode.logoImgOpt.map { img =>
      ImgInfo4Save(imgInfo2imgKey(img))
    }
    val gallerryIks = gallery2iiks( adnNode.gallery )
    val formNotFilled = nodeFormM(adnNode.adn)
    waOptFut map { welcomeAdOpt =>
      val welcomeImgKey = welcomeAd2iik(welcomeAdOpt)
      val fmr = FormMapResult(adnNode.meta, martLogoOpt, welcomeImgKey, gallerryIks)
      val formFilled = formNotFilled fill fmr
      Ok(leaderEditFormTpl(adnNode, formFilled, welcomeAdOpt))
    }
  }

  /** Сабмит формы редактирования узла рекламной сети. Функция смотрит тип узла рекламной сети и использует
    * тот или иной хелпер. */
  def editAdnNodeSubmit(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import request.adnNode
    nodeFormM(adnNode.adn).bindFromRequest().fold(
      {formWithErrors =>
        val welcomeAdOptFut = getWelcomeAdOpt(adnNode)
        debug(s"martEditFormSubmit(${adnNode.id.get}): Failed to bind form: ${formatFormErrors(formWithErrors)}")
        welcomeAdOptFut map { welcomeAdOpt =>
          NotAcceptable(leaderEditFormTpl(adnNode, formWithErrors, welcomeAdOpt))
        }
      },
      {fmr =>
        trace(s"editAdnNodeSubmit($adnId): newGallery[${fmr.gallery.size}] ;; newLogo = ${fmr.logoOpt.map(_.iik.filename)}")
        // В фоне обновляем логотип ТЦ
        val savedLogoFut = ImgFormUtil.updateOrigImg(
          needImgs = fmr.logoOpt.toSeq,
          oldImgs  = adnNode.logoImgOpt.toIterable
        )
        // Запускаем апдейт галереи.
        val galleryUpdFut = ImgFormUtil.updateOrigImgId(
          needImgs = gallery4s(fmr.gallery),
          oldImgIds = adnNode.gallery
        )
        // В фоне обновляем картинку карточки-приветствия.
        val savedWelcomeImgsFut = updateWelcodeAdFut(adnNode, fmr.waImgIdOpt)
        for {
          savedLogo <- savedLogoFut
          waIdOpt   <- savedWelcomeImgsFut
          gallery   <- galleryUpdFut
          _adnId    <- applyNodeChanges(adnNode, fmr.meta, waIdOpt, savedLogo, gallery).save
        } yield {
          // Собираем новый экземпляр узла
          Redirect(routes.MarketLkAdn.showAdnNode(_adnId))
            .flashing("success" -> "Изменения сохранены.")
        }
      }
    )
  }


  /** Накатить изменения на инстанс узла, породив новый инстанс.
    * Вынесена из editAdnNodeSubmit() для декомпозиции и для нужд for{}-синтаксиса. */
  private def applyNodeChanges(adnNode: MAdnNode, adnMeta2: AdnMMetadata, waIdOpt: Option[String],
                               newLogo: Option[MImgInfoT], newImgGallery: List[MImgInfoT]): MAdnNode = {
    adnNode.copy(
      meta = adnNode.meta.copy(
        // сохраняем метаданные
        name    = adnMeta2.name,
        town    = adnMeta2.town,
        address = adnMeta2.address,
        color   = adnMeta2.color,
        siteUrl = adnMeta2.siteUrl,
        phone   = adnMeta2.phone,
        // TODO Нужно осторожнее обновлять поля, которые не всегда содержат значения.
        audienceDescr = adnMeta2.audienceDescr,
        humanTrafficAvg = adnMeta2.humanTrafficAvg,
        info = adnMeta2.info,
        // сохраняем welcome ad id
        welcomeAdId = waIdOpt
      ),
      // сохраняем логотип
      logoImgOpt = newLogo,
      gallery = gallery2filenames(newImgGallery)
    )
  }


  /**
   * Экшен загрузки картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleTempLogo = Action(parse.multipartFormData) { implicit request =>
    _handleTempImg(MartLogoImageUtil, Some(TMP_LOGO_MARKER))
  }


  sealed case class FormMapResult(
    meta: AdnMMetadata,
    logoOpt: LogoOpt_t,
    waImgIdOpt: Option[ImgIdKey] = None,
    gallery: List[ImgIdKey] = Nil
  )
}

