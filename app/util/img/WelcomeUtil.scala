package util.img

import com.google.inject.{Singleton, Inject}
import io.suggest.common.fut.FutureUtil
import io.suggest.event.SioNotifierStaticClientI
import models.im._
import models.madn.EditConstants
import models.msc.{MWelcomeRenderArgs, WelcomeRenderArgsT}
import org.elasticsearch.client.Client
import play.api.Configuration
import util.PlayMacroLogsImpl
import util.cdn.CdnUtil
import util.showcase.ShowcaseUtil
import scala.concurrent.{ExecutionContext, Future}
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 10:54
 * Description: Утиль для картинки/карточки приветствия.
 */
@Singleton
class WelcomeUtil @Inject() (
  configuration          : Configuration,
  scUtil                 : ShowcaseUtil,
  cdnUtil                : CdnUtil,
  mImg3                  : MImg3_,
  implicit val ec        : ExecutionContext,
  implicit val esClient  : Client,
  implicit val sn        : SioNotifierStaticClientI
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

  /** Прокидывать ссылку bgImg через dynImg(), а не напрямую. */
  val BG_VIA_DYN_IMG: Boolean = configuration.getBoolean("showcase.welcome.bg.dynamic.enabled") getOrElse true

  /** Ключ для картинки, используемой в качестве приветствия. */
  val WELCOME_IMG_KEY = "wlcm"

  def welcomeImgIdKM = EditConstants.WELCOME_IMG_FN -> ImgFormUtil.img3IdOptM

  /** Асинхронно получить welcome-ad-карточку. */
  def getWelcomeAdOpt(welcomeAdId: Option[String]): Future[Option[MWelcomeAd]] = {
    FutureUtil.optFut2futOpt(welcomeAdId)(MWelcomeAd.getById(_))
  }
  def getWelcomeAdOpt(mnode: MNode): Future[Option[MWelcomeAd]] = {
    val waIdOpt = mnode.edges
      .withPredicateIterIds( MPredicates.NodeWelcomeAdIs )
      .toStream
      .headOption
    getWelcomeAdOpt(waIdOpt)
  }

  def updateWaImg(waOpt: Option[MWelcomeAd], newWaImgOpt: Option[MImgT]) = {
    val saveAllFut = ImgFormUtil.updateOrigImgFull(
      needImgs = newWaImgOpt.toSeq,
      oldImgs = waOpt
        .flatMap(_.imgs.headOption)
        .map { kv => mImg3(kv._2.filename) }
        .toIterable
    )
    saveAllFut map { _.headOption }
  }

  /** Обновление картинки и карточки приветствия. Картинка хранится в полу-рекламной карточке, поэтому надо ещё
    * обновить карточку и пересохранить её. */
  def updateWelcodeAdFut(adnNode: MNode, newWelcomeImgOpt: Option[MImgT]): Future[Option[String]] = {
    getWelcomeAdOpt(adnNode) flatMap { currWelcomeAdOpt =>
      updateWaImg(currWelcomeAdOpt, newWelcomeImgOpt) flatMap {
        // Новой картинки нет. Надо удалить старую карточку (если была), и очистить соотв. welcome-поле.
        case None =>
          val waIdOpt = adnNode.edges
            .withPredicateIterIds( MPredicates.NodeWelcomeAdIs )
            .toStream
            .headOption
          FutureUtil.optFut2futOpt(waIdOpt) { waId =>
            MWelcomeAd.deleteById(waId)
            .map { _ => None }
          }

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


  def welcomeAd2iik(waOpt: Option[MWelcomeAd]): Option[MImgT] = {
    waOpt
      .flatMap { _.imgs.headOption }
      .map[MImgT] { img => mImg3(img._2.filename) }
  }


  /**
   * Асинхронно собрать аргументы для рендера карточки приветствия.
   * @param mnode Узел, для которого нужно подготовить настройки рендера приветствия.
   * @param screen Настройки экрана, если есть.
   * @return Фьючерс с опциональными настройками. Если None, то приветствие рендерить не надо.
   */
  def getWelcomeRenderArgs(mnode: MNode, screen: Option[DevScreen])(implicit ctx: Context): Future[Option[WelcomeRenderArgsT]] = {
    val waIdOpt = {
      mnode.edges
        .withPredicateIterIds( MPredicates.NodeWelcomeAdIs )
        .toStream
        .headOption
    }
    val welcomeAdOptFut = FutureUtil.optFut2futOpt(waIdOpt)(MWelcomeAd.getById(_))

    // дедубликация кода. Можно наверное через Future.filter такое отрабатывать.
    def _colorBg = colorBg(mnode)

    // Получить параметры (метаданные) фоновой картинки из хранилища картирок.
    val bgFut = mnode.edges
      .withPredicateIterIds( MPredicates.GalleryItem )
      .toStream
      .headOption
      .fold[Future[Either[String, ImgUrlInfoT]]] {
        Future successful _colorBg
      } { bgImgFilename =>
        val oiik = mImg3(bgImgFilename)
        val fut0 = oiik.original.getImageWH
        lazy val logPrefix = s"getWelcomeRenderArgs(${mnode.idOrNull}): "
        fut0.map {
          case Some(meta) =>
            Right(bgCallForScreen(oiik, screen, meta))
          case _ =>
            trace(s"getWelcomeRenderArgs(${mnode.idOrNull}): no welcome bg WH for " + bgImgFilename)
            colorBg(mnode)
        }
        .recover { case ex: Throwable =>
          error(logPrefix + "Failed to read welcome image data", ex)
          _colorBg
        }
      }

    val fgImgOptFut = for {
      welcomeAdOpt <- welcomeAdOptFut
    } yield {
      for {
        welcomeAd <- welcomeAdOpt
        imgKey    <- welcomeAd.imgs.get(WELCOME_IMG_KEY)
      } yield {
        imgKey
      }
    }

    for {
      welcomeAdOpt <- welcomeAdOptFut
      bg1          <- bgFut
      fgImgOpt     <- fgImgOptFut
    } yield {
      val wra = MWelcomeRenderArgs(
        bg      = bg1,
        fgImage = fgImgOpt,
        fgText  = Some( mnode.meta.basic.name )
      )
      Some(wra)
    }
  }


  /** Собрать ссылку на фоновую картинку. */
  def bgCallForScreen(oiik: MImgT, screenOpt: Option[DevScreen], origMeta: ISize2di)(implicit ctx: Context): ImgUrlInfoT = {
    val oiik2 = oiik.original
    screenOpt
      .filter { _ => BG_VIA_DYN_IMG }
      .flatMap { scr =>
        scr.maybeBasicScreenSize.map(_ -> scr)
      }
      .fold [ImgUrlInfoT] {
        ImgUrlInfo(
          call = cdnUtil.dynImg(oiik2.fileName),
          meta = Some(origMeta)
        )
      } { case (bss, screen) =>
        val imOps = imConvertArgs(bss, screen)
        val dynArgs = oiik2.withDynOps(imOps)
        ImgUrlInfo(
          call = cdnUtil.dynImg(dynArgs),
          meta = Some(bss)
        )
      }
  }

  /** Сгенерить аргументы для генерации dyn-img ссылки. По сути, тут список параметров для вызова convert.
    * @param scrSz Размер конечной картинки.
    * @return Список ImOp в прямом порядке.
    */
  def imConvertArgs(scrSz: BasicScreenSize, screen: DevScreen): Seq[ImOp] = {
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
  private def colorBg(adnNode: MNode) = {
    val bgColor = adnNode.meta.colors.bg.fold(scUtil.SITE_BGCOLOR_DFLT)(_.code)
    Left(bgColor)
  }

}
