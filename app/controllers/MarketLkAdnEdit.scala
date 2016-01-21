package controllers

import com.google.inject.Inject
import io.suggest.js.UploadConstants
import io.suggest.model.n2.edge.MEdgeInfo
import io.suggest.model.n2.extra.MAdnExtra
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.model.n2.node.meta.{MAddress, MBasicMeta, MBusinessInfo}
import models._
import models.im.logo.LogoOpt_t
import models.im.{MImgs3, MImgT}
import models.jsm.init.MTargets
import models.madn.EditConstants._
import models.mctx.{CtxData, Context}
import models.mlk.{FormMapResult, NodeEditArgs}
import models.mproj.ICommonDi
import models.req.{INodeReq, IReq}
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, Result}
import play.core.parsers.Multipart
import play.twirl.api.Html
import util.FormUtil._
import util.PlayMacroLogsImpl
import util.acl._
import util.img.ImgFormUtil.img3IdOptM
import util.img._
import views.html.lk.adn.edit._

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
  welcomeUtil                     : WelcomeUtil,
  logoUtil                        : LogoUtil,
  mImgs3                          : MImgs3,
  tempImgSupport                  : TempImgSupport,
  galleryUtil                     : GalleryUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioController
  with PlayMacroLogsImpl
  with BruteForceProtectCtl
  with IsAdnNodeAdmin
  with IsAuth
{

  import LOGGER._
  import mCommonDi._

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
      mapping(metaKM, logoKM, welcomeUtil.welcomeImgIdKM, galleryUtil.galleryKM)
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
  def editAdnNode(adnId: String) = IsAdnNodeAdminGet(adnId, U.Lk).async { implicit request =>
    import request.mnode

    // Запуск асинхронной сборки данных из моделей.
    val nodeLogoOptFut = logoUtil.getLogoOfNode(mnode)
    val gallerryIks = galleryUtil
      .gallery2iiks {
        mnode.edges
          .withPredicateIter( MPredicates.GalleryItem )
      }
      .toList

    // Сборка и наполнения маппинга формы.
    val formM = nodeFormM(mnode.extras.adn.get)
    val formFilledFut = for {
      nodeLogoOpt  <- nodeLogoOptFut
    } yield {
      val wcLogoImg = welcomeUtil.wcLogoImg(mnode)
      val fmr = FormMapResult(mnode.meta, nodeLogoOpt, wcLogoImg, gallerryIks)
      formM fill fmr
    }

    // Рендер и возврат http-ответа.
    for {
      formFilled  <- formFilledFut
      resp        <- _editAdnNode(formFilled, Ok)
    } yield {
      resp
    }
  }

  /** Общий код рендера редактирования узла. */
  private def _editAdnNode(form: Form[FormMapResult], rs: Status)
                          (implicit request: INodeReq[_]): Future[Result] = {
    val args = NodeEditArgs(
      mnode         = request.mnode,
      mf            = form
    )

    val jsiTgs1 = Seq(MTargets.LkNodeEditForm)

    for {
      ctxData0  <- request.user.lkCtxData
    } yield {
      implicit val ctxData = ctxData0.copy(
        jsiTgs        = jsiTgs1
      )
      val render = nodeEditTpl(args)
      rs(render)
    }
  }


  /** Сабмит формы редактирования узла рекламной сети. Функция смотрит тип узла рекламной сети и использует
    * тот или иной хелпер. */
  def editAdnNodeSubmit(adnId: String) = IsAdnNodeAdminPost(adnId, U.Lk).async { implicit request =>
    import request.mnode
    lazy val logPrefix = s"editAdnNodeSubmit($adnId): "
    nodeFormM(mnode.extras.adn.get).bindFromRequest().fold(
      {formWithErrors =>
        debug(s"${logPrefix}Failed to bind form: ${formatFormErrors(formWithErrors)}")
        _editAdnNode(formWithErrors, NotAcceptable)
      },
      {fmr =>
        // В фоне обновляем картинку карточки-приветствия.
        val waFgEdgeOptFut = welcomeUtil.updateWcFgImg(mnode, fmr.waImgOpt)
        trace(s"${logPrefix}newGallery[${fmr.gallery.size}] ;; newLogo = ${fmr.logoOpt.map(_.fileName)}")
        // В фоне обновляем логотип узла
        val savedLogoFut = logoUtil.updateLogoFor(mnode, fmr.logoOpt)
        // Запускаем апдейт галереи.
        val galleryUpdFut = galleryUtil.updateGallery(
          newGallery = fmr.gallery,
          oldGallery = mnode.edges
            .withPredicateIter( MPredicates.GalleryItem )
            .map { _.nodeId }
            .toSeq
        )
        for {
          savedLogo   <- savedLogoFut
          waFgEdgeOpt <- waFgEdgeOptFut
          gallery     <- galleryUpdFut
          _           <- MNode.tryUpdate(mnode) {
            applyNodeChanges(_, fmr.meta, savedLogo, waFgEdgeOpt, gallery)
          }
        } yield {
          // Собираем новый экземпляр узла
          Redirect(routes.MarketLkAdn.showAdnNode(adnId))
            .flashing(FLASH.SUCCESS -> "Changes.saved")
        }
      }
    )
  }


  /** Накатить изменения на инстанс узла, породив новый инстанс.
    * Вынесена из editAdnNodeSubmit() для декомпозиции и для нужд for{}-синтаксиса. */
  private def applyNodeChanges(mnode: MNode, meta2: MMeta, newLogoOpt: Option[MImgT], waFgEdgeOpt: Option[MEdge],
                               newImgGallery: Seq[MImgT]): MNode = {
    mnode.copy(
      meta = mnode.meta.copy(
        basic = mnode.meta.basic.copy(
          nameOpt = meta2.basic.nameOpt,
          dateEdited = Some( DateTime.now )
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
          .withoutPredicateIter(WcLogo, Logo, GalleryItem)
        // Отрабатываем карточку приветствия.
        for (waFgEdge <- waFgEdgeOpt) {
          edgesIter ++= Iterator(waFgEdge)
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
                  dynImgArgs = img.qOpt
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
      mImgCompanion = mImgs3
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
  private def _imgUploadBase(f: IReq[MultipartFormData[TemporaryFile]] => Future[Result]) = {
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


}

