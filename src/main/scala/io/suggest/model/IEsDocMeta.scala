package io.suggest.model

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 18:01
 * Description: Интерфейс для доступа к метаданным документов ES.
 */
trait IEsDocMeta {

  /** Доступ к id документа. */
  def id: Option[String]

  /** Доступ к версии документа. */
  def version: Option[Long]

}


/** Дефолтовая реализация [[IEsDocMeta]]. */
case class EsDocMeta(
  override val id       : Option[String] = None,
  override val version  : Option[Long] = None
)
  extends IEsDocMeta
