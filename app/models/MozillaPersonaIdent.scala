package models

import io.suggest.model._
import models.MPersonIdent.IdTypes
import EsModel._
import scala.collection.Map
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client
import util.PlayMacroLogsImpl
import io.suggest.event.SioNotifierStaticClientI
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 14:45
 * Description: compat-модель для работы с ident'ами mozilla persona.
 * Эти иденты использовались в erlang-версии s.io live search. В 2014 mozilla отказалась от поддержки,
 * а 29 августа 2014 был произведён отказ от erlang-версии s.io live search.
 */


/** Идентификации от mozilla-persona. */
object MozillaPersonaIdent extends MPersonIdentSubmodelStatic with PlayMacroLogsImpl {

  override type T = MozillaPersonaIdent

  override val ES_TYPE_NAME = "mpiMozPersona"

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MozillaPersonaIdent(
      personId = stringParser( m(PERSON_ID_ESFN) ),
      email    = stringParser( m(KEY_ESFN) )
    )
  }

}


final case class MozillaPersonaIdent(
  email     : String,
  personId  : String
) extends MPersonIdent with MPersonLinks with MPIWithEmail {

  override type T = MozillaPersonaIdent

  /** Сгенерить id. Если допустить, что тут None, то _id будет из взят из поля key согласно маппингу. */
  override def id: Option[String] = Some(email)
  override def key = email
  override def idType = IdTypes.MOZ_PERSONA
  override def value = None
  override def isVerified = true
  override def writeVerifyInfo = false
  override def companion = MozillaPersonaIdent
  override def versionOpt = None
}


// JMX
trait MozillaPersonaIdentJmxMBean extends EsModelJMXMBeanI
final class MozillaPersonaIdentJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MozillaPersonaIdentJmxMBean
{
  override def companion = MozillaPersonaIdent
}
