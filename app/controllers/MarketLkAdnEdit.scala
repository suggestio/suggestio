package controllers

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.js.UploadConstants
import io.suggest.model.n2.edge.MEdgeInfo
import io.suggest.model.n2.extra.MAdnExtra
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.model.n2.node.meta.{MBusinessInfo, MAddress, MBasicMeta}
import models.im.logo.{LogoOpt_t, LogoUtil}
import models.im.{MImg3_, MImgT, MImg}
import models.jsm.init.MTargets
import org.elasticsearch.client.Client
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, Result}
import play.core.parsers.Multipart
import play.twirl.api.Html
import util.img._
import util.PlayMacroLogsImpl
import util.acl._
import models._
import views.html.lk.adn.edit._
import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import util.FormUtil._
import GalleryUtil._
import models.madn.EditConstants._
import util.img.ImgFormUtil.img3IdOptM

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.14 10:22
 * Description: Редактирование узлов рекламной сети скрывается за парой экшенов, которые в зависимости от типов
 * узлов делают те или иные действия.
 * Супервайзер ресторанной сети и ТЦ имеют одну форму и здесь обозначаются как "узлы-лидеры".
 */
class MarketLkAdnEdit @Inject() (
  override val messagesApi  : MessagesApi,
  override val current      : play.api.Application,
  override val cache        : CacheApi,
  welcomeUtil               : WelcomeUtil,
  logoUtil                  : LogoUtil,
  mImg3                     : MImg3_,
  tempImgSupport            : TempImgSupport,
  override val mNodeCache   : MAdnNodeCache,
  override implicit val ec  : ExecutionContext,
  implicit val esClient     : Client,
  override implicit val sn  : SioNotifierStaticClientI
)
  extends SioController
  with PlayMacroLogsImpl
  with BruteForceProtectCtl
  with IsAdnNodeAdmin
  with IsAuth
{

  import LOGGER._

  /** Макс. байтовая длина загружаемой картинки в галлерею. */
  private val IMG_GALLERY_MAX_LEN_BYTES: Int = {
    val mib = configuration.getInt("adn.node.img.gallery.len.max.mib") getOrElse 20
    mib * 1024 * 1024
  }

  private def logoKM: (String, Mapping[LogoOpt_t]) = {
    LOGO_IMG_FN -> img3IdOptM
  }

  // TODO N2 После переезда на N2 метаданные узла были раскиданы по нескольким моделям, но этого не видно в форме:
  //      тут всё по старому, убого.

  // У нас несколько вариантов развития событий с формами: ресивер, продьюсер или что-то иное. Нужно три маппинга.
  private def nameKM        = "name"    -> nameM
  private def townKM        = "town"    -> townSomeM
  private def addressKM     = "address" -> addressOptM
  private def colorKM       = "color"   -> colorDataSomeM
  private def fgColorKM     = "fgColor" -> colorDataOptM
  private def siteUrlKM     = "siteUrl" -> urlStrOptM
  private def phoneKM       = "phone"   -> phoneOptM

  private def audDescrKM    = "audienceDescr"   -> toStrOptM(audienceDescrM)
  private def humTrafAvgKM  = "humanTrafficAvg" -> optional(humanTrafficAvgM)

  private def infoKM        = "info" -> toStrOptM(text2048M)

  /** Маппер подформы метаданных для узла-ресивера. */
  private def rcvrMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, fgColorKM, siteUrlKM, phoneKM, audDescrKM, humTrafAvgKM, infoKM)
    {(name, town, address, color, fgColorOpt, siteUrlOpt, phoneOpt, audDescr, humanTrafficAvg, info) =>
      MMeta(
        basic = MBasicMeta(
          nameOpt = Some(name)
        ),
        address = MAddress(
          town    = town,
          address = address,
          phone   = phoneOpt
        ),
        business = MBusinessInfo(
          siteUrl           = siteUrlOpt,
          audienceDescr     = audDescr,
          humanTrafficAvg   = humanTrafficAvg,
          info              = info
        ),
        colors = MColors(
          bg = color,
          fg = fgColorOpt
        )
      )
    }
    {meta =>
      Some((
        meta.basic.name,
        meta.address.town, meta.address.address,
        meta.colors.bg, meta.colors.fg,
        meta.business.siteUrl,
        meta.address.phone,
        meta.business.audienceDescr,
        meta.business.humanTrafficAvg,
        meta.business.info
      ))
    }
  }

  /** Маппер подформы метаданных для узла-продьюсера. */
  private def prodMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, fgColorKM, siteUrlKM, phoneKM, infoKM)
    {(name, town, address, color, fgColor, siteUrlOpt, phoneOpt, info) =>
      MMeta(
        basic = MBasicMeta(
          nameOpt = Some(name)
        ),
        address = MAddress(
          town    = town,
          address = address,
          phone   = phoneOpt
        ),
        colors = MColors(
          bg = color,
          fg = fgColor
        ),
        business = MBusinessInfo(
          info = info
        )
      )
    }
    {meta =>
      Some((
        meta.basic.name,
        meta.address.town,
        meta.address.address,
        meta.colors.bg, meta.colors.fg,
        meta.business.siteUrl,
        meta.address.phone,
        meta.business.info
      ))
    }
  }

  /** Маппер для метаданных какого-то узла, для которого не подходят две предыдущие формы.
    * Сделан в виде метода, т.к. такой случай почти невероятен. */
  private def nodeMetaM = {
    mapping(nameKM, townKM, addressKM, colorKM, fgColorKM, siteUrlKM, phoneKM)
    {(name, town, address, color, fgColor, siteUrlOpt, phoneOpt) =>
      MMeta(
        basic = MBasicMeta(
          nameOpt = Some(name)
        ),
        address = MAddress(
          town    = town,
          address = address,
          phone   = phoneOpt
        ),
        colors = MColors(
          bg = color,
          fg = fgColor
        )
      )
    }
    {meta =>
      Some((
        meta.basic.name,
        meta.address.town,
        meta.address.address,
        meta.colors.bg, meta.colors.fg,
        meta.business.siteUrl,
        meta.address.phone
      ))
    }
  }

  /** Маппинг для формы добавления/редактирования торгового центра. */
  private def nodeFormM(nodeInfo: MAdnExtra): Form[FormMapResult] = {
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
      mapping(metaKM, logoKM, welcomeUtil.welcomeImgIdKM, galleryKM)
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

    // Запуск асинхронной сборки данных из моделей.
    val waOptFut = welcomeUtil.getWelcomeAdOpt(adnNode)
    val nodeLogoOptFut = logoUtil.getLogoOfNode(adnNode)
    val gallerryIks = gallery2iiks {
      adnNode.edges
        .withPredicateIterIds( MPredicates.GalleryItem )
        .toList
    }

    // Сборка и наполнения маппинга формы.
    val formM = nodeFormM(adnNode.extras.adn.get)
    val formFilledFut = for {
      welcomeAdOpt <- waOptFut
      nodeLogoOpt  <- nodeLogoOptFut
    } yield {
      val welcomeImgKey = welcomeUtil.welcomeAd2iik(welcomeAdOpt)
      val fmr = FormMapResult(adnNode.meta, nodeLogoOpt, welcomeImgKey, gallerryIks)
      formM fill fmr
    }

    // Рендер и возврат http-ответа.
    for {
      formFilled <- formFilledFut
      html       <- _editAdnNode(formFilled, waOptFut)
    } yield {
      Ok(html)
    }
  }


  private def _editAdnNode(form: Form[FormMapResult], waOptFut: Future[Option[MWelcomeAd]])
                          (implicit request: AbstractRequestForAdnNode[_]): Future[Html] = {
    implicit val jsInitTargets = Seq(MTargets.LkNodeEditForm)
    for {
      welcomeAdOpt <- waOptFut
    } yield {
      nodeEditTpl(request.adnNode, form, welcomeAdOpt)
    }
  }


  /** Сабмит формы редактирования узла рекламной сети. Функция смотрит тип узла рекламной сети и использует
    * тот или иной хелпер. */
  def editAdnNodeSubmit(adnId: String) = IsAdnNodeAdminPost(adnId).async { implicit request =>
    import request.adnNode
    lazy val logPrefix = s"editAdnNodeSubmit($adnId): "
    nodeFormM(adnNode.extras.adn.get).bindFromRequest().fold(
      {formWithErrors =>
        val waOptFut = welcomeUtil.getWelcomeAdOpt(request.adnNode)
        debug(s"${logPrefix}Failed to bind form: ${formatFormErrors(formWithErrors)}")
        _editAdnNode(formWithErrors, waOptFut)
          .map { NotAcceptable(_) }
      },
      {fmr =>
        // В фоне обновляем картинку карточки-приветствия.
        val savedWelcomeImgsFut = welcomeUtil.updateWelcodeAdFut(adnNode, fmr.waImgOpt)
        trace(s"${logPrefix}newGallery[${fmr.gallery.size}] ;; newLogo = ${fmr.logoOpt.map(_.fileName)}")
        // В фоне обновляем логотип узла
        val savedLogoFut = logoUtil.updateLogoFor(adnNode, fmr.logoOpt)
        // Запускаем апдейт галереи.
        val galleryUpdFut = GalleryUtil.updateGallery(
          newGallery = fmr.gallery,
          oldGallery = adnNode.edges
            .withPredicateIter( MPredicates.GalleryItem )
            .map { _.nodeId }
            .toSeq
        )
        for {
          savedLogo <- savedLogoFut
          waIdOpt   <- savedWelcomeImgsFut
          gallery   <- galleryUpdFut
          _         <- MNode.tryUpdate(adnNode) {
            applyNodeChanges(_, fmr.meta, savedLogo, waIdOpt, gallery)
          }
        } yield {
          trace("New gallery = " + gallery.mkString(", "))
          // Собираем новый экземпляр узла
          Redirect(routes.MarketLkAdn.showAdnNode(adnId))
            .flashing(FLASH.SUCCESS -> "Changes.saved")
        }
      }
    )
  }


  /** Накатить изменения на инстанс узла, породив новый инстанс.
    * Вынесена из editAdnNodeSubmit() для декомпозиции и для нужд for{}-синтаксиса. */
  private def applyNodeChanges(mnode: MNode, meta2: MMeta, newLogoOpt: Option[MImgT], waIdOpt: Option[String],
                               newImgGallery: Seq[MImgT]): MNode = {
    mnode.copy(
      meta = mnode.meta.copy(
        basic = mnode.meta.basic.copy(
          nameOpt = meta2.basic.nameOpt
        ),
        address = mnode.meta.address.copy(
          town    = meta2.address.town,
          address = meta2.address.address,
          phone   = meta2.address.phone
        ),
        colors = mnode.meta.colors.copy(
          bg = meta2.colors.bg,
          fg = meta2.colors.fg
        ),
        business = mnode.meta.business.copy(
          // TODO Нужно осторожнее обновлять поля, которые не всегда содержат значения (зависят от типа узла).
          audienceDescr   = meta2.business.audienceDescr,
          humanTrafficAvg = meta2.business.humanTrafficAvg,
          info            = meta2.business.info,
          siteUrl         = meta2.business.siteUrl
        )
      ),
      edges = {
        import MPredicates._
        // Готовим начальный итератор эджей-результатов.
        var edgesIter = mnode.edges
          .withoutPredicateIter(NodeWelcomeAdIs, Logo, GalleryItem)
        // Отрабатываем карточку приветствия.
        for (waId <- waIdOpt) {
          val waEdge = MEdge(NodeWelcomeAdIs, waId)
          edgesIter ++= Iterator(waEdge)
        }
        // Отрабатываем логотип
        for (newLogo <- newLogoOpt) {
          val logoEdge = MEdge(Logo, newLogo.rowKeyStr)
          edgesIter ++= Iterator(logoEdge)
        }
        // Отрабатываем галлерею
        if (newImgGallery.nonEmpty) {
          edgesIter ++= newImgGallery
            .iterator
            .zipWithIndex
            .map { case (img, i) =>
              MEdge(
                GalleryItem,
                img.rowKeyStr,
                order = Some(i),
                info  = MEdgeInfo(
                dynImgArgs = Some( img.dynImgOpsString )
              ))
            }
        }
        // Генерим результат
        mnode.edges.copy(
          out = edgesIter
            .map { medge =>
              medge.toEmapKey -> medge
            }
            .toMap
        )
      }
    )
  }

  import views.html.lk.adn.edit.ovl._

  /**
   * Экшен загрузки картинки для логотипа магазина.
   * Права на доступ к магазину проверяем для защиты от несанкциронированного доступа к lossless-компрессиям.
   * @return Тот же формат ответа, что и для просто temp-картинок.
   */
  def handleTempLogo = _imgUploadBase { implicit request =>
    tempImgSupport._handleTempImg(
      ovlRrr = Some { (imgId, ctx) =>
        _logoOvlTpl(imgId)(ctx)
      },
      mImgCompanion = mImg3
    )
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


  /** Обертка экшена для всех экшенов загрузки картинков. */
  private def _imgUploadBase(f: AbstractRequestWithPwOpt[MultipartFormData[TemporaryFile]] => Future[Result]) = {
    val bp = parse.multipartFormData(Multipart.handleFilePartAsTemporaryFile, maxLength = IMG_GALLERY_MAX_LEN_BYTES)
    IsAuth.async(bp) { implicit request =>
      bruteForceProtected {
        f(request)
      }
    }
  }

  /**
   * Общий код загрузки картинок для узла.
   * @param ovlRrr Функция-рендерер html оверлея картинки.
   * @return Action.
   */
  private def _imgUpload(ovlRrr: (String, Context) => Html) = {
    _imgUploadBase { implicit request =>
      tempImgSupport._handleTempImg(
        ovlRrr = Some(ovlRrr)
      )
    }
  }


  /** Внутренняя модель этого контроллера, отражающая результирующее значение биндинга формы редактирования узла. */
  sealed case class FormMapResult(
    meta        : MMeta,
    logoOpt     : LogoOpt_t,
    waImgOpt    : Option[MImgT]   = None,
    gallery     : List[MImg]      = Nil
  )

}

