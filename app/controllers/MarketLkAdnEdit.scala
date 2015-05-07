package controllers

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.suggest.js.UploadConstants
import models.im.MImg
import models.jsm.init.MTargets
import play.api.i18n.MessagesApi
import play.core.parsers.Multipart
import play.twirl.api.Html
import util.img.LogoUtil.LogoOpt_t
import util.img._
import util.PlayMacroLogsImpl
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import views.html.lk.adn.edit._
import io.suggest.ym.model.MAdnNode
import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import util.FormUtil._
import GalleryUtil._
import WelcomeUtil._
import play.api.Play.{current, configuration}
import models.madn.EditConstants._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.14 10:22
 * Description: Редактирование узлов рекламной сети скрывается за парой экшенов, которые в зависимости от типов
 * узлов делают те или иные действия.
 * Супервайзер ресторанной сети и ТЦ имеют одну форму и здесь обозначаются как "узлы-лидеры".
 */
class MarketLkAdnEdit @Inject() (
  override val messagesApi: MessagesApi,
  override val actorSystem: ActorSystem
)
  extends SioController with PlayMacroLogsImpl with TempImgSupport with BruteForceProtectCtl
{

  import LOGGER._

  /** Макс. байтовая длина загружаемой картинки в галлерею. */
  private val IMG_GALLERY_MAX_LEN_BYTES: Int = {
    val mib = configuration.getInt("adn.node.img.gallery.len.max.mib") getOrElse 20
    mib * 1024 * 1024
  }


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
    val formFilledFut = waOptFut map { welcomeAdOpt =>
      val welcomeImgKey = welcomeAd2iik(welcomeAdOpt)
      val fmr = FormMapResult(adnNode.meta, nodeLogoOpt, welcomeImgKey, gallerryIks)
      formNotFilled fill fmr
    }
    formFilledFut flatMap { formFilled =>
      _editAdnNode(formFilled, waOptFut)
        .map { Ok(_) }
    }
  }

  protected def _editAdnNode(form: Form[FormMapResult], waOptFut: Future[Option[MWelcomeAd]])
                            (implicit request: AbstractRequestForAdnNode[_]): Future[Html] = {
    implicit val jsInitTargets = Seq(MTargets.LkNodeEditForm)
    waOptFut map { welcomeAdOpt =>
      nodeEditTpl(request.adnNode, form, welcomeAdOpt)
    }
  }

  /** Сабмит формы редактирования узла рекламной сети. Функция смотрит тип узла рекламной сети и использует
    * тот или иной хелпер. */
  def editAdnNodeSubmit(adnId: String) = IsAdnNodeAdminPost(adnId).async { implicit request =>
    import request.adnNode
    lazy val logPrefix = s"editAdnNodeSubmit($adnId): "
    nodeFormM(adnNode.adn).bindFromRequest().fold(
      {formWithErrors =>
        val waOptFut = getWelcomeAdOpt(request.adnNode)
        debug(s"${logPrefix}Failed to bind form: ${formatFormErrors(formWithErrors)}")
        _editAdnNode(formWithErrors, waOptFut)
          .map { NotAcceptable(_) }
      },
      {fmr =>
        // В фоне обновляем картинку карточки-приветствия.
        val savedWelcomeImgsFut = WelcomeUtil.updateWelcodeAdFut(adnNode, fmr.waImgOpt)
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

  import views.html.lk.adn.edit.ovl._

  /**
   * Экшен загрузки картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleTempLogo = _imgUpload { (imgId, ctx) =>
    _logoOvlTpl(imgId)(ctx)
  }


  /**
   * Экшен для загрузки картинки приветствия.
   * @return JSON с тем же форматом ответа, что и для других img upload'ов.
   */
  def uploadWelcomeImg = _imgUpload { (imgId, ctx) =>
    _welcomeOvlTpl(imgId)(ctx)
  }


  /** Юзер постит временную картинку для личного галереи узла. */
  def handleGalleryImg = _imgUpload { (imgId, ctx) =>
    val index = ctx.request
      .getQueryString(UploadConstants.NAME_INDEX_QS_NAME)
      .get
      .toInt
    _galIndexedOvlTpl(imgId, fnIndex = index)(ctx)
  }

  /**
   * Общий код загрузки всех картинок для узла.
   * @param ovlRrr Функция-рендерер html оверлея картинки.
   * @return Action.
   */
  private def _imgUpload(ovlRrr: (String, Context) => Html) = {
    val bp = parse.multipartFormData(Multipart.handleFilePartAsTemporaryFile, maxLength = IMG_GALLERY_MAX_LEN_BYTES)
    IsAuth.async(bp) { implicit request =>
      bruteForceProtected {
        _handleTempImg(
          ovlRrr = Some(ovlRrr)
        )
      }
    }
  }


  /** Внутренняя модель этого контроллера, отражающая результирующее значение биндинга формы редактирования узла. */
  sealed case class FormMapResult(
    meta        : AdnMMetadata,
    logoOpt     : LogoOpt_t,
    waImgOpt    : Option[MImg] = None,
    gallery     : List[MImg] = Nil
  )
}

