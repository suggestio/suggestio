package models

import util.DkeyModelT
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.concurrent.Future
import io.suggest.model.{MDomain => MDomainRaw}
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.10.13 12:26
 * Description: Обертка над моделью доменов для веб-интерфейса sio. Веб-интерфейс должен иметь модель, слинкованную с
 * другими моделями sioweb21, и чтобы только read-only.
 */
case class MDomain(underlying: MDomainRaw) extends DkeyModelT {
  def dkey = underlying.dkey
  def addedAt = underlying.addedAt
  def addedBy = underlying.addedBy
  def save = underlying.save

  @JsonIgnore
  override def domainOpt: Future[Option[MDomain]] = Future.successful(Some(this))

}


object MDomain {

  /** Конструктор сабжа. Используется при добавлении домена в базу. */
  def apply(dkey: String, addedBy: String): MDomain = {
    val dr = new MDomainRaw(dkey=dkey, addedBy=addedBy)
    MDomain(dr)
  }

  def getForDkey(dkey: String): Future[Option[MDomain]] = {
    MDomainRaw.getForDkey(dkey).map(_.map(MDomain(_)))
  }

  def getAll: Future[List[MDomain]] = {
    MDomainRaw.getAll.map(_.map(MDomain(_)))
  }

}
