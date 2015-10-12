package io.suggest.model.n2.media.storage

import com.google.inject.{Singleton, Inject}
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.swfs.client.proto.get.GetRequest
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.libs.functional.syntax._
import MStorages.STYPE_FN_FORMAT

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:55
 * Description: Поддержка хранилища SeaWeedFS.
 * @see [[https://github.com/chrislusf/seaweedfs]]
 */
@Singleton
class SwfsStorage_ @Inject() (implicit val client: ISwfsClient) {

  val FID_FORMAT = (__ \ MStorFns.FID.fn).format[Fid]

  val READS: Reads[SwfsStorage] = (
    STYPE_FN_FORMAT.filter { _ == MStorages.SeaWeedFs } and
    FID_FORMAT
  ) { (_, fid) =>
    apply(fid)
  }

  val WRITES: OWrites[SwfsStorage] = (
    (STYPE_FN_FORMAT: OWrites[MStorage]) and
    FID_FORMAT
  ) { ss =>
    (ss.sType, ss.fid)
  }

  implicit val FORMAT = Format(READS, WRITES)

  def apply(fid: Fid): SwfsStorage = {
    SwfsStorage(fid, this)
  }

  /** Получить у swfs-мастера координаты для сохранения нового файла. */
  def assingNew()(implicit ec: ExecutionContext): Future[SwfsStorage] = {
    for( resp <- client.assign() ) yield {
      apply(resp.fidParsed)
    }
  }

}


case class SwfsStorage(fid: Fid, companion: SwfsStorage_)
  extends IMediaStorage
{

  import companion._

  override def sType = MStorages.SeaWeedFs

  override def toJson = Json.toJson(this)

  override def read(implicit ec: ExecutionContext): Enumerator[Array[Byte]] = {
    //val req = GetRequest()
    //companion.client.get()
    ???
  }

  override def delete(implicit ex: ExecutionContext): Future[_] = ???

  override def write(data: Enumerator[Array[Byte]])(implicit ec: ExecutionContext): Future[_] = ???

  override def isExist(implicit ec: ExecutionContext): Future[Boolean] = ???

}
