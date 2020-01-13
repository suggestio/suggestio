package io.suggest.jd

import io.suggest.err.ErrorConstants
import io.suggest.file.MSrvFileInfo
import io.suggest.n2.edge.{EdgeUid_t, MPredicate, MPredicates}
import io.suggest.primo.id.IId
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import io.suggest.text.UrlUtil2
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Validation
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 17:41
  * Description: Модель инфа по эджу для jd-карточек.
  * Модель является кросс-платформенной.
  * Изначально, модель называлась MJdEditEdge, но очень плотно интегрировалась с рендером, и edit-назначение отпало.
  *
  * Модель повторяет своей структурой MEdge, но не содержит нерелевантных для jd-редактора полей.
  */
object MJdEdge {

  /** Названия полей модели для сериализации в JSON. */
  object Fields {
    val PREDICATE_FN    = "p"
    val UID_FN          = "i"
    val TEXT_FN         = "t"
    val URL_FN          = "u"
    val SRV_FILE_FN     = "f"
  }

  /** Поддержка play-json между клиентом и сервером. */
  implicit val MAD_EDIT_EDGE_FORMAT: OFormat[MJdEdge] = {
    val F = Fields
    (
      (__ \ F.PREDICATE_FN).format[MPredicate] and
      (__ \ F.UID_FN).format[EdgeUid_t] and
      (__ \ F.TEXT_FN).formatNullable[String] and
      (__ \ F.URL_FN).formatNullable[String] and
      (__ \ F.SRV_FILE_FN).formatNullable[MSrvFileInfo]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MJdEdge] = UnivEq.derive


  /** Валидация данных эджа для его сохранения в БД. */
  def validateForStore(e: MJdEdge): StringValidationNel[MJdEdge] = {
    // Тут несколько вариантов: текст, картинка, видео. Поэтому разветвляем валидацию.
    val P = MPredicates.JdContent
    val `PRED` = "pred"
    val `JD` = "jd"
    if (e.predicate ==>> P) {
      val errMsgF = ErrorConstants.emsgF( `JD` + `.` + "edge" )

      val predVld = {
        // TODO Тут какой-то быдлокод: недо-валидация jd-предиката, больше похожая на сравнивание с самим собой.
        val expectedPred = P.children
          .find(pred => e.predicate ==>> pred)
        Validation.liftNel(e.predicate)(!expectedPred.contains(_), errMsgF(`PRED` + `.` + ErrorConstants.Words.EXPECTED))
      }

      val idVld = Validation.success(e.id)

      val textVld = if (e.predicate ==>> P.Text ) {
        Validation.liftNel(e.text)(
          {textOpt =>
            !textOpt.exists { text =>
              text.nonEmpty && text.length < JdConst.MAX_TEXT_LEN
            }
          },
          errMsgF("len")
        )
      } else {
        ScalazUtil.liftNelNone(e.text, errMsgF("text" + `.` + ErrorConstants.Words.UNEXPECTED))
      }

      val urlVld = {
        val URL = "url"
        if (e.predicate ==>> P.Frame) {
          ScalazUtil.liftNelSome(e.url, errMsgF(URL + `.` + ErrorConstants.Words.EXPECTED)) { url =>
            // Просто проверка ссылки.
            UrlUtil2.validateUrl(url, errMsgF(URL + `.` + ErrorConstants.Words.INVALID))
            // Раньше был парсинг ссылки на видеохостинг, но фреймы стали более абстрактными.
            //val pr = videoExtUrlParsers.parse( videoExtUrlParsers.anySvcVideoUrlP, url )
            //ScalazUtil.liftParseResult( pr ) { _ => errMsgF(URL + `.` + ErrorConstants.Words.INVALID) }
            //  // На стадии валидации возвращаем ссылку на видео. TODO Возвращать пересобранную ссылку вместо оригинала?
            //  .map { _ => url }
          }
        } else {
          ScalazUtil.liftNelNone(e.url, errMsgF(URL + `.` + ErrorConstants.Words.UNEXPECTED))
        }
      }

      val fileSrvVld = {
        val FILE = "file"
        if (e.predicate ==>> P.Image) {
          ScalazUtil.liftNelSome(e.fileSrv, errMsgF(FILE + `.` + ErrorConstants.Words.MISSING))( MSrvFileInfo.validateC2sForStore )
        } else {
          ScalazUtil.liftNelNone(e.fileSrv, errMsgF(FILE))
        }
      }

      (predVld |@| idVld |@| textVld |@| urlVld |@| fileSrvVld)(apply)

    } else {
      // Не jd-предикат, а что-то иное.
      Validation.failureNel( ErrorConstants.EMSG_CODE_PREFIX + `JD` + `PRED` + `.` + ErrorConstants.Words.EXPECTED )
    }
  }

  val predicate = GenLens[MJdEdge](_.predicate)
  val id        = GenLens[MJdEdge](_.id)
  val text      = GenLens[MJdEdge](_.text)
  val url       = GenLens[MJdEdge](_.url)
  val fileSrv   = GenLens[MJdEdge](_.fileSrv)

}


/** Данные по эджу для редактируемого документа.
  *
  * @param predicate Предикат.
  * @param url Ссылка на ресурс, на картинку, например.
  * @param fileSrv КроссПлатформенная инфа по файлу-узлу на стороне сервера.
  */
case class MJdEdge(
                    // TODO Предикат вообще тут неуместен. Это уровень представления, его нужно унести на уровень id или просто удалить: jdEdge описывает один ресурс (файл, текст, etc), а куда его привязать - это уже другой уровень.
                    predicate           : MPredicate,   // TODO MPredicate заменить на MPredicates.JdContent.Child или что-то типа него.
                    override val id     : EdgeUid_t,
                    text                : Option[String] = None,
                    url                 : Option[String] = None,
                    fileSrv             : Option[MSrvFileInfo]  = None
                  )
  extends IId[EdgeUid_t]
{

  def fileSrvUrl = fileSrv.flatMap(_.url)


  /** Подобрать значение для imgSrc. */
  def imgSrcOpt: Option[String] = {
    // Стараемся использовать собственную ссылку в первую очередь.
    // Например, это полезно, когда есть base64-ссылка для нового файла, а сервер присылает ещё одну,
    // которую надо будет ждать... а зачем ждать, когда всё уже есть?
    url
      .orElse { fileSrvUrl }
  }

  /** Оригинал изображения -- он или на сервере (edit) или в url data:base64. */
  def origImgSrcOpt: Option[String] = {
    fileSrvUrl
      .orElse( url )
  }

}
