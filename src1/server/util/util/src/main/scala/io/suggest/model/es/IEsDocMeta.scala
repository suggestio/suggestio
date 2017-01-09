package io.suggest.model.es

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
case class EsDocMeta[D](doc: D)(implicit ev: IEsDoc[D]) extends IEsDocMeta {
  override def version  = ev.version(doc)
  override def id       = ev.id(doc)
}
