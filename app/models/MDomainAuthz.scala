package models

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.hadoop.fs.Path
import org.joda.time.DateTime
import util.DkeyContainerT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.06.13 17:15
 * Description: Междумодельное барахло для аутентификаций.
 */

trait MDomainAuthzT extends DkeyContainerT {
  def id: String
  def dkey: String
  def date_created: DateTime

  @JsonIgnore def personIdOpt: Option[String]
  @JsonIgnore def delete: Boolean
  @JsonIgnore def isValid: Boolean
  @JsonIgnore def isNeedRevalidation: Boolean
  @JsonIgnore def filepath: Path
  @JsonIgnore def isQiType: Boolean
  @JsonIgnore def isValidationType: Boolean
  @JsonIgnore def save: MDomainAuthzT

  @JsonIgnore def domain = MDomain.getForDkey(dkey).get
  @JsonIgnore def personOpt = personIdOpt.map(MPerson.getById)
  @JsonIgnore def bodyCodeOpt: Option[String] = None
}


// Временная увторизация для админа. Втыкается там, где оно надо и для служебных нужд.
case class MPersonDomainAuthzAdmin(
  person_id: String,
  dkey: String
) extends MDomainAuthzT {

  def id: String = ""
  lazy val date_created: DateTime = DateTime.now()

  def personIdOpt: Option[String] = Some(person_id)
  def delete: Boolean = false
  def isValid: Boolean = true
  def isNeedRevalidation: Boolean = false
  def filepath: Path = null
  def isQiType: Boolean = false
  def isValidationType: Boolean = false
  def save = this
}
