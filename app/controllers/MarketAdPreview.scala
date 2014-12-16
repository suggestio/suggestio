package controllers

import play.core.parsers.Multipart
import util.PlayMacroLogsImpl
import models._
import play.api.libs.concurrent.Execution.Implicits._
import util.FormUtil._
import play.api.data._
import util.acl._
import scala.concurrent.Future
import play.api.mvc.Request
import controllers.ad.MarketAdFormUtil
import MarketAdFormUtil._
import util.blocks.BlockMapperResult
import views.html.market.showcase._
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:11
 * Description: Контроллер для preview-функционала рекламных карточек. Используется в adForm-редакторе
 * для обновления рекламной карточки в реальном времени.
 */

object MarketAdPreview extends SioController with PlayMacroLogsImpl with TempImgSupport with BruteForceProtect {
  import LOGGER._

  /** Макс.длина загружаемой картинки в байтах. */
  val IMG_UPLOAD_MAXLEN_BYTES: Int = {
    val mib = configuration.getInt("ad.img.len.max.mib") getOrElse 40
    mib * 1024 * 1024
  }

  /** Объект, содержащий дефолтовые значения для preview-формы. Нужен для возможности простого импорта значений
    * в шаблон формы и для изоляции области видимости от другого кода. */
  object PreviewFormDefaults {
    /** Дефолтовый id картинки, когда она не задана. */
    def IMG_ID = "TODO_IMG_ID"   // TODO Нужен id для дефолтовой картинки.

    def TEXT_COLOR = "000000"
  }


  /** Генератор preview-формы. Форма совместима с основной формой, но более толерантна к исходным данным. */
  private def getPreviewAdFormM(blockM: Mapping[BlockMapperResult]): AdFormM = {
    MarketAd.getAdFormM(userCatIdOptM, blockM)
  }

  private def detectAdPreviewForm(adnNode: MAdnNode)(implicit request: Request[collection.Map[String, Seq[String]]]) = {
    maybeGetAdPreviewFormM(adnNode, request.body)
  }


  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def maybeGetAdPreviewFormM(adnNode: MAdnNode, reqBody: collection.Map[String, Seq[String]]): Either[AdFormM, (BlockConf, AdFormM)] = {
    // TODO adModes пора выпиливать. И этот Either заодно.
    val adModes = reqBody.getOrElse("ad.offer.mode", Nil)
    adModes.headOption.flatMap { adModeStr =>
      AdOfferTypes.maybeWithName(adModeStr)
    }.fold[Either[AdFormM, (BlockConf, AdFormM)]] {
      warn("detectAdForm(): valid AD mode not present in request body. AdModes found: " + adModes)
      val form = getPreviewAdFormM( BlocksConf.DEFAULT.strictMapping )
        .withGlobalError("ad.mode.undefined.or.invalid", adModes : _*)
      Left(form)
    } {
      case AdOfferTypes.BLOCK =>
        val maybeBlockIdRaw = reqBody.get("ad.offer.blockId")
        maybeBlockIdRaw
          .getOrElse(Nil)
          .headOption
          .map[BlockConf] { blockIdStr => BlocksConf(blockIdStr.toInt) }
          .filter { block => MarketAd.blockIdsFor(adnNode) contains block.id }
          .fold[Either[AdFormM, (BlockConf, AdFormM)]] {
            // Задан пустой или скрытый/неправильный block_id.
            warn("detectAdForm(): valid block_id not found, raw block ids = " + maybeBlockIdRaw)
            val form = getPreviewAdFormM( BlocksConf.DEFAULT.strictMapping )
              .withGlobalError("ad.blockId.undefined.or.invalid")
            Left(form)
          } { blockConf =>
            val result = blockConf -> getPreviewAdFormM(blockConf.strictMapping)
            Right(result)
          }
    }
  }

  /** Сабмит формы редактирования карточки для генерации превьюшки.
    * @param adnId id узла, в рамках которого происходит работа.
    * @param isFull true - надо полноэкранную преьюшку, false - нужен обычный размер.
    * @return 200 Ok с рендером.
    *         406 Not Acceptable при ошибочной форме.
    */
  def adFormPreviewSubmit(adnId: String, isFull: Boolean) = IsAdnNodeAdmin(adnId).async(parse.urlFormEncoded) { implicit request =>
    detectAdPreviewForm(request.adnNode) match {
      case Right((bc, adFormM)) =>
        adFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"adFormPreviewSubmit($adnId): form bind failed: " + formatFormErrors(formWithErrors))
            NotAcceptable("Preview form bind failed.")
          },
          {case (mad, bim) =>
            val imgsFut: Future[Imgs_t] = Future.traverse(bim) {
              case (k, i4s) =>
                i4s.getImageWH map {
                  imgMetaOpt  =>  k -> MImgInfo(i4s.fileName, meta = imgMetaOpt)
                }
            } map {
              _.toMap
            }
            mad.producerId = adnId
            for {
              imgs <- imgsFut
            } yield {
              mad.imgs = imgs
              val render = if (isFull) {
                val args = blk.RenderArgs(withEdit = true, isStandalone = false, szMult = 2)
                _single_offer_w_description(mad, producer = request.adnNode, args = args)
              } else {
                val args = blk.RenderArgs(withEdit = true, isStandalone = false, szMult = 1)
                _single_offer(mad, args = args)
              }
              Ok(render)
            }
          }
        )

      case Left(formWithGlobalError) =>
        NotAcceptable("Form mode invalid")
    }
  }



  override val BRUTEFORCE_TRY_COUNT_DIVISOR: Int = 3
  override val BRUTEFORCE_CACHE_PREFIX: String = "aip:"

  /** Подготовка картинки, которая загружается в динамическое поле блока. */
  def prepareBlockImg(blockId: Int, fn: String, wsId: Option[String]) = {
    val bp = parse.multipartFormData(Multipart.handleFilePartAsTemporaryFile, maxLength = IMG_UPLOAD_MAXLEN_BYTES.toLong)
    IsAuth.async(bp) { implicit request =>
      bruteForceProtected {
        val bc: BlockConf = BlocksConf(blockId)
        bc.blockFieldForName(fn) match {
          case Some(bfi: BfImage) =>
            val resultFut = _handleTempImg(
              preserveUnknownFmt = false,
              runEarlyColorDetector = bfi.preDetectMainColor,
              wsId = wsId
            )
            resultFut

          case _ => NotFound
        }
      }
    }
  }

}
