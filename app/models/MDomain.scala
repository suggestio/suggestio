package models

import util.DkeyModelT
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.concurrent.Future
import io.suggest.model.{MDomain => MDomainRaw}
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.10.13 12:26
 * Description: Обертка над моделью доменов для веб-интерфейса sio. Веб-интерфейс должен иметь модель, слинкованную с
 * другими моделями sioweb21, и чтобы только read-only.
 */
case class MDomain(underlying: MDomainRaw) extends DkeyModelT {

  def this(dkey: String, addedBy: String, addedAt:DateTime = DateTime.now) = {
    this(new MDomainRaw(dkey=dkey, addedBy=addedBy, addedAt=addedAt))
  }

  def dkey = underlying.dkey
  def addedAt = underlying.addedAt
  def addedBy = underlying.addedBy
  def save = underlying.save

  @JsonIgnore
  override def domainOpt: Future[Option[MDomain]] = Future.successful(Some(this))

}


object MDomain {

  def getForDkey(dkey: String): Future[Option[MDomain]] = {
    MDomainRaw.getForDkey(dkey).map(_.map(MDomain(_)))
  }

  def getAll: Future[List[MDomain]] = {
    MDomainRaw.getAll.map(_.map(MDomain(_)))
  }

}
