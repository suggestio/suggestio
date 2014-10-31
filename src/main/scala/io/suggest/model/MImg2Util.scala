package io.suggest.model

import java.util.UUID

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
  def deleteFully(imgId: String)(implicit ec: ExecutionContext): Future[_] = {
    // Нужно извлечь uuid из id
    val rowKey = UuidUtil.base64ToUuid(imgId)
    deleteFully(rowKey)
  }
  def deleteFully(rowKey: UUID)(implicit ec: ExecutionContext): Future[_] = {
    val delImgFut = MUserImg2.deleteById(rowKey)
    val delMetaFut = MUserImgMeta2.deleteById(rowKey)
    val delThumbFut = MImgThumb2.deleteById(rowKey)
    delImgFut flatMap { _ =>
      delMetaFut flatMap { _ =>
        delThumbFut
      }
    }
  }

}
