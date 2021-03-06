package io.suggest.jd

import io.suggest.common.empty.EmptyUtil
import io.suggest.err.ErrorConstants
import io.suggest.file.MSrvFileInfo
import io.suggest.n2.edge.{EdgeUid_t, MEdgeDoc, MPredicate, MPredicates}
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.primo.id.OptId
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import io.suggest.text.UrlUtil2
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Validation
import scalaz.syntax.apply._
import scala.language.implicitConversions

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
    val NODE_ID_FN      = "n"
    val EDGE_DOC_FN     = "d"
    val URL_FN          = "u"
    val SRV_FILE_FN     = "f"
  }

  /** Поддержка play-json между клиентом и сервером. */
  implicit val MAD_EDIT_EDGE_FORMAT: OFormat[MJdEdge] = {
    val F = Fields
    (
      (__ \ F.PREDICATE_FN).format[MPredicate] and
      (__ \ F.NODE_ID_FN).formatNullable[String] and
      (__ \ F.EDGE_DOC_FN).formatNullable[MEdgeDoc]
        .inmap[MEdgeDoc](
          EmptyUtil.opt2ImplMEmptyF( MEdgeDoc ),
          EmptyUtil.implEmpty2OptF
        ) and
      (__ \ F.URL_FN).formatNullable[String] and
      (__ \ F.SRV_FILE_FN).formatNullable[MSrvFileInfo]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MJdEdge] = UnivEq.derive


  def validateEdgeDoc(p: MPredicate, e: MEdgeDoc, errMsg0: => String): StringValidationNel[MEdgeDoc] = {
    val errMsgF = ErrorConstants.emsgF( errMsg0 + `.` + "doc" )

    // При сохранении jd-полей, uid всегда должен быть задан.
    val idVld = Validation.liftNel(e.id)(
      _.isEmpty,
      errMsgF("uid" + `.`  + ErrorConstants.Words.MISSING)
    )

    val textVld = if (p ==>> MPredicates.JdContent.Text) {
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

    (idVld |@| textVld)(MEdgeDoc.apply)
  }

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
        Validation.liftNel(e.predicate)(
          !expectedPred.contains(_),
          errMsgF(`PRED` + `.` + ErrorConstants.Words.EXPECTED)
        )
      }

      // Сейчас валидируется файл-эдж?
      val isFile = e.predicate ==>> P.Image

      def eNodeId = "e.nodeid."
      val nodeIdVld = ScalazUtil.liftNelOptMust(
        e.nodeId,
        mustBeSome = isFile || (e.predicate ==>> P.Ad),
        reallyMust = true,
        error = errMsgF( eNodeId + ErrorConstants.Words.MISSING ),
      ) { nodeId =>
        Validation.liftNel(nodeId)(
          !_.matches("^[a-z0-9A-Z_-]{10,50}$"),
          eNodeId + ErrorConstants.Words.INVALID + ": " + e.nodeId
        )
      }

      val edgeDocVld = validateEdgeDoc(e.predicate, e.edgeDoc, errMsgF("doc"))

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
        if (isFile) {
          ScalazUtil.liftNelSome(e.fileSrv, errMsgF(FILE + `.` + ErrorConstants.Words.MISSING))( MSrvFileInfo.validateNodeIdOnly )
        } else {
          ScalazUtil.liftNelNone(e.fileSrv, errMsgF(FILE))
        }
      }

      (predVld |@| nodeIdVld |@| edgeDocVld |@| urlVld |@| fileSrvVld)(apply)

    } else {
      // Не jd-предикат, а что-то иное.
      Validation.failureNel( ErrorConstants.EMSG_CODE_PREFIX + `JD` + `PRED` + `.` + ErrorConstants.Words.EXPECTED )
    }
  }

  def predicate = GenLens[MJdEdge](_.predicate)
  def nodeId    = GenLens[MJdEdge](_.nodeId)
  def edgeDoc   = GenLens[MJdEdge](_.edgeDoc)
  def url       = GenLens[MJdEdge](_.url)
  def fileSrv   = GenLens[MJdEdge](_.fileSrv)

  implicit def jdEdge2idOpt(e: MJdEdge): Option[EdgeUid_t] =
    implicitly[OptId[EdgeUid_t] => Option[EdgeUid_t]]
      .apply( e.edgeDoc )

}


/** Данные по эджу для редактируемого документа.
  *
  * @param predicate Предикат.
  * @param nodeId id связанного узла, обязателен для fileSrv.
  * @param url Ссылка на ресурс, на картинку, например.
  * @param fileSrv КроссПлатформенная инфа по файлу-узлу на стороне сервера.
  */
final case class MJdEdge(
                          predicate           : MPredicate,
                          nodeId              : Option[String]          = None,
                          edgeDoc             : MEdgeDoc                = MEdgeDoc.empty,
                          url                 : Option[String]          = None,
                          fileSrv             : Option[MSrvFileInfo]    = None,
                        ) {

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
