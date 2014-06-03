package util.qsb

import play.api.mvc.QueryStringBindable
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.14 18:54
 * Description:
 */
object SMJoinAnswers {

  def haveWifiSuf     = ".haveWifi"
  def fullCoverSuf    = ".fullCover"
  def knownEquipSuf   = ".knownEquip"
  def altFwSuf        = ".altFw"
  def wrtFwSuf        = ".wrtFw"
  def landlineInetSuf = ".landlineInet"
  def smallRoomSuf    = ".smallRoom"
  def audSzSuf        = ".audSz"

  implicit def queryStringBinder(implicit boolOptBinder: QueryStringBindable[Option[Boolean]], strOptBinder: QueryStringBindable[Option[String]]) = {
    import QsbUtil._
    new QueryStringBindable[SMJoinAnswers] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SMJoinAnswers]] = {
        for {
          maybeHaveWifi     <- boolOptBinder.bind(key + haveWifiSuf, params)
          maybeFullCover    <- boolOptBinder.bind(key + fullCoverSuf, params)
          maybeKnownEquip   <- boolOptBinder.bind(key + knownEquipSuf, params)
          maybeAltFw        <- boolOptBinder.bind(key + altFwSuf, params)
          maybeWrtFw        <- boolOptBinder.bind(key + wrtFwSuf, params)
          maybeLandlineInet <- boolOptBinder.bind(key + landlineInetSuf, params)
          maybeSmallRoom    <- boolOptBinder.bind(key + smallRoomSuf, params)
          maybeAudSz        <- strOptBinder.bind(key  + audSzSuf, params)
        } yield {
          val maybeAudSz1 = maybeAudSz.flatMap {
            audSzStr => AudienceSizes.maybeWithName(audSzStr)
          }
          Right(SMJoinAnswers(
            haveWifi = maybeHaveWifi,
            fullCoverage = maybeFullCover,
            knownEquipment = maybeKnownEquip,
            altFw = maybeAltFw,
            isWrtFw = maybeWrtFw,
            landlineInet = maybeLandlineInet,
            smallRoom = maybeSmallRoom,
            audienceSz = maybeAudSz1
          ))
        }
      }

      override def unbind(key: String, value: SMJoinAnswers): String = {
        List(
          boolOptBinder.unbind(key + haveWifiSuf, value.haveWifi),
          boolOptBinder.unbind(key + fullCoverSuf, value.fullCoverage),
          boolOptBinder.unbind(key + knownEquipSuf, value.knownEquipment),
          boolOptBinder.unbind(key + altFwSuf, value.altFw),
          boolOptBinder.unbind(key + wrtFwSuf, value.isWrtFw),
          boolOptBinder.unbind(key + landlineInetSuf, value.landlineInet),
          boolOptBinder.unbind(key + smallRoomSuf, value.smallRoom),
          strOptBinder.unbind(key + audSzSuf, value.audienceSz.map(_.toString))
        )
          .filter { q => !q.isEmpty && !q.endsWith("=") }
          .mkString("&")
      }
    }
  }
}

case class SMJoinAnswers(
  haveWifi        : Option[Boolean],
  fullCoverage    : Option[Boolean],
  knownEquipment  : Option[Boolean],
  altFw           : Option[Boolean],
  isWrtFw         : Option[Boolean],
  landlineInet    : Option[Boolean],
  smallRoom       : Option[Boolean],
  audienceSz      : Option[AudienceSize]
)
