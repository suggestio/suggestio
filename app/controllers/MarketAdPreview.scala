package controllers

import util.{FormDataSerializer, PlayMacroLogsImpl}
import models._
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.FormUtil._
import play.api.data._
import util.acl._
import util.img._
import scala.concurrent.Future
import play.api.mvc.Request
import play.api.Play.current
import io.suggest.ym.parsers.Price
import controllers.ad.MarketAdFormUtil
import MarketAdFormUtil._
import util.blocks.BlockMapperResult
import io.suggest.ym.model.common.EMImg.Imgs_t
import views.html.market.showcase._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:11
 * Description: Контроллер для preview-функционала рекламных карточек. Используется в adForm-редакторе
 * для обновления рекламной карточки в реальном времени.
 */

object MarketAdPreview extends SioController with PlayMacroLogsImpl with TempImgSupport {
  import LOGGER._

  /** Объект, содержащий дефолтовые значения для preview-формы. Нужен для возможности простого импорта значений
    * в шаблон формы и для изоляции области видимости от другого кода. */
  object PreviewFormDefaults {
    /** Дефолтовый id картинки, когда она не задана. */
    val IMG_ID = "TODO_IMG_ID"   // TODO Нужен id для дефолтовой картинки.

    val TEXT_COLOR = "000000"
    val TEXT_FONT  = AOFieldFont(TEXT_COLOR)

    object Product {
      val PRICE_VALUE = Price(100F)
      val OLDPRICE_VALUE = Price(200F)
    }

    object Discount {
      val TPL_ID    = DISCOUNT_TPL_ID_MIN
      val DISCOUNT  = 50F
    }

    object Text {
      val TEXT = "Пример текста"
    }
  }


  /** Генератор preview-формы. Форма совместима с основной формой, но более толерантна к исходным данным. */
  private def getPreviewAdFormM(blockM: Mapping[BlockMapperResult]): AdFormM = {
    MarketAd.getAdFormM(userCatIdOptM, blockM)
  }

  private def detectAdPreviewForm(implicit request: Request[collection.Map[String, Seq[String]]]) = {
    maybeGetAdPreviewFormM(request.body)
  }


  /** Выбрать форму в зависимости от содержимого реквеста. Если ad.offer.mode не валиден, то будет Left с формой с global error. */
  private def maybeGetAdPreviewFormM(reqBody: collection.Map[String, Seq[String]]): Either[AdFormM, (BlockConf, AdFormM)] = {
    // TODO adModes пора выпиливать. И этот Either заодно.
    val adModes = reqBody.get("ad.offer.mode") getOrElse Nil
    adModes.headOption.flatMap { adModeStr =>
      AdOfferTypes.maybeWithName(adModeStr)
    }.fold[Either[AdFormM, (BlockConf, AdFormM)]] {
      warn("detectAdForm(): valid AD mode not present in request body. AdModes found: " + adModes)
      val form = getPreviewAdFormM(MarketAd.dfltBlock.strictMapping)
        .withGlobalError("ad.mode.undefined.or.invalid", adModes : _*)
      Left(form)
    } {
      case AdOfferTypes.BLOCK =>
        val maybeBlockIdRaw = reqBody.get("ad.offer.blockId")
        maybeBlockIdRaw
          .getOrElse(Nil)
          .headOption
          .map[BlockConf] { blockIdStr => BlocksConf(blockIdStr.toInt) }
          .filter(_.isShown)
          .fold[Either[AdFormM, (BlockConf, AdFormM)]] {
            // Задан пустой или скрытый/неправильный block_id.
            warn("detectAdForm(): valid block_id not found, raw block ids = " + maybeBlockIdRaw)
            val form = getPreviewAdFormM(MarketAd.dfltBlock.strictMapping)
              .withGlobalError("ad.blockId.undefined.or.invalid")
            Left(form)
          } { blockConf =>
            val result = blockConf -> getPreviewAdFormM(blockConf.strictMapping)
            Right(result)
          }
    }
  }

  def adFormPreviewSubmit(adnId: String, isFull: Boolean) = IsAdnNodeAdmin(adnId).async(parse.urlFormEncoded) { implicit request =>
    detectAdPreviewForm match {
      case Right((bc, adFormM)) =>
        adFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"adFormPreviewSubmit($adnId): form bind failed: " + formatFormErrors(formWithErrors))
            NotAcceptable("Preview form bind failed.")
          },
          {case (mad, bim) =>
            val imgsFut: Future[Imgs_t] = Future.traverse(bim) {
              case (k, i4s) =>
                previewPrepareImgMeta(i4s.iik) map {
                  imgMetaOpt  =>  k -> MImgInfo(i4s.iik.filename, meta = imgMetaOpt)
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
                _single_offer_w_description(mad, producer = request.adnNode)
              } else {
                _single_offer(mad)
              }
              Ok(render)
            }
          }
        )

      case Left(formWithGlobalError) =>
        NotAcceptable("Form mode invalid")
    }
  }

  /** Награбить метаданные по картинке для генерации превьюшки. */
  private def previewPrepareImgMeta(iik: ImgIdKey): Future[Option[MImgInfoMeta]] = {
    iik match {
      case tiik: TmpImgIdKey =>
        Future successful ImgFormUtil.getMetaForTmpImgCached(tiik)
      case oiik: OrigImgIdKey if oiik.meta.isDefined =>
        Future successful oiik.meta
      case oiik: OrigImgIdKey =>
        // Метаданных нет, но данные уже в базе. Надо бы прочитать метаданные из таблицы
        oiik.getImageWH
          .recover {
            case ex: Exception =>
              error(s"previewPrepareImgMeta($iik): Failed to fetch img metadata from hbase", ex)
              None
          }
    }
  }


  /** Экшен смены блока редактора. */
  def adBlockSwitchEditor(adnId: String) = IsAdnNodeAdmin(adnId).apply(parse.urlFormEncoded) { implicit request =>
    detectAdPreviewForm match {
      case Right((bc, newAdForm)) =>
        // Для улучшения восстановления значений при переключении между формами разных блоков, используем сериализацию состояния формы в hidden-поле редактора.
        val prevFormData: Map[String, String] = request.body.get("formData")
          .flatMap(_.headOption)
          .flatMap(FormDataSerializer.deserializeDataSafe)
          .getOrElse {
            request.body
              .mapValues(_.headOption)
              .filter(!_._2.isEmpty)
              .mapValues(_.get)
            }
        val oldBindResultOpt = request.body.get("ad.offer.oldBlockId")
          // Отрабатываем, если нет старого blockId. Такое маловероятно, скорее всего юзер выпилил соотв. input из формы.
          .flatMap { _.headOption }
          // Пытаемся забиндить форму старого блока и получить её значения.
          .map { oldBlockIdStr =>
            // Невалидность blockId считаем нештатной ситуацией, спровоцированной юзером.
            val oldBlockId = oldBlockIdStr.toInt
            val oldBc: BlockConf = BlocksConf(oldBlockId)
            val oldForm = getPreviewAdFormM(oldBc.strictMapping)
            val oldFormBinded = oldForm.bindFromRequest()
            val vOpt = oldFormBinded.fold(
              {oldFormWithErrors =>
                debug(s"adBlockSwitchEditor(): Failed to bind OLD blockId=$oldBlockId form: ${formatFormErrors(oldFormWithErrors)}")
                None
              },
              { Some.apply }
            )
            oldFormBinded -> vOpt
          }
        val formBinded0: AdFormM = oldBindResultOpt
          .flatMap(_._2)
          .fold
            { newAdForm.bindFromRequest() }    // Если старая форма не прокатила, то накатить все данные на новую форму.
            { newAdForm.fill }                 // Если старая форма схватилась, то залить её результаты в новую форму.
        val formBinded = newAdForm
          .bind(prevFormData ++ formBinded0.data)
          .discardingErrors
        val newFormData = (prevFormData ++ formBinded.data)
          .filter {
            case (k, v)  =>  !v.isEmpty && k != "ad.catId" && k != "ad.offer.blockId"
          }
        val formDataSer = FormDataSerializer.serializeData(newFormData)
        Ok(bc.renderEditor(formBinded, formDataSer = Some(formDataSer)))

      case Left(formWithErrors) =>
        Ok(views.html.blocks.editor._blockEditorTpl(formWithErrors))
    }
  }



  /** Подготовка картинки, которая загружается в динамическое поле блока. */
  def prepareBlockImg(blockId: Int, fn: String) = IsAuth.apply(parse.multipartFormData) { implicit request =>
    val bc: BlockConf = BlocksConf(blockId)
    bc.blockFieldForName(fn) match {
      case Some(bfi: BfImage) =>
        _handleTempImg(bfi.imgUtil, Some(bfi.marker))

      case _ => NotFound
    }
  }

}
