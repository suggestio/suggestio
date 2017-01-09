package models.mext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 18:42
 * Description: Представление инфы по результатам аплоада.
 */

/** Интерфейс распарсенных данных по успешному аплоаду. */
trait IPostAttachmentId {
  def strId: String
}

case class StringIdPostAttachment(strId: String)
  extends IPostAttachmentId
