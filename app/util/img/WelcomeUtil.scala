package util.img

import models.im._
import models.msc.WelcomeRenderArgsT
import util.cdn.CdnUtil
import util.showcase.ShowcaseUtil
import scala.concurrent.Future
import models._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 10:54
 * Description: Утиль для картинки/карточки приветствия.
 */
object WelcomeUtil {

  /** Прокидывать ссылку bgImg через dynImg(), а не напрямую. */
  val BG_VIA_DYN_IMG: Boolean = configuration.getBoolean("showcase.welcome.bg.dynamic.enabled") getOrElse true

  /** Ключ для картинки, используемой в качестве приветствия. */
  val WELCOME_IMG_KEY = "wlcm"

  val welcomeImgIdKM = "welcomeImgId" -> optional(ImgFormUtil.imgIdM)

  /** Асинхронно получить welcome-ad-карточку. */
  def getWelcomeAdOpt(welcomeAdId: Option[String]): Future[Option[MWelcomeAd]] = {
    welcomeAdId
      .fold [Future[Option[MWelcomeAd]]] (Future successful None) (MWelcomeAd.getById)
  }
  def getWelcomeAdOpt(node: MAdnNode): Future[Option[MWelcomeAd]] = {
    getWelcomeAdOpt( node.meta.welcomeAdId )
  }

  def updateWaImg(waOpt: Option[MWelcomeAd], newWaImgOpt: Option[MImg]) = {
    val saveAllFut = ImgFormUtil.updateOrigImgFull(
      needImgs = newWaImgOpt.toSeq,
      oldImgs = waOpt
        .flatMap(_.imgs.headOption)
        .map { kv => MImg(kv._2.filename) }
        .toIterable
    )
    saveAllFut map { _.headOption }
  }

  /** Обновление картинки и карточки приветствия. Картинка хранится в полу-рекламной карточке, поэтому надо ещё
    * обновить карточку и пересохранить её. */
  def updateWelcodeAdFut(adnNode: MAdnNode, newWelcomeImgOpt: Option[MImg]): Future[Option[String]] = {
    getWelcomeAdOpt(adnNode) flatMap { currWelcomeAdOpt =>
      updateWaImg(currWelcomeAdOpt, newWelcomeImgOpt) flatMap {
        // Новой картинки нет. Надо удалить старую карточку (если была), и очистить соотв. welcome-поле.
        case None =>
          adnNode.meta
            .welcomeAdId
            .fold [Future[Option[String]]]
              { Future successful None }
              { waId => MWelcomeAd.deleteById(waId).map { _ => None } }

        // Новая картинка есть. Пора обновить текущую карточук, или новую создать.
        case Some(newImg) =>
          ImgFormUtil.img2imgInfo(newImg) flatMap { newImgInfo =>
            val newWelcomeAd = updateWaOptWith(currWelcomeAdOpt, newImgInfo, adnNode.id.get)
            newWelcomeAd.save
              .map { Some.apply }
          }
      }
    }
  }


  /**
   * Обновление указанной рекламной карточки без сайд-эффектов. Если картинки нет, то и карточки на выходе не будет.
   * @param waOpt Исходная рекламная карточка, если есть.
   * @param newImgOpt Новая картинка, если есть.
   * @param newProducerId producerId если понадобится его выставить.
   * @return Опциональный результат в виде экземпляра MWA (новой или на основе исходной waOpt).
   */
  def updateWaOptAdHoc(waOpt: Option[MWelcomeAd], newImgOpt: Option[MImgInfoT], newProducerId: String): Option[MWelcomeAd] = {
    newImgOpt map { newImg =>
      updateWaOptWith(waOpt, newImg, newProducerId)
    }
  }

  def updateWaOptWith(waOpt: Option[MWelcomeAd], newImg: MImgInfoT, newProducerId: String): MWelcomeAd = {
    val newImgs = Map(WELCOME_IMG_KEY -> newImg)
    waOpt.fold
      { MWelcomeAd(producerId = newProducerId, imgs = newImgs) }
      { _.copy(imgs = newImgs) }
  }


  def welcomeAd2iik(waOpt: Option[MWelcomeAd]) = {
    waOpt
      .flatMap { _.imgs.headOption }
      .map[MImg] { img => MImg(img._2.filename) }
  }


  /**
   * Асинхронно собрать аргументы для рендера карточки приветствия.
   * @param adnNode Узел, для которого нужно подготовить настройки рендера приветствия.
   * @param screen Настройки экрана, если есть.
   * @return Фьючерс с опциональными настройками. Если None, то приветствие рендерить не надо.
   */
  def getWelcomeRenderArgs(adnNode: MAdnNode, screen: Option[DevScreenT])(implicit ctx: Context): Future[Option[WelcomeRenderArgsT]] = {
    val welcomeAdOptFut = adnNode.meta
      .welcomeAdId
      .fold (Future successful Option.empty[MWelcomeAd]) (MWelcomeAd.getById)
    // Получить параметры (метаданные) фоновой картинки из хранилища картирок.
    val bgFut = adnNode.gallery
      .headOption
      .fold[Future[Either[String, ImgUrlInfoT]]] {
        Future successful colorBg(adnNode)
      } { bgImgFilename =>
        val oiik = MImg(bgImgFilename)
        oiik.original.getImageWH map {
          case Some(meta) =>
            Right(bgCallForScreen(oiik, screen, meta))
          case _ => colorBg(adnNode)
        }
      }
    for {
      welcomeAdOpt <- welcomeAdOptFut
      bg1          <- bgFut
    } yield {
      val wra = new WelcomeRenderArgsT {
        override def bg = bg1
        override def fgImage = welcomeAdOpt.flatMap(_.imgs.get(WELCOME_IMG_KEY))
        override def fgText = Some(adnNode.meta.name)
      }
      Some(wra)
    }
  }


  /** Собрать ссылку на фоновую картинку. */
  def bgCallForScreen(oiik: MImg, screenOpt: Option[DevScreenT], origMeta: MImgInfoMeta)(implicit ctx: Context): ImgUrlInfoT = {
    val oiik2 = oiik.original
    screenOpt
      .filter { _ => BG_VIA_DYN_IMG }
      .flatMap { scr =>
        scr.maybeBasicScreenSize.map(_ -> scr)
      }
      .fold [ImgUrlInfoT] {
        new ImgUrlInfoT {
          override def call = CdnUtil.dynImg(oiik2.fileName)
          override def meta = Some(origMeta)
        }
      } { case (bss, screen) =>
        val imOps = imConvertArgs(bss, screen)
        val dynArgs = oiik2.copy(dynImgOps = imOps)
        new ImgUrlInfoT {
          override def call = CdnUtil.dynImg(dynArgs)
          override def meta = Some(bss)
        }
      }
  }

  /** Сгенерить аргументы для генерации dyn-img ссылки. По сути, тут список параметров для вызова convert.
    * @param scrSz Размер конечной картинки.
    * @return Список ImOp в прямом порядке.
    */
  def imConvertArgs(scrSz: BasicScreenSize, screen: DevScreenT): Seq[ImOp] = {
    val gravity = ImGravities.Center
    val acc0: List[ImOp] = Nil
    val bgc = screen.pixelRatio.bgCompression
    val acc1 = gravity ::
      AbsResizeOp(scrSz, Seq(ImResizeFlags.FillArea)) ::
      gravity ::
      ExtentOp(scrSz) ::
      StripOp ::
      bgc.imQualityOp ::
      ImInterlace.Plane ::
      bgc.chromaSubSampling ::
      acc0
    bgc
      .imGaussBlurOp
      .fold(acc1)(_ :: acc1)
  }

  /** внутренний метод для генерации ответа по фону приветствия в режиме цвета. */
  private def colorBg(adnNode: MAdnNode) = {
    val bgColor = adnNode.meta.color.getOrElse(ShowcaseUtil.SITE_BGCOLOR_DFLT)
    Left(bgColor)
  }

}
