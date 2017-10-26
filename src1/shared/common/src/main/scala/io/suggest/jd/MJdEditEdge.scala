package io.suggest.jd

import io.suggest.err.ErrorConstants
import io.suggest.file.MSrvFileInfo
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicate, MPredicates}
import io.suggest.primo.id.IId
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import io.suggest.vid.ext.{VideoExtUrlParsers, VideoExtUrlParsersT}
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scalaz.Validation
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 17:41
  * Description: Модель инфа по эджу для редактора карточек.
  * Модель является кросс-платформенной, но на сервере существует лишь на входе и выходе из jdoc-редакторов.
  *
  * Модель повторяет своей структурой MEdge, но не содержит нерелевантных для jd-редактора полей.
  */
object MJdEditEdge {

  /** Названия полей модели для сериализации в JSON. */
  object Fields {
    val PREDICATE_FN    = "p"
    val UID_FN          = "i"
    val TEXT_FN         = "t"
    val URL_FN          = "u"
    val SRV_FILE_FN     = "f"
  }

  /** Поддержка play-json между клиентом и сервером. */
  implicit val MAD_EDIT_EDGE_FORMAT: OFormat[MJdEditEdge] = {
    val F = Fields
    (
      (__ \ F.PREDICATE_FN).format[MPredicate] and
      (__ \ F.UID_FN).format[EdgeUid_t] and
      (__ \ F.TEXT_FN).formatNullable[String] and
      (__ \ F.URL_FN).formatNullable[String] and
      (__ \ F.SRV_FILE_FN).formatNullable[MSrvFileInfo]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MJdEditEdge] = UnivEq.derive


  /** Валидация данных эджа для его сохранения в БД. */
  def validateForStore(e: MJdEditEdge,
                       videoExtUrlParsers: VideoExtUrlParsersT = new VideoExtUrlParsers): StringValidationNel[MJdEditEdge] = {
    // Тут несколько вариантов: текст, картинка, видео. Поэтому разветвляем валидацию.
    val P = MPredicates.JdContent
    if (e.predicate ==>> P) {
      val errMsgF = ErrorConstants.emsgF( "jd.edge" )

      val predVld = {
        // TODO Тут какой-то быдлокод: недо-валидация jd-предиката, больше похожая на сравнивание с самим собой.
        val expectedPred = P.children
          .find(pred => e.predicate ==>> pred)
        Validation.liftNel(e.predicate)(!expectedPred.contains(_), errMsgF("pred" + `.` + ErrorConstants.Words.EXPECTED))
      }

      val idVld = Validation.success(e.id)

      val textVld = if (e.predicate ==>> P.Text ) {
        Validation.liftNel(e.text)({ textOpt => !textOpt.exists { text => text.nonEmpty && text.length < JdConst.MAX_TEXT_LEN } }, errMsgF("len"))
      } else {
        ScalazUtil.liftNelNone(e.text, errMsgF("text" + `.` + ErrorConstants.Words.UNEXPECTED))
      }

      val urlVld = {
        val URL = "url"
        if (e.predicate ==>> P.Video) {
          ScalazUtil.liftNelSome(e.url, errMsgF(URL + `.` + ErrorConstants.Words.EXPECTED)) { url =>
            //Раньше было так: UrlUtil2.validateUrl(url, errMsgF(URL + `.` + ErrorConstants.Words.INVALID))
            // Теперь тут проверка включая видео-хостинг и id видео:
            val pr = videoExtUrlParsers.parse( videoExtUrlParsers.anySvcVideoUrlP, url )
            ScalazUtil.liftParseResult( pr ) { _ => errMsgF(URL + `.` + ErrorConstants.Words.INVALID) }
              // На стадии валидации возвращаем ссылку на видео. TODO Возвращать пересобранную ссылку вместо оригинала?
              .map { _ => url }
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
      Validation.failureNel( ErrorConstants.EMSG_CODE_PREFIX + "pred" + `.` + ErrorConstants.Words.EXPECTED )
    }
  }

}


/** Данные по эджу для редактируемого документа.
  *
  * @param predicate Предикат.
  * @param url Ссылка на ресурс, на картинку, например.
  * @param fileSrv КроссПлатформенная инфа по файлу-узлу на стороне сервера.
  */
case class MJdEditEdge(
                        predicate           : MPredicate,   // TODO MPredicate заменить на MPredicates.JdContent.Child или что-то типа него.
                        override val id     : EdgeUid_t,
                        text                : Option[String] = None,
                        url                 : Option[String] = None,
                        fileSrv             : Option[MSrvFileInfo]  = None
                      )
  extends IId[EdgeUid_t]
{

  def withText(text: Option[String])      = copy(text = text)
  def withUrl(url: Option[String])        = copy(url = url)
  def withFileSrv(fileSrv: Option[MSrvFileInfo]) = copy(fileSrv = fileSrv)

  /** Подобрать значение для imgSrc. */
  def imgSrcOpt: Option[String] = {
    // Стараемся использовать собственную ссылку в первую очередь.
    // Например, это полезно, когда есть base64-ссылка для нового файла, а сервер присылает ещё одну,
    // которую надо будет ждать... а зачем ждать, когда всё уже есть?
    url
      .orElse { fileSrv.flatMap(_.url) }
  }

}