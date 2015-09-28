package io.suggest.model

import java.util.UUID

import io.suggest.event.SioNotifier.{Classifier, Event}
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.util.UuidUtil

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.10.14 10:32
 * Description: Утиль для обобщенной работы с моделями MUserImg2.
 */

object MImg2Util {

  /**
   * Удалить картинку из всех img-моделей кассандры.
   * @param imgId filename картинки.
   * @return Фьючерс для синхронизации.
   */
  def deleteFully(imgId: String)(implicit ec: ExecutionContext, sn: SioNotifierStaticClientI): Future[_] = {
    // Нужно извлечь uuid из id
    val rowKey = UuidUtil.base64ToUuid(imgId)
    deleteFully(rowKey)
  }
  def deleteFully(rowKey: UUID)(implicit ec: ExecutionContext, sn: SioNotifierStaticClientI): Future[_] = {
    val delImgFut = MUserImg2.deleteById(rowKey)
    delImgFut onSuccess { case _ =>
      sn publish Img2FullyDeletedEvent(rowKey)
    }
    val delMetaFut = MUserImgMeta2.deleteById(rowKey)
    delImgFut flatMap { _ =>
      delMetaFut
    }
  }

}


object Img2FullyDeletedEvent {

  def getClassifier(rowKey: Option[UUID] = None): Classifier = {
    List(Some(getClass.getSimpleName), rowKey)
  }

}

case class Img2FullyDeletedEvent(rowKey: UUID) extends Event {

  lazy val rowKeyStr = UuidUtil.uuidToBase64(rowKey)

  override def getClassifier: Classifier = {
    Img2FullyDeletedEvent.getClassifier(Some(rowKey))
  }

  override def toString: String = s"${getClass.getSimpleName}($rowKeyStr)"
}
