package models

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.hadoop.fs.Path
import org.joda.time.DateTime
import util.DkeyModelT
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.06.13 17:15
 * Description: Междумодельное барахло для аутентификаций.
 */

trait MDomainAuthzT extends DkeyModelT {
  def id: String
  def dkey: String
  def dateCreated: DateTime

  // Доступ к храниищу модели
  @JsonIgnore def save: Future[_]
  @JsonIgnore def delete: Future[Any]

  @JsonIgnore def personIdOpt: Option[String]
  @JsonIgnore def isValid: Boolean
  @JsonIgnore def isNeedRevalidation: Boolean
  @JsonIgnore def isQiType: Boolean
  @JsonIgnore def isValidationType: Boolean

  @JsonIgnore def personOpt = personIdOpt.map(MPerson.getById)
  @JsonIgnore def bodyCodeOpt: Option[String]
}


// Временная aвторизация для админа. Втыкается там, где оно надо и для служебных нужд.
case class MPersonDomainAuthzAdmin(
  person_id: String,
  dkey: String
) extends MDomainAuthzT {

  def id: String = ""
  lazy val dateCreated: DateTime = DateTime.now()

  def save = Future.successful(this)
  def delete = Future.successful(false)

  def personIdOpt: Option[String] = Some(person_id)
  def isValid: Boolean = true
  def isNeedRevalidation: Boolean = false
  def isQiType: Boolean = false
  def isValidationType: Boolean = false
  def bodyCodeOpt: Option[String] = None
}
