package util.img

import java.nio.ByteBuffer
import java.util.UUID

import io.suggest.util.UuidUtil
import net.sf.jmimemagic.Magic
import org.im4java.core.Info
import play.api.mvc.QueryStringBindable
import util.{AsyncUtil, FormUtil, PlayMacroLogsImpl}
import io.suggest.img.{ImgCropParsers, ConvertModes, ImgCrop, SioImageUtilT}
import play.api.Play.{current, configuration}
import io.suggest.model.MPict
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.{File, FileNotFoundException}
import org.apache.commons.io.FileUtils
import scala.util.{Failure, Success}
import java.lang
import com.fasterxml.jackson.annotation.JsonIgnore
import models._
import play.api.cache.Cache
import io.suggest.ym.model.common.{MImgSizeT, MImgInfoT}
import play.api.Logger
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */

object ImgFormUtil extends PlayMacroLogsImpl {
  import play.api.data.Forms._
  import play.api.data.Mapping
  import LOGGER._

  type LogoOpt_t = Option[ImgInfo4Save[ImgIdKey]]

  private val IIK_MAXLEN = 80

  // Ключи в карте MUserImgMeta, которые хранят данные о картинке.
  val IMETA_WIDTH  = "w"
  val IMETA_HEIGHT = "h"

  /** Включение ревалидации уже сохраненных картинок при обновлении позволяет убирать картинки "дырки",
    * появившиеся в ходе ошибочной логики. */
  private val REVALIDATE_ALREADY_SAVED_IMGS = configuration.getBoolean("img.update.revalidate.already.saved") getOrElse false


  /** Маппер для поля с id картинки. Используется обертка над id чтобы прозрачно различать tmp и orig картинки. */
  val imgIdM: Mapping[ImgIdKey] = {
    nonEmptyText(minLength = 8, maxLength = IIK_MAXLEN)
      .transform[ImgIdKey](ImgIdKey.apply, _.filename)
      .verifying("img.id.invalid.", { _.isValid })
  }

  /** маппер для поля с id картинки, который может отсутствовать. */
  val imgIdOptM: Mapping[Option[ImgIdKey]] = {
    optional(text(maxLength = IIK_MAXLEN))
      .transform[Option[ImgIdKey]](
        {txtOpt =>
          try {
            txtOpt
              .filter(_.length >= 8)
              .map(ImgIdKey.apply)
          } catch {
            case ex: Exception =>
              debug("imgIdOptM.apply: Cannot parse img id key: " + txtOpt, ex)
              None
          }
        },
        { _.map(_.filename) }
      )
  }


  /** Маппер для поля с id картинки-логотипа, но результат конвертируется в ImgInfo. */
  private def logoImgIdM(_imgIdM: Mapping[ImgIdKey]) = _imgIdM
    .transform(
      { ImgInfo4Save(_, withThumb = false) },
      { ii: ImgInfo4Save[ImgIdKey] => ii.iik }
    )

  /** Проверяем tmp-файл на использование jpeg. Уже сохраненные id не проверяются. */
  private val imgIdJpegM = imgIdM
    .verifying("img.fmt.invalid", { iik => iik match {
      case tiik: TmpImgIdKey =>
        tiik.mptmp.data.fmt == OutImgFmts.JPEG
      case oiik: OrigImgIdKey =>
        true
    }})

  private val LOGO_IMG_ID_K = "logoImgId"


  /** Синхронно проверить переданный img id key насколько это возможно. */
  private def checkIIK(iik: ImgIdKey, marker: String): Boolean = {
    iik match {
      case tiik: TmpImgIdKey =>
        tiik.mptmp
          .data.markerOpt
          .exists(_ == marker)

      case _ => true
    }
  }

  /** Собрать маппинг для id изображения, промаркированного известным маркером, либо уже сохранённый orig. */
  private def imgIdMarkedM(errorMsg: String, marker: String): Mapping[ImgIdKey] = {
    imgIdM.verifying(errorMsg, checkIIK(_, marker))
  }

  /** Аналог [[imgIdMarkedM]], но функция толерантна к ошибкам, и без ошибок отсеивает некорректные img id. */
  private def imgIdMarkedOptM(marker: String): Mapping[Option[ImgIdKey]] = {
    imgIdOptM.transform[Option[ImgIdKey]](
      { _.filter(checkIIK(_, marker)) },
      { identity }
    )
  }

  /** Генератор logo-маппингов. */
  def getLogoKM(errorMsg: String, marker: String): (String, Mapping[LogoOpt_t]) = {
    val imgIdM = imgIdMarkedM(errorMsg, marker = marker)
    val logoImgInfoM = ImgFormUtil.logoImgIdM(imgIdM)
    LOGO_IMG_ID_K -> optional(logoImgInfoM)
  }


  // Валидация значений crop'а.
  /** Минимальный размер откропанной стороны. */
  private val CROP_SIDE_MIN_PX = configuration.getInt("img.crop.side.max") getOrElse 10
  /** Максимальный размер откропанной стороны. */
  private val CROP_SIDE_MAX_PX = configuration.getInt("img.crop.side.max") getOrElse 2048
  /** Максимальный размер сдвига относительно левого верхнего узла. */
  private val CROP_OFFSET_MAX_PX = configuration.getInt("img.crop.offset.max") getOrElse 4096

  /** Проверка одной стороны кропа на соотвествие критериям. */
  private def isCropSideValid(sideVal: Int): Boolean = {
    sideVal >= CROP_SIDE_MIN_PX  &&  sideVal <= CROP_SIDE_MAX_PX
  }
  /** Проверка одного параметра сдвига кропа на соотвествие критериям. */
  private def isCropOffsetValid(offsetVal: Int): Boolean = {
    offsetVal >= 0 && offsetVal <= CROP_OFFSET_MAX_PX
  }

  /** Маппинг обязательного параметра кропа на реальность. */
  def imgCropM: Mapping[ImgCrop] = {
    nonEmptyText(minLength = 4, maxLength = 16)
      .transform[Option[ImgCrop]] (ImgCrop.maybeApply, _.map(_.toCropStr).getOrElse(""))
      .verifying("crop.invalid", _.isDefined)
      .transform[ImgCrop](_.get, Some.apply)
      .verifying("crop.height.invalid",   {crop => isCropSideValid(crop.height)} )
      .verifying("crop.width.invalid",    {crop => isCropSideValid(crop.width)} )
      .verifying("crop.offset.x.invalid", {crop => isCropOffsetValid(crop.offX)} )
      .verifying("crop.offset.y.invalid", {crop => isCropOffsetValid(crop.offY)} )
  }


  private def imgCropOptM: Mapping[Option[ImgCrop]] = {
    val txtM = text(maxLength = 16).transform(FormUtil.strTrimSanitizeLowerF, FormUtil.strIdentityF)
    optional(txtM)
      .transform[Option[String]] (_.filter(!_.isEmpty), identity)
      .transform[Option[ImgCrop]] (_.flatMap(ImgCrop.maybeApply), _.map(_.toCropStr))
  }


  def updateOrigImgId(needImgs: Seq[ImgInfo4Save[ImgIdKey]], oldImgIds: Iterable[String]): Future[List[OrigImgIdKey]] = {
    updateOrigImgFull(needImgs, oldImgIds.map(MImgInfo(_)))
  }

  /** Комбо из updateOrigImgFull() и miiPreferFirstCropped(). */
  def updateOrigImg(needImgs: Seq[ImgInfo4Save[ImgIdKey]], oldImgs: Iterable[MImgInfoT]): Future[Option[OrigImgIdKey]] = {
    updateOrigImgFull(needImgs, oldImgs)
      .map { miiPreferFirstCropped }
  }

  /**
   * Замена иллюстраций к некоей сущности.
   * @param needImgs Необходимые в результате набор картинок. Тут могут быть как уже сохранённыя картинка,
   *                 так и новая из tmp. Если Nil, то старые картинки будут удалены.
   * @param oldImgs Уже сохранённые ранее картинки, если есть.
   * @return Список id новых и уже сохранённых картинок.
   */
  private def updateOrigImgFull(needImgs: Seq[ImgInfo4Save[ImgIdKey]], oldImgs: Iterable[MImgInfoT]): Future[List[OrigImgIdKey]] = {
    // Защита от какой-либо деятельности в случае полного отсутствия входных данных.
    if (needImgs.isEmpty && oldImgs.isEmpty) {
      Future successful Nil
    } else {
      updateOrigImgFullDo(needImgs, oldImgs = oldImgs)
    }
  }
  private def updateOrigImgFullDo(needImgs: Seq[ImgInfo4Save[ImgIdKey]], oldImgs: Iterable[MImgInfoT]): Future[List[OrigImgIdKey]] = {
    // newTmpImgs - это одноразовый итератор, содержит исходные индексы и списки картинок для сохранения.
    val newTmpImgs = needImgs
      .iterator
      .zipWithIndex
      // В новых (произведённых) картинках есть только добавленные картинки.
      .filter { case (ii4s, i)  =>  ii4s.iik.isInstanceOf[TmpImgIdKey] }
      // 2014.05.08: Нужно сохранять ещё и исходную tmp-картинку, если передана откадрированная tmp-картинка.
      .map { case (ii4sRaw, i) =>
        val ti4s = ii4sRaw.asInstanceOf[ImgInfo4Save[TmpImgIdKey]]
        val results = List(ti4s)
        results -> i
      }
    // Запустить сохранение tmp-картинок.
    val savedTmpImgsFut = Future.traverse(newTmpImgs) { case (tiis, i) =>
      Future.traverse(tiis) { tii =>
        // 08.oct.2014: На сохранение отправляем только некропанные оригиналы.
        //              TODO Нужно вынести работу с кропом куда-то за пределы этого метода.
        val tiiUncropped = tii.copy(iik = tii.iik.uncropped)
        val fut = handleTmpImageForStoring(tiiUncropped)
          .map { sti0 =>
            // Восстановить кроп
            val sti1 = sti0.copy(
              tii4s = tii
            )
            Some(sti1 -> i)
          }
        fut onComplete { case tryResult =>
          tii.iik.mptmp.file.delete()
          tmpMetaCacheInvalidate(tii.iik)
        }
        fut recover {
          case ex: Exception =>
            error(s"Failed to store picture " + tii, ex)
            None
        }
      }
    } map { savedTmpImgsOrNull =>
      // Строим результат сохранения новых tmp-картинок.
      savedTmpImgsOrNull
        .flatten // Разворачиваем list'ы сохранённых tmp-картинок в разных размерах
        .flatten // Развернуть Option'ы, которые подавляют ошибки сохранения.
        .map { case (savedImg, i) =>
          // 2014.oct.08: Вписываем исходный cropOpt, которого в сохранённых по сути нет.
          // Это нужно, чтобы вернуть данные о картинке назад без потерь данных о ней.
          // TODO Этот метод с кропом вообще не связан теперь (2014.oct.08), надо как-то вынести это всё за его пределы.
          val oiik0 = savedImg.oiik
          val odata1 = oiik0.data.copy(
            cropOpt = savedImg.tii4s.iik.cropOpt
          )
          val oiik1 = OrigImgIdKey(odata1, oiik0.meta)
          oiik1 -> i
        }
        .toList
    }
    // Какие картинки надо оставить с прошлого раза и отфорвардить в результаты
    val needOrigImgs1 = needImgs
      .iterator
      .zipWithIndex
      .flatMap { case v @ (ii4s, i) =>
        if (ii4s.iik.isInstanceOf[OrigImgIdKey])
          Seq( v.asInstanceOf[(ImgInfo4Save[OrigImgIdKey], Int)] )  // TODO Стрёмная оптимизация, т.к. asInstanceOf неизбежен. Но может есть метод лучше?
        else
          Nil
      }
      // Из старых картинок выбрать подходящую уже сохранённую, если она там есть.
      .flatMap {
        case (ti4s, i) =>
          oldImgs.find { oii => ti4s.iik == oii }
            .map { _ -> i }
      }
      .map { case (miit, i)  =>  (miit: OrigImgIdKey) -> i }
      .toList

    // 2014.09.18: Из-за бага с удалением ненужных картинок, была введена валидация уже сохранённых orig img, подлежащих повторному сохранению.
    // Это нужно будет отключить, когда "дырки" в галереях карточек и узлов исчезнут. Валидация уже сохранённых картинок сильно замедляет сохранение.
    val needOrigImgsFilteredFut = if (REVALIDATE_ALREADY_SAVED_IMGS) {
      Future.traverse(needOrigImgs1) { case v @ (oiit, _) =>
        try {
          MUserImgMeta2.getByStrId(oiit.data.rowKey, oiit.origQualifierOpt)
            .map {
              // Всё ок, картинка скорее всего есть в базе, пропускаем.
              case Some(_) =>
                Some(v)
              // Ревалидация выявила проблему: картинки нет в базе. Затираем весь ряд для надежности.
              case None =>
                eraseOiik(oiit)
                warn("Revalidate: Found invalid orig img reference: " + oiit + " -- Reerasing it and forgetting.")
                None
            }
        } catch {
          case ex: Exception => Future successful None
        }
      } map {
        _.flatMap(identity(_))
      }
    } else {
      Future successful needOrigImgs1
    }

    // Восстановить исходный порядок needImgs на основе исходных индексов, собрать финальный результат метода.
    val resultFut = for {
      newSavedImgs  <- savedTmpImgsFut
      needOrigImgs2 <- needOrigImgsFilteredFut
    } yield {
      // 2014.sep.19: Бывает ситуация, когда картинка была сохранена оригиналом, а потом откропана и снова сохранена под тем же rowkey. Нужно устранять дубликаты.
      val needOrigImgs3 = needOrigImgs2
        .filter { case (oiit, _) =>
          !newSavedImgs.exists {
            case (miitNew, _) =>
              miitNew.data.rowKey == oiit.data.rowKey
          }
        }
      (newSavedImgs.iterator ++ needOrigImgs3.iterator)
        .toStream
        // Вышеуказанная проблема всё равно присутствует...
        .groupBy(_._1.data.rowKey)
        // Отфильтровываем orig-картинку при наличии кропаной версии.
        .mapValues(_.sortBy(_._1.filename).reverse.head)
        .values
        .toList
        .sortBy(_._2)
        .map(_._1)
    }
    // Если сохранение удалось, то надо запустить в фоне удаление старых картинок.
    resultFut onSuccess { case _ =>
      needOrigImgsFilteredFut onSuccess { case needOrigImgs2 =>
        // 2014.sep.18: Нужно удалять по ключам и сравнивать по ключам, без учёта кропов всяких и т.д.
        val delOldImgs = oldImgs.filterNot { miit =>
          val oiik: OrigImgIdKey = miit
          needOrigImgs2.exists { needOI =>
            needOI._1.data.rowKey  ==  oiik.data.rowKey
          }
        }
        if (delOldImgs.nonEmpty)
          debug("Will delete unused imgs: " + delOldImgs.mkString(", "))
        Future.traverse(delOldImgs)(eraseOiik)
      }
    }
    // Вернуть результат.
    resultFut
  }


  private def eraseOiik(oiik: MImgInfoT): Future[_] = {
    val rowKey = oiik.data.rowKey
    val fut = Future.traverse(Seq(MUserImg2, MUserImgMeta2, MImgThumb2)) { imodel =>
      imodel.deleteByStrId(rowKey)
    }
    fut onComplete {
      case Success(res) => trace("Old img deleted: " + oiik + " result: " + res)
      case Failure(ex)  => error("Failed to delete old img " + oiik, ex)
    }
    fut
  }

  /**
   * В контроллер приходит сабмит по картинкам. Для tmp-картинок надо отправить их целиком в orig-хранилище.
   * Для любых картинок надо обновить нарезку, применив crop и отправив в хранилище.
   * @param tii4s Исходная tmp-картинка.
   * @return Фьючерс, содержащий imgId в виде строки.
   */
  def handleTmpImageForStoring(tii4s: ImgInfo4Save[TmpImgIdKey]): Future[SavedTmpImg] = {
    import tii4s.iik.mptmp
    lazy val logPrefix = s"handleTmpImageForStoring(${tii4s.iik.filename}): "
    trace(s"${logPrefix}Starting: crop=${tii4s.iik.cropOpt}, withThumb=${tii4s.withThumb} withId=${tii4s.withId}")
    val (rowkeyStr, rowkey) = tii4s.withId match {
      case Some(_rowkeyStr) =>
        _rowkeyStr -> UuidUtil.base64ToUuid(_rowkeyStr)
      case None =>
        val _rowkey = UUID.randomUUID()
        val _rowkeyStr = UuidUtil.uuidToBase64(_rowkey)
        _rowkeyStr -> _rowkey
    }
    // Запускаем сбор данных по финальной сохраняемой картинке
    // 26.sep.2014: TODO svg-картинки не имеют размера, но identify как-то его определяет.
    val identifyResult = OrigImageUtil.identify(mptmp.file)
    // Параметр кропа отображаем на выходной размер фотки.
    val crop4nameOpt: Option[ImgCrop] = None
    // qualifier для сохранения картинки и её метаданных
    val q = ImgIdKey.origQualifier(crop4nameOpt)
    val saveOrigFut: Future[OrigImgData] = future {
      OrigImageUtil.maybeReadFromFile(mptmp.file)
    } flatMap { imgBytes =>
      MUserImg2(id = rowkey, img = ByteBuffer.wrap(imgBytes), q = q)
        .save
        .map { _ =>
          OrigImgData(rowkeyStr, cropOpt = crop4nameOpt)
        }
    }
    saveOrigFut onComplete {
      case Success(result) => trace(logPrefix + "Orig img saved ok. Result = " + result)
      case Failure(ex)     => error(logPrefix + "Failed to save img.", ex)
    }
    // 26.mar.2014: понадобился доступ к метаданным картинки в контексте элемента. Запускаем identify в фоне
    val imgMetaFut: Future[MImgInfoMeta] = {
      // 2014.04.22: Сохранение метаданных в HBase для доступа в ad-preview.
      val md = identifyInfo2md(identifyResult)
      MUserImgMeta2(id = rowkey, md = md, q = q)
        .save
        .map { _ => MImgInfoMeta(height = identifyResult.getImageHeight,  width = identifyResult.getImageWidth) }
    }
    imgMetaFut onComplete {
      case Success(result) => trace(logPrefix + "Img metadata saved ok: " + result)
      case Failure(ex)     => error(logPrefix + "Failed to save img metadata.", ex)
    }
    // Если указано withThumb, то пора сгенерить thumbnail без учёта кропов всяких.
    val saveThumbFut: Future[_] = if (tii4s.withThumb) {
      val stFut = future {
        val tmpThumbFile = File.createTempFile("origThumb", ".jpeg")
        val thumbBytes = try {
          ThumbImageUtil.convert(mptmp.file, tmpThumbFile, mode = ConvertModes.THUMB)
          // Прочитать файл в памяти и сохранить в MImgThumb
          FileUtils.readFileToByteArray(tmpThumbFile)
        } finally {
          tmpThumbFile.delete()
        }
        new MImgThumb2(
          id = rowkey,
          img = ByteBuffer.wrap(thumbBytes),
          imageUrl = None
        )
      } flatMap { thumbDatum =>
        thumbDatum.save
      }
      stFut onComplete {
        case Success(_)  => trace(logPrefix + "Img thumb saved ok")
        case Failure(ex) => error(logPrefix + "Failed to save img thumbnail", ex)
      }
      stFut
    } else {
      Future successful ()
    }
    // Связываем все асинхронные задачи воедино
    for {
      _          <- saveThumbFut
      origData   <- saveOrigFut
      imgMeta    <- imgMetaFut
    } yield {
      SavedTmpImg(origData, tii4s, imgMeta)
    }
  }


  /**
   * Проверить и уточнить значение кропа картинки.
   * До 2014.oct.08 была замечена проблема с присылаемым на сервер кропом, а после dyn-img это обрушивало всю красоту системы.
   * Баг был в том, что кроп уходил за пределы картинки.
   * @param crop Кроп, присланный клиентом.
   * @param targetSz Целевой размер картинки. TODO 2014.10.08 Размер не обязательно корректный, т.к. приходит с клиента.
   * @param srcSz Исходный размер картинки.
   * @return Пофикшенный crop.
   */
  def repairCrop(crop: ImgCrop, targetSz: MImgSizeT, srcSz: MImgSizeT): ImgCrop = {
    var newCrop = crop
    // Пофиксить offset'ы кропа. Кроп может по сдвигу уезжать за пределы допустимой ширины/длины.
    if (crop.offX + crop.width > srcSz.width)
      newCrop = crop.copy(offX = srcSz.width - crop.width)
    if (crop.offY + crop.height > srcSz.height)
      newCrop = crop.copy(offY = srcSz.height - crop.height)
    newCrop
  }


  def identifyInfo2md(info: Info): Map[String, String] = {
    Map(
      IMETA_WIDTH  -> info.getImageWidth.toString,
      IMETA_HEIGHT -> info.getImageHeight.toString
    )
  }

  /**
   * Т.к. updateOrigImg() возвращает список результатов, хотя подразумевается только один, то надо
   * выделять нужную картинку из выхлопа сохраненных картинок.
   * Так, при кадрировании возвращается две сохранённые картинки (исходная и кадрированная) вместо одной или нуля.
   * @param l Выхлоп updateOrigImg()
   * @return Опционально выбранная картинка.
   */
  // TODO 08.oct.2014: Возможно, этот метод больше не нужен, т.к. crop отрабатывается через dynImg,
  //      а update...() метод не возвращает множеств результатов, как это было ранее.
  private def miiPreferFirstCropped(l: List[OrigImgIdKey]): Option[OrigImgIdKey] = {
    l.reduceOption { (oiik1, oiik2) =>
      (oiik1.isCropped, oiik2.isCropped) match {
        case (true, false) =>
          oiik1
        case (false, true) =>
          oiik2
        // Внезапно обе откропленные картинки. Выбираем ту, что имеет название по-длинее
        case _ =>
          trace(s"miiPreferFirstCropped($l): reduce(): Both imgs are cropped/uncropped. Selecting one with longest filename...")
          List(oiik1, oiik2)
            .map { oiik => oiik.filename -> oiik }
            .sortBy(_._1)
            .last
            ._2
      }
    }
  }


  /**
   * Асинхронное удаление отработанных времененных файлов.
   * @param imgs Поюзанные картинки.
   * @return Фьючерс для синхронизации.
   */
  def rmTmpImgs(imgs: Seq[SavedTmpImg]): Future[_] = {
    future {
      imgs.foreach { img =>
        try {
          img.tmpImgFile.delete()
        } catch {
          case ex: FileNotFoundException =>
            trace("File already deleted: " + img.tmpImgFile.getAbsolutePath)
          case ex: Throwable => error("Failed to delete file " + img.tmpImgFile.getAbsolutePath, ex)
        }
      }
    }
  }

  /** Для временной картинки прочитать данные для постоения объекта метаданных. */
  def getMetaForTmpImg(img: TmpImgIdKey): Option[MImgInfoMeta] = {
    val info = OrigImageUtil.identify(img.mptmp.file)
    val result = MImgInfoMeta(height = info.getImageHeight, width = info.getImageWidth)
    Some(result)    // TODO Option -- тут аттавизм, пока остался, потом надо будет удалить.
  }

  def getTmpMetaCacheKey(id: String) = id + ".tme"
  val TMP_META_EXPIRE_SEC: Int = configuration.getInt("img.tmp.meta.cache.expire.seconds") getOrElse 40

  def getMetaForTmpImgCached(img: TmpImgIdKey): Option[MImgInfoMeta] = {
    val ck = getTmpMetaCacheKey(img.mptmp.data.key)
    Cache.getOrElse(ck, TMP_META_EXPIRE_SEC) {
      getMetaForTmpImg(img)
    }
  }

  def tmpMetaCacheInvalidate(tii: TmpImgIdKey) {
    val ck = getTmpMetaCacheKey(tii.filename)
    Cache.remove(ck)
  }


  /** Приведение выхлопа мапперов imgId к результату сохранения, минуя это самое сохранение. */
  implicit def logoOpt2imgInfo(logoOpt: LogoOpt_t): Option[MImgInfo] = {
    logoOpt.map { logo => MImgInfo(logo.iik.filename) }
  }

  /** Конвертер данных из готовых MImgInfo в [[OrigImgIdKey]]. */
  implicit def imgInfo2imgKey(m: MImgInfoT): OrigImgIdKey = {
    m match {
      case oiik: OrigImgIdKey =>
        oiik
      case _ =>
        OrigImgIdKey(m.filename, m.meta)
    }
  }

}

/** Базовый трейт для sioweb-реализаций image util. */
sealed trait SiowebImageUtilT extends SioImageUtilT with PlayMacroLogsImpl


/** Резайзилка картинок, используемая для генерация "оригиналов", т.е. картинок, которые затем будут кадрироваться. */
object OrigImageUtil extends SiowebImageUtilT {
  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override val MIN_SZ_PX: Int = configuration.getInt("img.orig.sz.min.px") getOrElse 256

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override val MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = {
    val kib = configuration.getInt("img.org.preserve.src.max.len.kib").getOrElse(90)
    Some(kib * 1024L)
  }

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = configuration.getDouble("img.orig.jpeg.quality") getOrElse 90.0

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  override val DOWNSIZE_HORIZ_PX: Integer  = Integer valueOf (configuration.getInt("img.orig.maxsize.h.px") getOrElse 2048)
  override val DOWNSIZE_VERT_PX:  Integer  = configuration.getInt("img.orig.maxsize.v.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  override def GAUSSIAN_BLUG: Option[lang.Double] = None
}


object ThumbImageUtil extends SiowebImageUtilT {
  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (configuration.getInt("img.thumb.maxsize.h.px") getOrElse 256)
  val DOWNSIZE_VERT_PX : Integer = configuration.getInt("img.thumb.maxsize.h.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override def MIN_SZ_PX: Int = DOWNSIZE_HORIZ_PX.intValue()

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = configuration.getDouble("img.thumb.jpeg.quality") getOrElse 85.0
}


/** Обработка логотипов. */
object AdnLogoImageUtil extends SiowebImageUtilT {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (configuration.getInt("img.logo.shop.maxsize.h.px") getOrElse 512)
  val DOWNSIZE_VERT_PX: Integer  = Integer valueOf (configuration.getInt("img.logo.shop.maxsize.v.px") getOrElse 128)

  /** Качество сжатия jpeg. */
  val JPEG_QUALITY_PC: Double = configuration.getDouble("img.logo.shop.jpeg.quality") getOrElse 0.95

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  val MIN_SZ_PX: Int = configuration.getInt("img.logo.shop.side.min.px") getOrElse 30

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None
}


/** Там, где допустимы квалратные логотипы, используем этот трайт. */
trait SqLogoImageUtil extends SiowebImageUtilT {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (configuration.getInt("img.logo.mart.maxsize.h.px") getOrElse 512)
  val DOWNSIZE_VERT_PX: Integer  = configuration.getInt("img.logo.mart.maxsize.v.px").map(Integer valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Качество сжатия jpeg. */
  val JPEG_QUALITY_PC: Double = configuration.getDouble("img.logo.mart.jpeg.quality") getOrElse 0.95

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  val MIN_SZ_PX: Int = configuration.getInt("img.logo.mart.side.min.px") getOrElse 70

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  val MAX_OUT_FILE_SIZE_BYTES: Option[Int] = configuration.getInt("img.logo.mart.result.size.max")

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

}


/** Конвертор картинок в логотипы ТЦ. */
object LogoImageUtil extends SqLogoImageUtil


@deprecated("Use models.im.MImg instead", "2014.oct.27")
object ImgIdKey {
  def apply(key: String): ImgIdKey = {
    if (key startsWith MPictureTmp.KEY_PREFIX) {
      TmpImgIdKey(key)
    } else {
      OrigImgIdKey(key)
    }
  }

  def origQualifier(cropOpt: Option[ImgCrop]) = cropOpt.fold(MPict.Q_USER_IMG_ORIG) { _.toCropStr }


  /** QSB для id картинки. */
  implicit def iikQsb(implicit strB: QueryStringBindable[String]) = {
    new QueryStringBindable[ImgIdKey] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ImgIdKey]] = {
        strB.bind(key, params) map {
          _.right.flatMap { rawIik =>
            try {
              Right(ImgIdKey.apply(rawIik))
            } catch {
              case ex: Throwable =>
                Left("Img id is invalid.")
            }
          }
        }
      }

      override def unbind(key: String, value: ImgIdKey): String = {
        strB.unbind(key, value.filename)
      }
    }
  }

}

@deprecated("Use models.im.MImg instead", "2014.oct.27")
sealed trait ImgIdKey {
  def filename: String
  def isExists: Future[Boolean]
  def isValid: Boolean
  def isTmp: Boolean
  override def hashCode = filename.hashCode
  def cropOpt: Option[ImgCrop]
  def getBaseImageWH: Future[Option[MImgInfoMeta]]
  def getImageWH: Future[Option[MImgInfoMeta]]

  /** Метаданные картинки в некотором очевидном виде. */
  def getImageMeta: Future[Option[ImgMetaI]]

  /** Выдать во временный файл MPictureTmp. */
  def toTempPict: Future[MPictureTmp]

  /** Выдать оригинал во временный файл MPictureTmp. */
  def toTempPictOrig: Future[MPictureTmp]

  // Определение column qualifier для сохранения/чтения картинки по этому ключу.
  def origQualifierOpt = cropOpt.map { _.toCropStr }
  def origQualifier = ImgIdKey.origQualifier(cropOpt)

  /** Является ли эта картинка кадрированной производной?
    * @return false, если картинка оригинальная. true если откадрированная картинка.
    */
  def isCropped = cropOpt.isDefined

  def uncropped: ImgIdKey
}


@deprecated("Use models.im.MImg instead", "2014.oct.27")
object TmpImgIdKey {

  val GET_IMAGE_WH_CACHE_DURATION = configuration.getInt("tiik.getImageWH.cache.minutes").getOrElse(1).minutes

  def apply(filename: String): TmpImgIdKey = {
    val mptmp = MPictureTmp(filename)
    TmpImgIdKey(filename, mptmp)
  }
  def apply(mptmp: MPictureTmp): TmpImgIdKey = {
    val filename = mptmp.filename
    TmpImgIdKey(filename, mptmp)
  }

}

@deprecated("Use models.im.MImg instead", "2014.oct.27")
case class TmpImgIdKey(filename: String, @JsonIgnore mptmp: MPictureTmp) extends ImgIdKey with MImgInfoT {

  @JsonIgnore
  override def cropOpt = mptmp.data.cropOpt

  @JsonIgnore
  override def isTmp: Boolean = true

  @JsonIgnore
  override def isExists: Future[Boolean] = Future successful isValid

  @JsonIgnore
  override def isValid: Boolean = mptmp.isExist

  @JsonIgnore
  override def getBaseImageWH: Future[Option[MImgInfoMeta]] = {
    // TODO Нужно считать размер для неоткропанной картинки, если текущая уже откропана.
    getImageWH
  }

  @JsonIgnore
  override def meta: Option[MImgInfoMeta] = None

  @JsonIgnore
  override def getImageWH: Future[Option[MImgInfoMeta]] = {
    // Кеширование позволяет избежать замеров размеров картинки.
    Cache.getOrElse(filename + ".tgIWH", expiration = TmpImgIdKey.GET_IMAGE_WH_CACHE_DURATION.toSeconds.toInt) {
      // Для синхронного вызова identify() используем отдельный поток в отдельном пуле.
      // Распараллеливание заодно поможет сразу закинуть в кеш данный фьючерс.
      val identifyFut = Future {
        val info = OrigImageUtil.identify(mptmp.file)
        val imeta = MImgInfoMeta(
          height = info.getImageHeight,
          width = info.getImageWidth
        )
        Some(imeta)
      }(AsyncUtil.jdbcExecutionContext)
      identifyFut recover {
        case ex: org.im4java.core.InfoException =>
          Logger(getClass).info("getImageWH(): Unable to identity image " + filename, ex)
          None
      }
    }
  }

  /** Метаданные картинки в некотором очевидном виде. */
  override def getImageMeta: Future[Option[ImgMetaI]] = {
    val whOptFut = getImageWH
    val timestamp = mptmp.file.lastModified()
    whOptFut map { whOpt =>
      whOpt map { wh =>
        new ImgMetaI {
          override lazy val md: Map[String, String] = {
            Map(
              ImgFormUtil.IMETA_WIDTH  ->  wh.width.toString,
              ImgFormUtil.IMETA_HEIGHT ->  wh.height.toString
            )
          }

          override val timestampMs: Long = timestamp
        }
      }
    }
  }

  @JsonIgnore
  override def uncropped: TmpImgIdKey = {
    if (isCropped) {
      val mptmpData1 = mptmp.data.copy(cropOpt = None)
      val mptmp1 = MPictureTmp(mptmpData1)
      TmpImgIdKey(mptmp1)
    } else {
      this
    }
  }

  @JsonIgnore
  override def toTempPict: Future[MPictureTmp] = {
    Future successful mptmp
  }

  @JsonIgnore
  override def toTempPictOrig: Future[MPictureTmp] = {
    Future successful uncropped.mptmp
  }
}


@deprecated("Use models.im.MImg instead", "2014.oct.27")
object OrigImgIdKey extends ImgCropParsers {
  import ImgFormUtil.{IMETA_HEIGHT, IMETA_WIDTH}

  val FILENAME_PARSER: Parser[OrigImgData] = {
    "(?i)[0-9a-z_-]+".r ~ opt("~" ~> cropStrP) ^^ {
      case rowKey ~ cropOpt =>
        OrigImgData(rowKey, cropOpt)
    }
  }

  val IMAGE_WH_CACHE_DURATION = configuration.getInt("oiik.getImageWH.cache.minutes").getOrElse(1).minutes

  def parseFilename(filename: String) = parse(FILENAME_PARSER, filename)

  def apply(filename: String): OrigImgIdKey = {
    apply(filename, meta = None)
  }
  def apply(filename: String, meta: Option[MImgInfoMeta]): OrigImgIdKey = {
    val data = OrigImgIdKey.parseFilename(filename).get
    apply(filename, meta, data)
  }
  def apply(data: OrigImgData): OrigImgIdKey = {
    apply(data, meta = None)
  }
  def apply(data: OrigImgData, meta: Option[MImgInfoMeta]): OrigImgIdKey = {
    val filename = data.toFilename
    apply(filename, meta, data)
  }
  def apply(filename: String, data: OrigImgData): OrigImgIdKey = {
    apply(filename, meta = None, data = data)
  }

  def apply(filename: String, meta: Option[MImgInfoMeta], data: OrigImgData): OrigImgIdKey = {
    new OrigImgIdKey(filename, meta, data)
  }

  def getOrigImageMetaCache(rowKey: String, qOpt: Option[String] = None): Future[Option[MUserImgMeta2]] = {
    // Кеширование сильно ускоряет получение параметров картинки из базы на параллельных и последовательных запросах.
    Cache.getOrElse("gOIWH." + rowKey + qOpt.getOrElse(""),  expiration = IMAGE_WH_CACHE_DURATION.toSeconds.toInt) {
      MUserImgMeta2.getByStrId(rowKey, qOpt)
    }
  }

  /** Прочитать ширину-длину хранимой orig-картинки по модели MUserImgMetadata.
    * Метод довольно статичен, но private чтобы не допускать логических ошибок при передаче параметров
    * (ведь можно ошибочно передать [[OrigImgIdKey.filename]] например -- функция будет вести себя ошибочно при crop).
    * @param rowKey Чистый ключ картинки. Доступен через [[OrigImgIdKey.data.rowKey]].
    * @return Асинхронные метаданные по ширине-высоте картинки.
    */
  def getOrigImageWH(rowKey: String, qOpt: Option[String] = None): Future[Option[MImgInfoMeta]] = {
    getOrigImageMetaCache(rowKey, qOpt)
      .map { imetaOpt =>
        for {
          imeta     <- imetaOpt
          widthStr  <- imeta.md.get(IMETA_WIDTH)
          heightStr <- imeta.md.get(IMETA_HEIGHT)
        } yield {
          MImgInfoMeta(height = heightStr.toInt, width = widthStr.toInt)
        }
      }
      // TODO Можно попытаться прочитать картинку из хранилища, если метаданные по картинке не найдены.
  }

}

// TODO cropOpt нужно убрать отсюда, т.к. кропы старого формата больше не хранятся.
@deprecated("Use Seq[ImOp] instead", "2014.oct.27")
case class OrigImgData(rowKey: String, cropOpt: Option[ImgCrop]) {
  def toFilename: String = {
    var sbSz: Int = rowKey.length
    if (cropOpt.isDefined)
      sbSz += 22
    val sb = new StringBuilder(sbSz, rowKey)
    if (cropOpt.isDefined)
      sb.append('~').append(cropOpt.get.toUrlSafeStr)
    sb.toString()
  }
}

/**
 * Экземпляр ключа картинки, которая лежит в хранилище на постоянном хранении.
 * 2014.oct.08: отключен case-class, т.к. вызов copy() приводил к противоречиям в состоянии класса.
 * @param filename Имя файла. Описывает содержимое data.
 * @param meta Метаданные
 * @param data Данные хранения. Ключ ряда например.
 */
@deprecated("Use models.im.MImg instead", "2014.oct.27")
class OrigImgIdKey(val filename: String, val meta: Option[MImgInfoMeta], val data: OrigImgData)
  extends MImgInfoT with ImgIdKey
{
  @JsonIgnore
  override def isTmp: Boolean = false

  @JsonIgnore
  override def isExists: Future[Boolean] = {
    MUserImg2.getByStrId(data.rowKey, q = None).map(_.isDefined)
  }

  @JsonIgnore
  override def isValid: Boolean = UuidUtil.isUuidStrValid(filename)

  @JsonIgnore
  override def equals(obj: scala.Any): Boolean = {
    // Сравниваем с MImgInfo без учёта поля meta.
    obj match {
      case mii: MImgInfoT => (mii eq this) || (mii.filename == this.filename)
      case _ => false
    }
  }

  /** Метаданные картинки в некотором очевидном виде. */
  override def getImageMeta = OrigImgIdKey.getOrigImageMetaCache(data.rowKey, origQualifierOpt)

  @JsonIgnore
  override def cropOpt = data.cropOpt

  @JsonIgnore
  override def getBaseImageWH = OrigImgIdKey.getOrigImageWH(data.rowKey)

  override def getImageWH = OrigImgIdKey.getOrigImageWH(data.rowKey, origQualifierOpt)

  override def uncropped: OrigImgIdKey = {
    if (isCropped) {
      val data1 = data.copy(cropOpt = None)
      val fn1 = data1.toFilename
      OrigImgIdKey(fn1, meta, data1)
    } else {
      this
    }
  }

  protected def _toTempPict(qOpt: Option[String]): Future[MPictureTmp] = {
    // Кеширование позволяет не выкачивать одну и ту же картинку снова, когда она уже лежит на диске.
    // TODO Следует использовать кеш прямо на диске, имена файлов должны состоять из rowKeyStr и qOpt.
    //      Это разгрузит RAM ценой небольшого iowait. В остальном всё должно быть также, как и с mptmp.
    val ck = filename + ".tTP"
    Cache.getAs[Future[MPictureTmp]](ck) match {
      case Some(resultFut) =>
        // TODO Следует ли отрабатывать ситуацию, когда приходит неправильный фьючерс (файл внезапно уже удалён)?
        // При чтении из кеша можно делать touch на файле и продлевать ttl кеша.
        resultFut onSuccess { case mptmp =>
          mptmp.touch()
          Cache.set(ck, resultFut, expiration = MPictureTmp.DELETE_AFTER)
        }
        resultFut

      case None =>
        // Нужно выкачать из модели оригинальную картинку во временный файл.
        val resultFut = MUserImg2.getByStrId(this.data.rowKey, qOpt) map { oimgOpt =>
          val oimg = oimgOpt.get
          val magicMatch = Magic.getMagicMatch(oimg.imgBytes)
          val oif = OutImgFmts.forImageMime(magicMatch.getMimeType).get
          val mptmp = MPictureTmp.mkNew(None, cropOpt = None, oif)
          mptmp.writeIntoFile(oimg.imgBytes)
          mptmp
        }
        Cache.set(ck, resultFut, expiration = MPictureTmp.DELETE_AFTER)
        resultFut
    }
  }

  override def toTempPict: Future[MPictureTmp] = {
    _toTempPict(origQualifierOpt)
  }

  override def toTempPictOrig: Future[MPictureTmp] = {
    _toTempPict(None)
  }

}


/** Выходные форматы картинок. */
object OutImgFmts extends Enumeration {

  protected class Val(val name: String, val mime: String) extends super.Val(name)

  type OutImgFmt = Val

  val JPEG: OutImgFmt = new Val("jpeg", "image/jpeg")
  val PNG: OutImgFmt  = new Val("png", "image/png")
  val GIF: OutImgFmt  = new Val("gif", "image/gif")
  val SVG: OutImgFmt  = new Val("svg", "image/svg+xml")

  implicit def value2val(x: Value): OutImgFmt = x.asInstanceOf[OutImgFmt]

  /**
   * Предложить формат для mime-типа.
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def forImageMime(mime: String): Option[OutImgFmt] = {
    values
      .find(_.mime equalsIgnoreCase mime)
      .asInstanceOf[Option[OutImgFmt]]
  }

}


/**
 * Класс для объединения кропа и id картинки (чтобы не использовать Tuple2 с числовыми названиями полей)
 * @param iik Указатель на картинку.
 * @param withThumb Генерить ли превьюшку? (она будет сохранена рядом).
 * @param withId Использовать указанный id. Если None, то будет сгенерен новый рандомный id.
 * @tparam T Реальный тип iik.
 */
@deprecated("Use models.im.MImg instead", "2014.oct.27")
case class ImgInfo4Save[+T <: ImgIdKey](
  iik       : T,
  withThumb : Boolean = true,
  withId    : Option[String] = None
)


@deprecated("Use models.im.MImg instead", "2014.oct.27")
case class SavedTmpImg(origData: OrigImgData, tii4s: ImgInfo4Save[TmpImgIdKey], meta: MImgInfoMeta) {
  def idStr: String = origData.toFilename
  def oiik = OrigImgIdKey(origData, Some(meta))

  def tmpImgFile = tii4s.iik.mptmp.file
}
